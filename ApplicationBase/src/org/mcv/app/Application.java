package org.mcv.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.h2.jdbc.JdbcSQLException;
import org.mcv.app.LogEntry.Kind;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.paranamer.ParanamerModule;

import jodd.proxetta.MethodInfo;
import jodd.proxetta.ProxyAspect;
import jodd.proxetta.impl.ProxyProxetta;
import jodd.proxetta.pointcuts.ProxyPointcutSupport;
import lombok.Cleanup;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;

/**
 * @author Miguelc
 *
 *         The Application class takes care of creating self-logging,
 *         self-persisting Base objects. It also provides some JSON
 *         serialization and deserialization utilities.
 * 
 */
@Data
@ToString(exclude = { "db", "props", "log", "logLocation", "logLevel" })
public class Application {

	@Delegate
	Db db;

	String name;

	static ObjectMapper mapper;
	static {
		mapper = new ObjectMapper();
		mapper.findAndRegisterModules();
		mapper.registerModule(new ParanamerModule());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
		mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
		mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
	}

	Base log = new Base("log");
	static Base staticLog = new Base("staticLog");
	File logLocation;
	LogEntry.Kind logLevel = Kind.DEBUG;

	Properties props = new Properties();

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            Application name.
	 */
	public Application(String name) {
		this.name = name;
		log.app = this;
		if (staticLog.app == null)
			staticLog.app = this;

		Thread.setDefaultUncaughtExceptionHandler((t, e) -> log.error(e, "Uncaught exception"));

		try {
			@Cleanup
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.props");
			props.load(is);
		} catch (Exception e) {
			log.warn(e, "Could not load config.props");
		}

		db = new Db(this, name);
		db.createAppTable();
		db.createLogsTable();

		String logLoc = props.getProperty("app.logLocation");
		if (logLoc != null) {
			logLocation = new File(logLoc);
			if (!logLocation.exists() && !logLocation.getName().endsWith(".log")) {
				logLocation.mkdirs();
			}

			String level = props.getProperty("app.logLevel", "DEBUG");
			logLevel = Kind.valueOf(level);
		}
		log.info("Application " + name + " initialized.");
	}

	/**
	 * Sets the Setter and Method proxies on the Base object.
	 * 
	 * @param base
	 *            Base object
	 * @return Proxied Base object
	 */
	@SuppressWarnings("unchecked")
	<T extends Base> T setProxies(T base) {
		try {
			log.entry(base);
			ProxyAspect forSetters = new ProxyAspect(SetterAdvice.class, new ProxyPointcutSupport() {
				public boolean apply(MethodInfo methodInfo) {
					if (isPublic(methodInfo) && matchMethodName(methodInfo, "get*")) {
						String clazz = methodInfo.getReturnTypeName();
						if(clazz.equals("Ljava/util/List") ||
								clazz.equals("Ljava/util/Set") ||
								clazz.equals("Ljava/util/Map")) {
							return true;
						}
					}
					return hasAnnotation(methodInfo, Modify.class)
							|| (isPublic(methodInfo) && matchMethodName(methodInfo, "set*")
									&& hasOneArgument(methodInfo) && !hasAnnotation(methodInfo, Ignore.class));
				}
			});
			ProxyAspect forMethods = new ProxyAspect(MethodAdvice.class, new ProxyPointcutSupport() {
				public boolean apply(MethodInfo methodInfo) {
					if (hasReturnValue(methodInfo)
							&& (matchMethodName(methodInfo, "get*") || (matchMethodName(methodInfo, "is*")))
							&& hasNoArguments(methodInfo)) {
						// getter
						return false;
					}

					if (matchMethodName(methodInfo, "set*") && hasOneArgument(methodInfo)) {
						// setter
						return false;
					}
					if (hasAnnotation(methodInfo, Ignore.class)) {
						return false;
					}
					return isPublic(methodInfo);
				}
			});

			ProxyProxetta proxetta = ProxyProxetta.withAspects(forSetters, forMethods);
			proxetta.setVariableClassName(true);
			T ret = (T) proxetta.builder(base.getClass()).define().getConstructor(String.class)
					.newInstance(base.getName());
			copy(base, ret);
			return log.exit(ret);
		} catch (Exception e) {
			log.error(e, "AOP error");
			throw new WrapperException(e);
		}
	}

	private <T extends Base> void copy(T from, T to) {
		to.app = from.app;
		to.children = from.children;
		to.created = from.created;
		to.current = from.current;
		to.deleted = from.deleted;
		to.json = from.json;
		to.parent = from.parent;
		to.version = from.version;
		jsonClone(from, to);
	}

	@SuppressWarnings("unchecked")
	@Magic
	public <T extends Base> T create(Object... params) {
		Resolver solver = new Resolver(UUID.randomUUID().toString());
		String resolvedName = solver.resolvedName;
		Class<? extends Base> clazz = (Class<? extends Base>) solver.resolvedClass;
		return (T) create(resolvedName, clazz, smart(clazz, params), params);
	}

	@SuppressWarnings("unchecked")
	@Magic
	public <T extends Base> T createAs(String name, Object... params) {
		Resolver solver = new Resolver(UUID.randomUUID().toString());
		Class<? extends Base> clazz = (Class<? extends Base>) solver.resolvedClass;
		return (T) create(name, clazz, smart(clazz, params), params);
	}

	boolean smart(Class<? extends Base> clazz, Object[] params) {
		if (clazz == null)
			return true;
		log.debug("class = " + clazz + " parsm = " + params.length);
		return (clazz.getDeclaredFields().length > params.length);
	}

	/**
	 * Create a NEW base object.
	 * 
	 * @param name
	 *            Object's name
	 * @param clazz
	 *            Object's class
	 * @param params
	 *            Any additional arguments for the constructor
	 * @return New object
	 */
	public <T extends Base> T create(String name, Class<? extends T> clazz, Object... params) {
		return create(name, clazz, true, params);
	}

	public <T extends Base> T create(String name, Class<? extends T> clazz, boolean batch, Object... params) {
		try {
			log.entry(name, clazz);
			T obj = db.retrieve(name, clazz, params);
			if (obj == null) {
				return log.exit(createNew(name, clazz, batch, params));
			} else {
				// return object retrieved from DB
				return log.exit(obj);
			}
		} catch (Exception e) {
			log.error(e, "Unexpected error creating object %s.%s", clazz.getCanonicalName(), name);
			throw new WrapperException(e);
		}
	}

	Optional<Tuple<Constructor<?>, Object[]>> getConstructor(String name, Class<?> clazz, Object[] params) {
		Constructor<?>[] ctors = clazz.getConstructors();
		Class<?>[] paramTypes = new Class<?>[params.length];
		Class<?>[] paramTypes1 = new Class<?>[params.length + 1];
		for (int i = 0; i < params.length; i++) {
			paramTypes[i] = params[i].getClass();
		}
		paramTypes1[0] = String.class;
		System.arraycopy(paramTypes, 0, paramTypes1, 1, paramTypes.length);

		for (Constructor<?> ctor : ctors) {
			Class<?>[] classes = ctor.getParameterTypes();
			print("Try CTOR " + Arrays.deepToString(classes));
			boolean mismatch = false;
			if (classes.length == paramTypes.length) {
				for (int i = 0; i < classes.length; i++) {
					if (!classes[i].equals(paramTypes[i])) {
						mismatch = true;
						break;
					}
				}
				if (!mismatch) {
					print("found constructor " + Arrays.deepToString(paramTypes));
					return Optional.of(new Tuple<>(ctor, params));
				}
			}
			mismatch = false;
			if (classes.length == paramTypes1.length) {
				for (int i = 0; i < classes.length; i++) {
					if (!classes[i].equals(paramTypes1[i])) {
						mismatch = true;
						break;
					}
				}
				if (!mismatch) {
					print("found constructor " + Arrays.deepToString(paramTypes1));
					Object[] params1 = new Object[params.length + 1];
					params1[0] = name;
					System.arraycopy(params, 0, params1, 1, params.length);
					return Optional.of(new Tuple<>(ctor, params1));
				}
			}
		}
		return Optional.empty();
	}

	@SuppressWarnings("unchecked")
	private <T extends Base> T createNew(String name, Class<? extends T> clazz, boolean batch, Object... params) {
		try {
			Optional<Tuple<Constructor<?>, Object[]>> constructor = getConstructor(name, clazz, params);
			if (!constructor.isPresent()) {
				throw new ApplicationException("No suitable constructor found for params [%s] %s", name,
						Arrays.deepToString(params));
			}
			Tuple<Constructor<?>, Object[]> cons = constructor.get();
			try {
				T obj = (T) cons.getA().newInstance(cons.getB());
				if (obj.name == null)
					obj.name = name;
				obj.app = this;
				obj.children = new LinkedList<Long>();
				obj.created = LocalDateTime.now();
				obj.current = true;
				obj.deleted = false;
				obj.parent = 0;
				obj.version = 1;
				obj.inBatch = batch;
				obj.json = toJson(obj);
				db.newRecord(obj);
				return log.exit(setProxies(obj));
			} catch (Exception e) {
				throw new WrapperException(e);
			}
		} catch (Exception e) {
			Throwable t = WrapperException.unwrap(e);
			if (t instanceof JdbcSQLException && t.getMessage().contains("Unique index or primary key violation")) {
				// object was not retrieved because deleted!
				log.error(t, "Object %s.%s exists, but has been deleted", clazz.getCanonicalName(), name);
			} else {
				log.error(t, "Unexpected error creating object %s.%s", clazz.getCanonicalName(), name);
			}
		}
		return log.exit(null);
	}

	/**
	 * Serializes an object.
	 * 
	 * @param obj
	 *            An object
	 * @return JSON string
	 */
	public static String toJson(Object obj) {
		try {
			if (obj == null)
				return "null";
			String s = mapper.writeValueAsString(obj);
			if (s.equals("[[]]"))
				s = "[]";
			return s;
		} catch (Exception e) {
			staticLog.warn(e, "Error serializing %s to JSON", String.valueOf(obj));
			return String.valueOf(obj);
		}
	}

	/**
	 * Deserializes an object.
	 * 
	 * @param json
	 *            JSON string
	 * @param obj
	 *            Template object
	 * @return The deserialized object
	 */
	public static <T> T fromJson(String json, T obj) {
		try {
			return mapper.readValue(json, new TypeReference<T>() {
			});
		} catch (Exception e) {
			staticLog.warn(e, "Error deserializing JSON: %s", json);
			throw new WrapperException(e);
		}
	}

	/**
	 * Deserializes an object's JSON into an existing object.
	 * 
	 * @param from
	 *            Source object
	 * @param to
	 *            Destination object
	 */
	public static <T extends Base> void jsonClone(T from, T to) {
		try {
			mapper.readerForUpdating(to).readValue(from.getJson());
		} catch (Exception e) {
			staticLog.warn(e, "Error cloning JSON: %s", from != null ? from.getJson() : "null");
			throw new WrapperException(e);
		}
	}

	/**
	 * Redo.
	 * 
	 * @param base
	 *            Object to redo
	 */
	public <T extends Base> void redo(T base) {
		Base version = db.retrieve(base.getName(), base.getClassFullName(), base.getChildren().get(0), false);
		if (version == null) {
			notfound(base, base.getChildren().get(0));
			return;
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	private void notfound(Base base, Long ver) {
		base.error("Version %d not found", ver);
	}

	/**
	 * Undo.
	 * 
	 * @param base
	 *            Object to undo
	 */
	public void undo(Base base) {
		Base version = db.retrieve(base.getName(), base.getClassFullName(), base.getParent(), false);
		if (version == null) {
			notfound(base, base.getParent());
			return;
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Redo to specific version in the version tree.
	 * 
	 * @param base
	 *            Object to redo
	 */
	public void redo(Base base, long version) {
		Base ver = db.retrieve(base.getName(), base.getClassFullName(), version, false);
		if (ver == null) {
			notfound(base, version);
			return;
		}
		copy(ver, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Make any version the current one.
	 * 
	 * @param base
	 *            The current object
	 * @param version
	 *            The version to copy into the current object
	 */
	public <T extends Base> void select(T base, T version) {
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Writes a log entry to a file.
	 * 
	 * @param file
	 *            output file
	 * @param entry
	 *            log entry
	 * @return success
	 */
	public static boolean writeLog(File file, LogEntry entry, boolean logToConsole) {
		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		try (PrintStream out = new PrintStream(new FileOutputStream(file, true), true, "utf-8")) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
			String msg = String.format("%s [%s] %-6s method: %s.%s(%s) caller: %s.%s(%s)%n%s",
					entry.getTimestamp().format(formatter), entry.getThread(), entry.getKind().toString(),
					entry.getClazz(), entry.getMethod(),
					entry.getMethodLine() >= 0 ? String.valueOf(entry.getMethodLine()) : "", entry.getCallerClass(),
					entry.getCaller(), entry.getCallerLine() > -0 ? String.valueOf(entry.getCallerLine()) : "",
					formatObjects(entry));
			out.println(msg);
			if (logToConsole) {
				print(msg);
			}
			return true;
		} catch (Exception e) {
			print("Error formatting log: " + e);
			for (StackTraceElement ste : e.getStackTrace()) {
				print("\t" + ste);
			}
			return false;
		}
	}

	@SuppressWarnings("all")
	static void print(String msg) {
		System.out.println(msg);
	}

	private static String formatObjects(LogEntry entry) {
		switch (entry.kind) {
		case NONE:
			return "";
		case SETTER:
			return "\told value: " + entry.objList.get(0) + "\r\n\tnew value: " + entry.objList.get(1);
		case ENTRY:
			return "\targs: " + toJson(entry.objList);
		case EXIT:
			return "\treturns: " + entry.objList.get(0);
		case WARN:
		case ERROR:
			StringBuilder sb = new StringBuilder();
			if (entry.message != null && entry.message.length() > 0) {
				sb.append("\t").append(entry.message);
			}
			if (entry.objList != null && !entry.objList.isEmpty()) {
				if (sb.length() != 0)
					sb.append("\r\n");
				sb.append("\tstack:\r\n").append(stack(entry.objList));
			}
			return sb.toString();
		case DEBUG:
		case INFO:
			if (entry.getMessage() != null && entry.getMessage().length() > 0) {
				return "\tmessage: " + entry.message;
			}
		}
		return "";
	}

	private static String stack(List<Object> trace) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : trace) {
			if (obj instanceof StackTraceElement) {
				sb.append("\t\t").append(obj).append("\r\n");
			} else if (obj instanceof LinkedHashMap) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) obj;
				StackTraceElement ste = new StackTraceElement((String) map.get("declaringClass"),
						(String) map.get("methodName"), (String) map.get("fileName"), (Integer) map.get("lineNumber"));
				sb.append("\t\t").append(ste).append("\r\n");
			} else {
				print("Funny object :" + obj + " of type " + obj.getClass());
			}
		}
		return sb.toString();
	}

	public <T extends Base> List<T> query(Class<T> base, Predicate<T> predicate) {
		List<T> list = getList(base);
		Stream<T> stream = list.stream();
		stream = stream.filter(predicate);
		return stream.collect(Collectors.toList());
	}
}
