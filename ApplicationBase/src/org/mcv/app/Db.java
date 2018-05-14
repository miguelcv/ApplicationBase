package org.mcv.app;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.Cleanup;

public class Db {

	private static final String CLASSNAME = "CLASSNAME";
	private static final String INTEGER = "integer";
	private Connection conn;
	private Application app;

	/**
	 * Initializes H2 database.
	 * 
	 * @param app
	 * @param name
	 */
	public Db(Application app, String name) {
		this.app = app;
		getConnection(name);
	}

	private void getConnection(String name) {
		String connString = "";
		try {
			Class.forName("org.h2.Driver");
			String format = app.getProps().getProperty("h2.url", "jdbc:h2:./%s");
			connString = String.format(format, name);
			conn = DriverManager.getConnection(connString, "", "");
		} catch (Exception e) {
			app.log.error(e, "Error connecting to DB: %s", connString);
			throw new WrapperException(e);
		}
	}

	/**
	 * Closes DB connection.
	 */
	public void close() {
		try {
			conn.close();
		} catch (Exception e) {
			app.log.error(e, "Error disconnecting from DB");
			throw new WrapperException(e);
		}
	}

	/*
	 * Create tables
	 */
	static String[] recordSpec = new String[] { 
			"NAME", CLASSNAME, "CLASSPATH", "VERSION", "PARENT", "CHILDREN",
			"CREATED", "CURRENT", "DELETED", "JSON" };

	static String[] typeSpec = new String[] { 
			string(1024), // NAME
			string(1024), // CLASSNAME
			string(1024), // CLASSPATH
			"long", // VERSION
			"long", // PARENT
			string(1024), // CHILDREN
			"timestamp", // CREATED
			"boolean", // CURRENT
			"boolean", // DELETED
			"clob", // JSON
	};

	static String string(int len) {
		return "nvarchar(" + len + ")";
	}

	private static String typeSpec() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < typeSpec.length; i++) {
			sb.append(recordSpec[i]).append(" ").append(typeSpec[i]).append(",").append("\r\n");
		}
		return sb.toString();
	}

	static String[] recordSpecLogs = new String[] { "NAME", CLASSNAME, "STAMP", "THREAD", "METHOD",
			"METHODLINE", "METHODCLASS", "CALLER", "CALLERLINE", "CALLERCLASS", "KIND", "OBJLIST", "MESSAGE" };

	static String[] typeSpecLogs = new String[] { "nvarchar(1024)", // NAME
			string(1024), // CLASSNAME
			"timestamp", // STAMP
			string(256), // THREAD
			string(256), // METHOD
			INTEGER, // METHODLINE
			string(256), // METHODCLASS
			string(256), // CALLER
			INTEGER, // CALLERLINE
			string(256), // CALLERCLASS
			INTEGER, // KIND
			"clob", // OBJLIST
			"clob" // MESSAGE
	};

	private static String typeSpecLogs() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < typeSpecLogs.length; i++) {
			sb.append(recordSpecLogs[i]).append(" ").append(typeSpecLogs[i]).append(",").append("\r\n");
		}
		return sb.toString();
	}

	public void createAppTable() {
		try (PreparedStatement st = conn.prepareStatement(
				"CREATE TABLE IF NOT EXISTS APPLICATION (" + typeSpec() + "PRIMARY KEY (NAME, CLASSNAME, VERSION) )")) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error creating APPLICATION table");
			throw new WrapperException(e);
		}
	}

	public void createLogsTable() {
		try (PreparedStatement st = conn.prepareStatement("CREATE TABLE IF NOT EXISTS LOGS (" + typeSpecLogs() + ")")) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error creating LOGS table");
			throw new WrapperException(e);
		}
	}

	/**
	 * Clears database.
	 */
	public void clear() {
		try (PreparedStatement st = conn.prepareStatement("DELETE FROM APPLICATION")) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error clearing DB");
			throw new WrapperException(e);
		}
		try (PreparedStatement st = conn.prepareStatement("DELETE FROM LOGS")) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error clearing DB");
			throw new WrapperException(e);
		}
	}

	/**
	 * Cleans up database.
	 */
	public void cleanup(int days) {
		String sql = String.format(
				"DELETE FROM APPLICATION WHERE CREATED < TIMESTAMPADD('DAY', -%d, NOW()) AND (CURRENT = false OR DELETED = true)",
				days);
		try (PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error cleaning DB");
			throw new WrapperException(e);
		}
		sql = String.format("DELETE FROM LOGS WHERE CREATED < TIMESTAMPADD('DAY', -%d, NOW())", days);
		try (PreparedStatement st = conn.prepareStatement(sql)) {
			st.executeUpdate();
		} catch (Exception e) {
			app.log.error(e, "Error cleaning DB");
			throw new WrapperException(e);
		}
	}

	/*
	 * Create new version based on previous.
	 */
	synchronized <T extends Base> T newVersion(T obj) {
		try {
			app.log.entry(obj);

			// adjust current object
			long nextNumber = nextNumber(obj);
			obj.current = false;
			obj.children.add(nextNumber);
			Map<String, Object> map = Application.fromJson(obj.getJson(), new HashMap<String, Object>());
			map.put("current", false);
			map.put("children", obj.children);
			obj.json = Application.toJson(map);
			update(obj, false, obj.children, obj.json);

			// create new version
			obj.current = true;
			obj.children = new LinkedList<Long>();
			obj.created = LocalDateTime.now();
			obj.deleted = obj.isDeleted();
			obj.parent = obj.getVersion();
			obj.version = nextNumber;
			obj.json = Application.toJson(obj);

			newRecord(obj);
			return app.log.exit(obj);
		} catch (Exception e) {
			app.log.error(e, "Error creating new version for object %s", obj.getName());
			throw new WrapperException(e);
		}
	}

	/**
	 * Updates record in DB. Update existing version, create new version, store.
	 * 
	 * @param base
	 */
	public <T extends Base> void store(T base) {
		if (willStore(base)) {
			newVersion(base);
			base.inBatch = false;
		}
	}

	/*
	 * Special case: deleted object.
	 */
	<T extends Base> void storeDeleted(T base) {
		if (base.isCurrent()) {
			newVersion(base);
			base.inBatch = false;
		}
	}

	/**
	 * Is object storeable?
	 * 
	 * @param base
	 * @return will it store?
	 */
	public <T extends Base> boolean willStore(T base) {
		return base.isCurrent() && !base.isDeleted() && anythingChanged(base);
	}

	/*
	 * Has anything changed?
	 */
	private <T extends Base> boolean anythingChanged(T obj) {
		Base stored = retrieve(obj.getName(), obj.getClass(), true);
		if (stored == null)
			return true;
		return !Application.toJson(obj).equals(stored.getJson());
	}

	/*
	 * Insert new version in DB.
	 */
	void newRecord(Base record) {
		try {
			@Cleanup
			PreparedStatement st = conn.prepareStatement("INSERT INTO APPLICATION (" + insertSpec(recordSpec)
					+ ") VALUES (" + questionMarks(recordSpec) + ")");
			prepare(st, record);
			int n = st.executeUpdate();
			if (n != 1) {
				throw new ApplicationException("More or less than 1 record inserted: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/*
	 * Adjust existing record in DB.
	 */
	void update(Base record, boolean current, List<Long> children, String json) {
		try (PreparedStatement st = conn.prepareStatement("UPDATE APPLICATION SET CURRENT = ?, "
				+ " CHILDREN = ?, JSON = ? " + "WHERE NAME=? AND CLASSNAME = ? AND VERSION = ?")) {
			st.setBoolean(1, current);
			st.setString(2, Application.toJson(children));
			st.setString(3, json);
			st.setString(4, record.getName());
			st.setString(5, record.getClassFullName());
			st.setLong(6, record.getVersion());
			int n = st.executeUpdate();
			if (n != 1) {
				throw new ApplicationException("More or less than 1 record updated: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/*
	 * Adjust 'current' flag in DB.
	 */
	void updateCurrent(Base record) {
		try (PreparedStatement st = conn
				.prepareStatement("UPDATE APPLICATION SET CURRENT = false WHERE NAME = ? AND CLASSNAME = ?")) {
			st.setString(1, record.getName());
			st.setString(2, record.getClassFullName());
			st.executeUpdate();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
		try (PreparedStatement st = conn.prepareStatement(
				"UPDATE APPLICATION SET CURRENT = true " + "WHERE NAME=? AND CLASSNAME = ? AND VERSION = ?")) {
			st.setString(1, record.getName());
			st.setString(2, record.getClassFullName());
			st.setLong(3, record.getVersion());
			int n = st.executeUpdate();
			if (n != 1) {
				throw new ApplicationException("More or less than 1 record updated: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Helper functions.
	 */
	private static String questionMarks(String[] spec) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < spec.length; i++) {
			sb.append("?");
			if (i < spec.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private static String insertSpec(String[] spec) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < spec.length; i++) {
			sb.append(spec[i]);
			if (i < spec.length - 1) {
				sb.append(",");
			}
		}
		return sb.toString();
	}

	private void prepare(PreparedStatement st, Base record) {
		try {
			int i = 1;
			st.setString(i++, record.getName());
			st.setString(i++, record.getClassFullName());
			st.setString(i++, Base.getClasspath(record));
			st.setLong(i++, record.getVersion());
			st.setLong(i++, record.getParent());
			st.setString(i++, Application.toJson(record.getChildren()));
			st.setTimestamp(i++, Timestamp.valueOf(record.getCreated()));
			st.setBoolean(i++, record.isCurrent());
			st.setBoolean(i++, record.isDeleted());
			st.setString(i, record.getJson());
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/* Retrieve records */

	/**
	 * Gets a single record.
	 * 
	 * @param name
	 * @param clazz
	 * @return object
	 */
	<T extends Base> T retrieve(String name, Class<? extends T> clazz, Object[] params) {
		return retrieve(name, clazz, false, params);
	}

	<T extends Base> T retrieve(String name, Class<? extends T> clazz, boolean noProxy, Object... params) {
		app.log.entry(name, Base.name(clazz.getName()), noProxy);
		try (PreparedStatement st = conn.prepareStatement(
				"SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? AND CURRENT = TRUE AND DELETED = false")) {
			st.setString(1, name);
			st.setString(2, Base.name(clazz.getName()));
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					String className = rs.getString(CLASSNAME);
					@SuppressWarnings("unchecked")
					T record = makeBase(name, (Class<? extends T>) Class.forName(className), rs.getString("JSON"), params);
					if (noProxy) {
						return app.log.exit(record);
					} else {
						return app.log.exit(app.setProxies(record));
					}
				}
				app.log.warn("Object %s not found!", name);
				return app.log.exit(null);
			} catch (Exception e) {
				app.log.error(e, "Error execuring query %s", st.toString());
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, "Error retrieving object %s", name);
			throw new WrapperException(e);
		}
	}

	/**
	 * Retrieve specific version.
	 * 
	 * @param name
	 * @param clazz
	 * @param version
	 * @param noProxy
	 * @return version
	 */
	public <T extends Base> T retrieve(String name, String className, long version, boolean noProxy) {
		app.log.entry(name, className, version, noProxy);
		try (PreparedStatement st = conn.prepareStatement("SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? AND VERSION = ?")) {
			st.setString(1, name);
			st.setString(2, className);
			st.setLong(3, version);
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					String cName = rs.getString(CLASSNAME);
					@SuppressWarnings("unchecked")
					T record = makeBase(name, (Class<? extends T>) Class.forName(cName), rs.getString("JSON"));
					if (noProxy) {
						return app.log.exit(record);
					} else {
						return app.log.exit(app.setProxies(record));
					}
				}
				app.log.warn("Object %s not found!", name);
				return app.log.exit(null);
			} catch (Exception e) {
				app.log.error(e, "Error performing query %s", st.toString());
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, "Error retrieving object %s", name);
			throw new WrapperException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Base> T makeBase(String name, Class<? extends T> clazz, String json, Object... params) {
		try {
			Optional<Tuple<Constructor<?>,Object[]>> constructor = app.getConstructor(name, clazz, params);
			if(constructor.isPresent()) {
				Tuple<Constructor<?>,Object[]> cons = constructor.get();
				try {
					T base = (T) cons.getA().newInstance(cons.getB());
					base.json = json;
					Application.jsonClone(base, base);
					base.app = app;
					return base;
				} catch(Exception e) {
					throw new WrapperException(e);
				}
			} else {
				throw new ApplicationException("No suitable constructor found for params [%s] %s", name, Arrays.deepToString(params));
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	synchronized long lastNumber(Base base) {
		try (PreparedStatement st = conn
				.prepareStatement("SELECT MAX(VERSION) AS HIGHEST FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ?")) {
			st.setString(1, base.getName());
			st.setString(2, base.getClassFullName());
			try (ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					return rs.getLong("HIGHEST");
				}
				return 0;
			} catch (Exception e) {
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	synchronized long nextNumber(Base base) {
		return lastNumber(base) + 1;
	}

	/**
	 * Get all objects of a specific class.
	 * 
	 * @param clazz
	 * @return list of objects
	 */
	public <T extends Base> List<T> getList(Class<? extends Base> clazz) {
		List<T> ret = new ArrayList<>();
		app.log.entry(clazz);
		try (PreparedStatement st = conn.prepareStatement(
				"SELECT * FROM APPLICATION WHERE CLASSPATH LIKE ? AND CURRENT = TRUE AND DELETED = false ORDER BY NAME")) {
			st.setString(1, Base.getClasspath(clazz) + "%");
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					@SuppressWarnings("unchecked")
					T record = (T) makeBase(rs.getString("NAME"), clazz, rs.getString("JSON"));
					ret.add(app.setProxies(record));
				}
				return app.log.exit(ret);
			} catch (Exception e) {
				app.log.error(e, e.toString());
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, e.toString());
			throw new WrapperException(e);
		}
	}

	/**
	 * Get all versions of a specific object.
	 * 
	 * @param base
	 * @return all versions
	 */
	public <T extends Base> List<T> getVersions(T base) {
		List<T> ret = new ArrayList<>();
		app.log.entry(base);
		try (PreparedStatement st = conn
				.prepareStatement("SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? ORDER BY VERSION")) {
			st.setString(1, base.getName());
			st.setString(2, base.getClassFullName());
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					@SuppressWarnings("unchecked")
					T record = (T) makeBase(base.getName(), base.getClass(), rs.getString("JSON"));
					record.current = false;
					ret.add(record);
				}
				return app.log.exit(ret);
			} catch (Exception e) {
				app.log.error(e, "Error executing query %s", st.toString());
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, "Error getting versions for object %s", base.getName());
			throw new WrapperException(e);
		}
	}

	/**
	 * Get all logs for a specific object.
	 * 
	 * @param name
	 * @param clazz
	 * @return logs
	 */
	public List<LogEntry> getLogs(String name, String clazz) {
		app.log.entry(name, clazz);
		List<LogEntry> logs = new ArrayList<>();
		try (PreparedStatement st = conn
				.prepareStatement("SELECT * FROM LOGS WHERE NAME = ? AND CLASSNAME = ? ORDER BY STAMP")) {
			st.setString(1, name);
			st.setString(2, clazz);
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					LogEntry record = makeLogEntry(rs);
					logs.add(record);
				}
				return app.log.exit(logs);
			} catch (Exception e) {
				app.log.error(e, "Error executing query %s", st.toString());
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, "Error getting logs for object %s", name);
			throw new WrapperException(e);
		}
	}

	/**
	 * Get all logs.
	 * 
	 * @return logs
	 */
	public List<LogEntry> getAllLogs() {
		app.log.entry();
		List<LogEntry> logs = new ArrayList<>();
		try (PreparedStatement st = conn.prepareStatement("SELECT * FROM LOGS ORDER BY STAMP")) {
			try (ResultSet rs = st.executeQuery()) {
				while (rs.next()) {
					LogEntry record = makeLogEntry(rs);
					logs.add(record);
				}
				return app.log.exit(logs);
			} catch (Exception e) {
				app.log.error(e, "Error querying all logs");
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			app.log.error(e, "Error getting all logs");
			throw new WrapperException(e);
		}
	}

	private LogEntry makeLogEntry(ResultSet rs) {
		try {
			LogEntry log = new LogEntry();
			log.setCaller(rs.getString("CALLER"));
			log.setCallerClass(rs.getString("CALLERCLASS"));
			log.setCallerLine(rs.getInt("CALLERLINE"));

			log.setMethod(rs.getString("METHOD"));
			log.setMethodClass(rs.getString("METHODCLASS"));
			log.setMethodLine(rs.getInt("METHODLINE"));

			log.setName(rs.getString("NAME"));
			log.setClazz(rs.getString(CLASSNAME));
			log.setKind(LogEntry.Kind.fromInteger(rs.getInt("KIND")));
			log.setMessage(rs.getString("MESSAGE"));
			log.setObjList(Application.fromJson(rs.getString("OBJLIST"), new ArrayList<>()));
			log.setThread(rs.getString("THREAD"));
			log.setTimestamp(rs.getTimestamp("STAMP").toLocalDateTime());
			return log;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Store a log entry.
	 * 
	 * @param entry
	 */
	public void storeLogEntry(LogEntry entry) {
		try {
			@Cleanup
			PreparedStatement st = conn.prepareStatement("INSERT INTO LOGS (" + insertSpec(recordSpecLogs)
					+ ") VALUES (" + questionMarks(recordSpecLogs) + ")");
			prepareLogs(st, entry);
			st.execute();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private void prepareLogs(PreparedStatement st, LogEntry entry) {
		try {
			int i = 1;
			st.setString(i++, entry.name);
			st.setString(i++, entry.clazz);
			st.setTimestamp(i++, Timestamp.valueOf(entry.timestamp));
			st.setString(i++, entry.thread);
			st.setString(i++, entry.method);
			st.setInt(i++, entry.methodLine);
			st.setString(i++, entry.methodClass);
			st.setString(i++, entry.caller);
			st.setInt(i++, entry.callerLine);
			st.setString(i++, entry.callerClass);
			st.setInt(i++, entry.kind.ordinal());
			st.setString(i++, Application.toJson(entry.objList));
			st.setString(i++, entry.message);
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}
}
