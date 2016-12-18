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
import java.util.Properties;

import jodd.proxetta.MethodInfo;
import jodd.proxetta.ProxyAspect;
import jodd.proxetta.impl.ProxyProxetta;
import jodd.proxetta.pointcuts.ProxyPointcutSupport;
import lombok.Cleanup;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;

import org.h2.jdbc.JdbcSQLException;
import org.mcv.app.LogEntry.Kind;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Miguelc
 *
 * The Application class takes care of creating self-logging, self-persisting Base objects.
 * It also provides some JSON serialization and deserialization utilities.
 * 
 */
@Data
@ToString(exclude = { "db", "props", "log", "logLocation", "logLevel" })
public class Application {

	@Delegate
	Db db;
	
	String name;
	
	static ObjectMapper mapper;
	
	Base log = new Base("log", Base.class);
	static Base staticLog = new Base("staticLog", Base.class);
	File logLocation;
	LogEntry.Kind logLevel = Kind.DEBUG;
	static boolean logToConsole;
	
	Properties props = new Properties();

	/**
	 * Constructor.
	 * 
	 * @param name	Application name.
	 */
	public Application(String name) {
		this.name = name;
		log.app = this;
		if (staticLog.app == null)
			staticLog.app = this;

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			public void uncaughtException(Thread t, Throwable e) {
				log.error(e, "Uncaught exception");
			}
		});

		try {
			@Cleanup
			InputStream is = Thread.currentThread().getContextClassLoader()
					.getResourceAsStream("config.props");
			props.load(is);
		} catch (Exception e) {
			log.warn(e, "Could not load config.props");
		}

		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
			mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
			mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);
			mapper.setVisibility(PropertyAccessor.GETTER, Visibility.NONE);
			mapper.setVisibility(PropertyAccessor.IS_GETTER, Visibility.NONE);
			mapper.setVisibility(PropertyAccessor.SETTER, Visibility.NONE);
		}

		db = new Db(this, name);
		db.createAppTable();
		db.createLogsTable();

		String logLoc = props.getProperty("app.logLocation");
		log.info("Log location = " + logLoc);
		if (logLoc != null) {
			logLocation = new File(logLoc);
			if (!logLocation.exists()) {
				if (!logLocation.getName().endsWith(".log")) {
					logLocation.mkdirs();
				}
			}

			String level = props.getProperty("app.logLevel", "DEBUG");
			logLevel = Kind.valueOf(level);
			log.info("Log level = " + level);
		}
		
		logToConsole = props.getProperty("app.logToConsole", "true").equals("true");
		
		log.info("Application " + name + " initialized.");
	}

	/**
	 * Sets the Setter and Method proxies on the Base object.
	 * 
	 * @param base	Base object
	 * @return Proxied Base object
	 */
	@SuppressWarnings("unchecked")
	<T extends Base> T setProxies(T base) {
		try {
			log.entry(base);
			ProxyAspect forSetters = new ProxyAspect(SetterAdvice.class,
					new ProxyPointcutSupport() {
						public boolean apply(MethodInfo methodInfo) {
							return isPublic(methodInfo)
									&& matchMethodName(methodInfo, "set*")
									&& hasOneArgument(methodInfo)
									&& !hasAnnotation(methodInfo, Ignore.class);
						}
					});
			ProxyAspect forMethods = new ProxyAspect(MethodAdvice.class,
					new ProxyPointcutSupport() {
						public boolean apply(MethodInfo methodInfo) {
							if (hasReturnValue(methodInfo)
									&& (matchMethodName(methodInfo, "get*") || (matchMethodName(
											methodInfo, "is*")))
									&& hasNoArguments(methodInfo)) {
								// getter
								return false;
							}

							if (matchMethodName(methodInfo, "set*")
									&& hasOneArgument(methodInfo)) {
								// setter
								return false;
							}
							if (hasAnnotation(methodInfo, Ignore.class)) {
								return false;
							}
							return isPublic(methodInfo);
						}
					});

			ProxyProxetta proxetta = ProxyProxetta.withAspects(forSetters,
					forMethods);
			proxetta.setVariableClassName(true);
			T ret = (T) proxetta.builder(base.getClass()).define()
					.getConstructor(String.class, Class.class)
					.newInstance(base.getName(), base.getClass());
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
	public <T extends Base> T create() {
		Resolver solver = new Resolver();
		String name = solver.resolvedName;
		Class<? extends Base> clazz = (Class<? extends Base>) solver.resolvedClass;
		return (T) create(name, clazz);
	}

	Constructor<?> getConstructor(String name, Class<?> clazz, Object[] params) {
		Constructor<?>[] ctors = clazz.getConstructors();
		Class<?>[] paramTypes = new Class<?>[params.length + 2];
		paramTypes[0] = String.class;
		paramTypes[1] = Class.class;
		for (int i = 0; i < params.length; i++) {
			paramTypes[i + 2] = params[i].getClass();
		}
		for (Constructor<?> ctor : ctors) {
			Class<?>[] classes = ctor.getParameterTypes();
			if(classes.length != paramTypes.length) continue;
			boolean mismatch = false;
			for (int i = 0; i < classes.length; i++) {
				if (!classes[i].equals(paramTypes[i])) {
					mismatch = true;
					break;
				}
			}
			if (!mismatch) {
				return ctor;
			}
		}
		return null;
	}
	
	Object[] getConstructorParams(String name, Class<?> clazz, Object[] params) {
		Object[] ctorParams = new Object[params.length + 2];
		ctorParams[0] = name;
		ctorParams[1] = clazz;
		for (int i = 0; i < params.length; i++) {
			ctorParams[i + 2] = params[i];
		}
		return ctorParams;
	}
	
	/**
	 * Create a NEW base object.
	 * 
	 * @param name		Object's name
	 * @param clazz		Object's class
	 * @param params	Any additional arguments for the constructor
	 * @return New object	
	 */
	@SuppressWarnings("unchecked")
	public <T extends Base> T create(String name, Class<? extends T> clazz,
			Object... params) {
		try {
			log.entry(name, clazz);

			T obj = db.retrieve(name, clazz, params);
			if (obj == null) {
				try {
					Constructor<?> constructor = getConstructor(name, clazz, params);
					if(constructor != null) {
						Object[] ctorParams = getConstructorParams(name, clazz, params);
						
						obj = (T) constructor.newInstance((Object[])ctorParams);
						obj.app = this;
						obj.children = new LinkedList<Long>();
						obj.created = LocalDateTime.now();
						obj.current = true;
						obj.deleted = false;
						obj.parent = 0;
						obj.version = 1;
						obj.json = toJson(obj);
						db.newRecord(obj);
						return log.exit(setProxies(obj));
					} else {
						log.error("No suitable constructor found for types %s",
								Arrays.deepToString(params));
						return log.exit(null);						
					}
				} catch (Exception e) {
					Throwable t = WrapperException.unwrap(e);
					if (t instanceof JdbcSQLException
							&& t.getMessage().contains(
									"Unique index or primary key violation")) {
						// object was not retrieved because deleted!
						log.error(t,
								"Object %s.%s exists, but has been deleted",
								clazz.getCanonicalName(), name);
						return log.exit(null);
					} else {
						log.error(t, "Unexpected error creating object %s.%s",
								clazz.getCanonicalName(), name);
						return log.exit(null);
					}
				}
			} else {
				// return object retrieved from DB
				return log.exit(obj);
			}
		} catch (Exception e) {
			log.error(e, "Unexpected error creating object %s.%s",
					clazz.getCanonicalName(), name);
			throw new WrapperException(e);
		}
	}

	/**
	 * Serializes an object.
	 * 
	 * @param obj	An object
	 * @return JSON string
	 */
	public static String toJson(Object obj) {
		try {
			if (obj == null)
				return "null";
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			staticLog.warn(e, "Error serializing %s to JSON",
					String.valueOf(obj));
			return String.valueOf(obj);
		}
	}

	/**
	 * Deserializes an object.
	 * 
	 * @param json	JSON string
	 * @param obj	Template object
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
	 * @param from	Source object
	 * @param to	Destination object
	 */
	public static <T extends Base> void jsonClone(T from, T to) {
		try {
			mapper.readerForUpdating(to).readValue(from.getJson());
		} catch (Exception e) {
			staticLog.warn(e, "Error cloning JSON: %s",
					from != null ? from.getJson() : "null");
			throw new WrapperException(e);
		}
	}

	/**
	 * Redo.
	 * 
	 * @param base	Object to redo
	 */
	public <T extends Base> void redo(T base) {
		Base version = db.retrieve(base.getName(), base.getClazz(), base
				.getChildren().get(0), false);
		if (version == null) {
			base.error("Version %d not found", base.getChildren().get(0));
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Undo.
	 * 
	 * @param base	Object to undo
	 */
	public void undo(Base base) {
		Base version = db.retrieve(base.getName(), base.getClazz(),
				base.getParent(), false);
		if (version == null) {
			base.error("Version %d not found", base.getParent());
			return;
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Redo to specific version in the version tree.
	 * 
	 * @param base	Object to redo
	 */
	public void redo(Base base, long version) {
		Base ver = db.retrieve(base.getName(), base.getClazz(), version, false);
		if (ver == null) {
			base.error("Version %d not found", version);
			return;
		}
		copy(ver, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Make any version the current one.
	 * 
	 * @param base	The current object
	 * @param version	The version to copy into the current object
	 */
	public <T extends Base> void select(T base, T version) {
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Writes a log entry to a file.
	 * 
	 * @param file	output file
	 * @param entry	log entry
	 * @return	success
	 */
	public static boolean writeLog(File file, LogEntry entry) {
		try {
			if (!file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			@Cleanup
			PrintStream out = new PrintStream(new FileOutputStream(file, true),
					true, "utf-8");
			DateTimeFormatter formatter = DateTimeFormatter
					.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
			out.printf(
					"%s [%s] %-6s method: %s.%s(%s) caller: %s.%s(%s)%n%s%n",
					entry.getTimestamp().format(formatter),
					entry.getThread(),
					entry.getKind().toString(),
					entry.getClazz(),
					entry.getMethod(),
					entry.getMethodLine() >= 0 ? String.valueOf(entry
							.getMethodLine()) : "",
					entry.getCallerClass(),
					entry.getCaller(),
					entry.getCallerLine() > -0 ? String.valueOf(entry.getCallerLine()) : "", formatObjects(entry));
			
			if(logToConsole) {
				System.out.printf(
						"%s [%s] %-6s method: %s.%s(%s) caller: %s.%s(%s)%n%s%n",
						entry.getTimestamp().format(formatter),
						entry.getThread(),
						entry.getKind().toString(),
						entry.getClazz(),
						entry.getMethod(),
						entry.getMethodLine() >= 0 ? String.valueOf(entry.getMethodLine()) : "",	
						entry.getCallerClass(),
						entry.getCaller(),
						entry.getCallerLine() > -0 ? String.valueOf(entry.getCallerLine()) : "",
						formatObjects(entry));
			}
			return true;
		} catch (Exception e) {
			System.out.println("Error formatting log: " + e);
			for (StackTraceElement ste : e.getStackTrace()) {
				System.out.println("\t" + ste);
			}
			return false;
		}
	}

	private static Object formatObjects(LogEntry entry) {
		switch (entry.kind) {
		case NONE:
			return "";
		case SETTER:
			return "\told value: " + entry.objList.get(0) + "\r\n\tnew value: "
					+ entry.objList.get(1);
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
			if (entry.objList != null && entry.objList.size() > 0) {
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
		return null;
	}

	private static String stack(List<Object> trace) {
		StringBuilder sb = new StringBuilder();
		for (Object obj : trace) {
			if (obj instanceof StackTraceElement) {
				sb.append("\t\t").append(obj).append("\r\n");
			} else if (obj instanceof LinkedHashMap) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>) obj;
				StackTraceElement ste = new StackTraceElement(
						(String) map.get("declaringClass"),
						(String) map.get("methodName"),
						(String) map.get("fileName"),
						(Integer) map.get("lineNumber"));
				sb.append("\t\t").append(ste).append("\r\n");
			} else {
				System.out.println("Funny object :" + obj + " of type "
						+ obj.getClass());
			}
		}
		return sb.toString();
	}

}
