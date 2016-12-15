package org.mcv.app;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import lombok.Cleanup;

public class Db {

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
		conn = getConnection(name);
	}

	private Connection getConnection(String name) {
		try {
			Class.forName("org.h2.Driver");
			String format = app.getProps().getProperty("h2.url", "jdbc:h2:./%s"); 
			String connString = String.format(format, name);
			Connection conn = DriverManager.getConnection(connString, "", "");
			return conn;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Closes DB connection.
	 */
	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			// log.error("{}", e, e);
			throw new WrapperException(e);
		}
	}

	/*
	 * Create tables
	 */
	static String[] recordSpec = new String[] { "NAME", "CLASSNAME", "VERSION",
			"PARENT", "CHILDREN", "CREATED", "CURRENT", "DELETED", "JSON" };

	static String[] typeSpec = new String[] { "nvarchar(1024)", // NAME
			"nvarchar(1024)", // CLASSNAME
			"long", // VERSION
			"long", // PARENT
			"nvarchar(1024)", // CHILDREN
			"timestamp", // CREATED
			"boolean", // CURRENT
			"boolean", // DELETED
			"clob", // JSON
	};

	private static String typeSpec() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < typeSpec.length; i++) {
			sb.append(recordSpec[i]).append(" ").append(typeSpec[i])
					.append(",").append("\r\n");
		}
		return sb.toString();
	}

	static String[] recordSpecLogs = new String[] { "NAME", "CLASSNAME", "STAMP", "THREAD",
			"METHOD", "CALLER", "CALLERLINE", "CALLERCLASS", "KIND", "OBJECT1",
			"OBJECT2", "MESSAGE" };

	static String[] typeSpecLogs = new String[] { 
			"nvarchar(1024)", // NAME
			"nvarchar(1024)", // CLASSNAME
			"timestamp", // TIMESTAMP
			"nvarchar(256)", // THREAD
			"nvarchar(256)", // METHOD
			"nvarchar(256)", // CALLER
			"integer", // CALLERLINE
			"nvarchar(256)", // CALLERCLASS
			"integer", // KIND
			"clob", // OBJECT!
			"clob", // OBJECT2
			"nvarchar(1024)" // MESSAGE
	};

	private static String typeSpecLogs() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < typeSpecLogs.length; i++) {
			sb.append(recordSpecLogs[i]).append(" ").append(typeSpecLogs[i])
					.append(",").append("\r\n");
		}
		return sb.toString();
	}

	public void createAppTable() {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("CREATE TABLE IF NOT EXISTS APPLICATION ("
							+ typeSpec()
							+ "PRIMARY KEY (NAME, CLASSNAME, VERSION) )");
			st.executeUpdate();
		} catch (Exception e2) {
			throw new WrapperException(e2);
		}
	}

	public void createLogsTable() {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("CREATE TABLE IF NOT EXISTS LOGS ("
							+ typeSpecLogs() + ")");
			st.executeUpdate();
		} catch (Exception e2) {
			throw new WrapperException(e2);
		}
	}

	/**
	 * Clears database.
	 */
	public void clear() {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("DELETE FROM APPLICATION");
			st.executeUpdate();
			st = conn.prepareStatement("DELETE FROM LOGS");
			st.executeUpdate();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/*
	 * Create new version based on previous.
	 */
	public synchronized <T extends Base> T newVersion(T obj) {
		try {
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
			return obj;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Updates record in DB.
	 * Update existing version, create new version, store.
	 * 
	 * @param base
	 */
	public <T extends Base> void store(T base) {
		if(willStore(base)) {
			newVersion(base);
			base.inBatch = false;
		}
	}

	/*
	 * Special case: deleted object.
	 */
	<T extends Base> void storeDeleted(T base) {
		if(base.isCurrent()) {
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
		@SuppressWarnings("unchecked")
		Base stored = retrieve(obj.getName(), (Class<? extends T>) obj.getClazz());
		if(stored == null) return true;
		return !Application.toJson(obj).equals(stored.getJson());
	}

	/*
	 * Insert new version in DB.
	 */
	void newRecord(Base record) {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("INSERT INTO APPLICATION ("
							+ insertSpec(recordSpec) + ") VALUES ("
							+ questions(recordSpec) + ")");
			prepare(st, record);
			int n = st.executeUpdate();
			if(n != 1) {
				throw new Exception("More or less than 1 record inserted: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/*
	 * Adjust existing record in DB.
	 */
	void update(Base record, boolean current, List<Long> children, String json) {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("UPDATE APPLICATION SET CURRENT = ?, "
							+ " CHILDREN = ?, JSON = ? " 
							+ "WHERE NAME=? AND CLASSNAME = ? AND VERSION = ?");
			st.setBoolean(1, current);
			st.setString(2, Application.toJson(children));
			st.setString(3, json);
			st.setString(4, record.getName());
			st.setString(5, record.getClazz().getCanonicalName());
			st.setLong(6, record.getVersion());
			int n = st.executeUpdate();
			if(n != 1) {
				throw new Exception("More or less than 1 record updated: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/*
	 * Adjust current flag in DB.
	 */
	void updateCurrent(Base record) {
		try {
			@Cleanup
			PreparedStatement st = conn.prepareStatement("UPDATE APPLICATION SET CURRENT = false WHERE NAME = ? AND CLASSNAME = ?");
			st.setString(1, record.getName());
			st.setString(2, record.getClazz().getCanonicalName());
			st.executeUpdate();
			
			st = conn.prepareStatement("UPDATE APPLICATION SET CURRENT = true "
							+ "WHERE NAME=? AND CLASSNAME = ? AND VERSION = ?");
			st.setString(1, record.getName());
			st.setString(2, record.getClazz().getCanonicalName());
			st.setLong(3, record.getVersion());
			int n = st.executeUpdate();
			if(n != 1) {
				throw new Exception("More or less than 1 record updated: " + n);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	/**
	 * Helper functions.
	 */
	private static String questions(String[] spec) {
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
			st.setString(i++, record.getClazz().getCanonicalName());
			st.setLong(i++, record.getVersion());
			st.setLong(i++, record.getParent());
			st.setString(i++, Application.toJson(record.getChildren()));
			st.setTimestamp(i++, Timestamp.valueOf(record.getCreated()));
			st.setBoolean(i++, record.isCurrent());
			st.setBoolean(i++, record.isDeleted());
			st.setString(i++, record.getJson());
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
	public <T extends Base> T retrieve(String name, Class<? extends T> clazz) {
		return retrieve(name, clazz, false);
	}
	
	public <T extends Base> T retrieve(String name, Class<? extends T> clazz, boolean noProxy) {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? AND CURRENT = TRUE AND DELETED = false");
			st.setString(1, name);
			st.setString(2, clazz.getCanonicalName());
			@Cleanup
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				String className = rs.getString("CLASSNAME");
				@SuppressWarnings("unchecked")
				T record = makeBase(name, (Class<? extends T>)Class.forName(className), rs.getString("JSON"));
				if(noProxy) {
					return record;
				} else {
					return app.setProxies(record);
				}
			}
			return null;
		} catch (Exception e) {
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
	public <T extends Base> T retrieve(String name, Class<? extends T> clazz, long version, boolean noProxy) {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? AND VERSION = ?");
			st.setString(1, name);
			st.setString(2, clazz.getCanonicalName());
			st.setLong(3, version);
			@Cleanup
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				String className = rs.getString("CLASSNAME");
				@SuppressWarnings("unchecked")
				T record = makeBase(name, (Class<? extends T>)Class.forName(className), rs.getString("JSON"));
				if(noProxy) {
					return record;
				} else {
					return app.setProxies(record);
				}
			}
			return null;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}


	private <T extends Base> T makeBase(String name, Class<? extends T> clazz, String json) {
		try {
			T base = clazz.getConstructor(String.class, Class.class)
					.newInstance(name, clazz);
			base.json = json;
			Application.jsonClone(base, base);
			base.app = app;
			return base;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	synchronized long lastNumber(Base base) {
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT MAX(VERSION) AS HIGHEST FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ?");
			st.setString(1, base.getName());
			st.setString(2, base.getClazz().getCanonicalName());
			@Cleanup
			ResultSet rs = st.executeQuery();
			if (rs.next()) {
				long number = rs.getLong("HIGHEST");
				return number;
			}
			return 0;
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
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT * FROM APPLICATION WHERE CLASSNAME = ? AND CURRENT = TRUE AND DELETED = false ORDER BY NAME");
			st.setString(1, clazz.getCanonicalName());
			@Cleanup
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				@SuppressWarnings("unchecked")
				T record = (T) makeBase(rs.getString("NAME"), clazz, rs.getString("JSON"));
				ret.add(app.setProxies(record));
			}
			return ret;
		} catch (Exception e) {
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
		try {
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT * FROM APPLICATION WHERE NAME = ? AND CLASSNAME = ? ORDER BY VERSION");
			st.setString(1, base.getName());
			st.setString(2, base.clazz.getCanonicalName());
			@Cleanup
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				@SuppressWarnings("unchecked")
				T record = (T) makeBase(rs.getString("NAME"), base.clazz, rs.getString("JSON"));
				record.current = false;
				ret.add(record);
			}
			return ret;
		} catch (Exception e) {
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
		try {
			List<LogEntry> logs = new ArrayList<>();
			@Cleanup
			PreparedStatement st = conn
					.prepareStatement("SELECT * FROM LOGS WHERE NAME = ? AND CLASSNAME = ? ORDER BY STAMP");
			st.setString(1, name);
			st.setString(2, clazz);
			@Cleanup
			ResultSet rs = st.executeQuery();
			while (rs.next()) {
				LogEntry record = makeLogEntry(rs);
				logs.add(record);
			}
			return logs;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private LogEntry makeLogEntry(ResultSet rs) {
		try {
			LogEntry log = new LogEntry();
			log.setCaller(rs.getString("CALLER"));
			log.setCallerClass(rs.getString("CALLERCLASS"));
			log.setCallerLine(rs.getInt("CALLERLINE"));
			log.setName(rs.getString("NAME"));
			log.setClazz(rs.getString("CLASSNAME"));
			log.setKind(LogEntry.Kind.fromInteger(rs.getInt("KIND")));
			log.setMessage(rs.getString("MESSAGE"));
			log.setMethod(rs.getString("METHOD"));
			log.setObject1(rs.getString("OBJECT1"));
			log.setObject2(rs.getString("OBJECT2"));
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
			PreparedStatement st = conn.prepareStatement("INSERT INTO LOGS ("
					+ insertSpec(recordSpecLogs) + ") VALUES ("
					+ questions(recordSpecLogs) + ")");
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
			st.setString(i++, entry.caller);
			st.setInt(i++, entry.callerLine);
			st.setString(i++, entry.callerClass);
			st.setInt(i++, entry.kind.ordinal());
			st.setString(i++, Application.toJson(entry.object1));
			st.setString(i++, Application.toJson(entry.object2));
			st.setString(i++, entry.message);
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}
}
