package org.mcv.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import jodd.proxetta.MethodInfo;
import jodd.proxetta.ProxyAspect;
import jodd.proxetta.impl.ProxyProxetta;
import jodd.proxetta.pointcuts.ProxyPointcutSupport;
import lombok.Cleanup;
import lombok.Data;
import lombok.ToString;
import lombok.experimental.Delegate;

import org.mcv.app.LogEntry.Kind;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author Miguelc
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
	Kind logLevel = Kind.DEBUG;
	Properties props = new Properties();

	/**
	 * Constructor.
	 * 
	 * @param name
	 */
	public Application(String name) {
		this.name = name;
		log.app = this;
		if(staticLog.app == null) staticLog.app = this;
		
		Thread.currentThread().setUncaughtExceptionHandler(
				new Thread.UncaughtExceptionHandler() {
					public void uncaughtException(Thread t, Throwable e) {
						log.error("Uncaught exception", e);
					}
				});
		try {
			@Cleanup
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.props");
			props.load(is);
		} catch (Exception e) {
			log.warn("Could not load config.props", e);
		}

		if (mapper == null) {
			mapper = new ObjectMapper();
			mapper.findAndRegisterModules();
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
		logLocation = new File(logLoc);
		if (!logLocation.exists()) {
			log.warn("Location " + logLoc + " does not exist", null);
		}

		String level = props.getProperty("app.logLevel", "DEBUG");
		logLevel = Kind.valueOf(level);
		log.info("Log level = " + level);

		log.info("Application " + name + " initialized.");
	}

	/**
	 * Set the Setter and Method proxies.
	 * 
	 * @param base
	 * @return proxied object
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
			log.error("AOP error", e);
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

	/**
	 * Create a NEW base object.
	 * 
	 * @param name
	 * @param clazz
	 * @return new object
	 */
	public <T extends Base> T create(String name, Class<? extends T> clazz) {
		try {
			log.entry(name, clazz);
			T obj = db.retrieve(name, clazz);
			if (obj == null) {
				try {
					obj = clazz.getConstructor(String.class, Class.class)
							.newInstance(name, clazz);
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
				} catch (Exception e) {
					// unique index violation: object was not retrieved because
					// deleted!
					log.error(String.format(
							"Object %s.%s exists, but has been deleted!",
							clazz, name), e);
					return log.exit(null);
				}
			} else {
				// return object retrieved from DB
				return log.exit(obj);
			}
		} catch (Exception e) {
			log.error("Exception in create()", e);
			throw new WrapperException(e);
		}
	}

	/**
	 * Serialize an object.
	 * 
	 * @param obj
	 * @return JSON string
	 */
	public static String toJson(Object obj) {
		try {
			if (obj == null)
				return "null";
			return mapper.writeValueAsString(obj);
		} catch (Exception e) {
			staticLog.warn("Error serializing JSON", e);
			return String.valueOf(obj);
		}
	}

	/**
	 * Deserialize an object.
	 * 
	 * @param json
	 * @param obj
	 * @return the object
	 */
	public static <T> T fromJson(String json, T obj) {
		try {
			return mapper.readValue(json, new TypeReference<T>() {
			});
		} catch (Exception e) {
			staticLog.warn("Error deserializing JSON", e);
			throw new WrapperException(e);
		}
	}

	/**
	 * Deserialize an object into an existing object.
	 * 
	 * @param from
	 * @param to
	 */
	public static <T extends Base> void jsonClone(T from, T to) {
		try {
			mapper.readerForUpdating(to).readValue(from.getJson());
		} catch (Exception e) {
			staticLog.warn("Error cloning JSON", e);
			throw new WrapperException(e);
		}
	}

	/**
	 * Redo.
	 * 
	 * @param base
	 */
	public <T extends Base> void redo(T base) {
		Base version = db.retrieve(base.getName(), base.getClazz(), base
				.getChildren().get(0), false);
		if (version == null) {
			base.error("Version " + base.getChildren().get(0) + " not found",
					null);
			return;
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Undo.
	 * 
	 * @param base
	 */
	public void undo(Base base) {
		Base version = db.retrieve(base.getName(), base.getClazz(),
				base.getParent(), false);
		if (version == null) {
			base.error("Version " + base.getParent() + " not found", null);
			return;
		}
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

	/**
	 * Redo to specific version.
	 * 
	 * @param base
	 */
	public void redo(Base base, long version) {
		Base ver = db.retrieve(base.getName(), base.getClazz(), version, false);
		if (ver == null) {
			base.error("Version " + version + " not found", null);
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
	 * @param version
	 */
	public <T extends Base> void select(T base, T version) {
		copy(version, base);
		base.current = true;
		db.updateCurrent(base);
	}

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
			out.printf("%s [%s] %-6s method=%s() caller=%s.%s(%d)%n%s%n", entry
					.getTimestamp().format(formatter), entry.getThread(), entry
					.getKind().toString(), entry.getMethod(), entry
					.getCallerClass(), entry.getCaller(),
					entry.getCallerLine(), formatObjects(entry));
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	private static Object formatObjects(LogEntry entry) {
		switch (entry.kind) {
		case NONE:
			return "";
		case SETTER:
			return "\told value=" + entry.object1 + " new value="
					+ entry.object2;
		case ENTRY:
			return "\targs=" + entry.object1;
		case EXIT:
			return "\treturn value=" + entry.object1;
		case WARN:
		case ERROR:
			StringBuilder sb = new StringBuilder();
			if (entry.object1 != null) {
				sb.append("\texception=").append(entry.object1);
			}
			if (entry.message != null) {
				sb.append("\r\n");
				sb.append("\tmessage=").append(entry.message);
			}
			if (entry.object2 != null) {
				sb.append("\r\n");
				sb.append("\tstack=\r\n").append(stack(entry.object2));
			}
			return sb.toString();
		case DEBUG:
		case INFO:
			if (entry.getMessage() != null) {
				return "\tmessage=" + entry.message;
			}
		}
		return null;
	}

	private static String stack(Object trace) {
		StringBuilder sb = new StringBuilder();
		if (trace instanceof String) {
			List<StackTraceElement> stack = fromJson((String) trace,
					new ArrayList<StackTraceElement>());
			for (StackTraceElement elt : stack) {
				sb.append("\t\t").append(elt).append("\r\n");
			}
		}
		if (trace instanceof StackTraceElement[]) {
			for (StackTraceElement elt : (StackTraceElement[]) trace) {
				sb.append("\t\t").append(elt).append("\r\n");
			}
		}
		return sb.toString();
	}

}
