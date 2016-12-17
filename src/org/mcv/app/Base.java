package org.mcv.app;

import java.io.File;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import lombok.Cleanup;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import org.mcv.app.LogEntry.Kind;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Miguelc
 *
 */
@Getter
@ToString(exclude={"logLocation", "logLevel"})
@EqualsAndHashCode
public class Base {

	// ID fields
	final String name;
	final Class<? extends Base> clazz;

	/**
	 * Constructor.
	 * 
	 * @param name
	 * @param clazz
	 */
	public Base(String name, Class<? extends Base> clazz) {
		this.name = name;
		this.clazz = clazz;
		try {
			Properties props = new Properties();
			@Cleanup
			InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("config.props");
			props.load(is);
			String logLoc = props.getProperty(clazz.getCanonicalName() + "."
					+ name + ".logLocation", props.getProperty(
					clazz.getCanonicalName() + ".logLocation",
					props.getProperty("app.logLocation")));
			logLocation = new File(logLoc);
			String level = props.getProperty(clazz.getCanonicalName() + "."
					+ name + ".logLevel", props.getProperty(
					clazz.getCanonicalName() + ".logLevel",
					props.getProperty("app.logLevel", "DEBUG")));
			logLevel = Kind.valueOf(level);
			
			info("Log location = " + logLoc);
			if (!logLocation.exists()) {
				if(!logLocation.getName().endsWith(".log")) {
					logLocation.mkdirs();
				}
			}
			info("Log level = " + level);
		} catch (Exception e) {
			System.out.println("Could not load config.props: " + e);
		}
	}

	long version;
	long parent;
	List<Long> children;
	LocalDateTime created;
	boolean current;
	boolean deleted;

	@JsonIgnore
	boolean inBatch;
	@JsonIgnore
	String json;
	@JsonIgnore
	Application app;
	@JsonIgnore
	@Setter
	File logLocation;
	@JsonIgnore
	Kind logLevel;

	public void setLogLevel(Kind kind) {
		logLevel = kind;
		if (kind == Kind.ENTRY)
			logLevel = Kind.INFO;
		if (kind == Kind.EXIT)
			logLevel = Kind.INFO;
		if (kind == Kind.SETTER)
			logLevel = Kind.INFO;
	}

	/**
	 * Delete this object.
	 */
	public void delete() {
		if (current) {
			deleted = true;
			app.getDb().storeDeleted(this);
		}
	}

	/**
	 * Undelete this object.
	 */
	public void undelete() {
		if (current) {
			deleted = false;
			app.getDb().store(this);
		}
	}

	/**
	 * Persist this object.
	 */
	public void store() {
		app.store(this);
	}

	/**
	 * Batch persistence until explicit store()
	 */
	public void startBatch() {
		inBatch = true;
	}

	/**
	 * Undo.
	 */
	public void undo() {
		if (canUndo()) {
			app.undo(this);
		}
	}

	/**
	 * Can redo?
	 * 
	 * @return boolean
	 */
	public boolean canRedo() {
		return current && !deleted && children.size() == 1;
	}

	/**
	 * Can undo?
	 * 
	 * @return boolean
	 */
	public boolean canUndo() {
		return current && !deleted && (parent > 0);
	}

	/**
	 * Redo.
	 */
	public void redo() {
		if (canRedo()) {
			app.redo(this);
		}
	}

	/**
	 * Redo to specific version.
	 * 
	 * @param version
	 */
	public void redo(long version) {
		if (current && !deleted) {
			app.redo(this, version);
		}
	}

	/**
	 * Which versions are available for redo.
	 * 
	 * @return list of versions
	 */
	public List<Long> redoOptions() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * List of all versions.
	 * 
	 * @return list
	 */
	@SuppressWarnings("unchecked")
	public <T extends Base> List<T> getVersions() {
		return (List<T>) app.getVersions(this);
	}

	/**
	 * Make specific version the current one.
	 * 
	 * @param base
	 *            the selected version
	 */
	public void select(Base base) {
		app.select(this, base);
	}

	/**
	 * Get all the logs for this object.
	 * 
	 * @return logs
	 */
	public List<LogEntry> getLogs() {
		return app.getLogs(name, clazz.getCanonicalName());
	}

	@Ignore
	void log(LogEntry entry) {
		if (app != null) {
			app.storeLogEntry(entry);
		}
		if (logLocation != null && filter(entry)) {
			if (logLocation.isDirectory()) {
				Application.writeLog(new File(logLocation, clazz.getCanonicalName()
						+ "." + name + ".log"), entry);
			} else {
				Application.writeLog(logLocation, entry);
			}
		}
	}

	@Ignore
	private boolean filter(LogEntry entry) {
		// loglevel = DEBUG, INFO, WARN, ERROR, NONE

		if (logLevel == null) {
			logLevel = app.logLevel;
		}

		switch (entry.kind) {
		case DEBUG:
			return logLevel == Kind.DEBUG;
		case INFO:
		case ENTRY:
		case EXIT:
		case SETTER:
			return logLevel == Kind.DEBUG || logLevel == Kind.INFO;
		case WARN:
			return logLevel != Kind.ERROR && logLevel != Kind.NONE;
		case ERROR:
		default:
			return logLevel != Kind.NONE;
		}
	}

	@Ignore
	void log(Kind kind, String message, List<Object> objList) {
		
		StackTraceElement[] elts = Thread.currentThread().getStackTrace();
		int[] indices = SteUtils.findCalleeAndCaller(elts);
		
		StackTraceElement caller = elts[indices[1]];
		StackTraceElement callee = elts[indices[0]];
		
		LogEntry entry = new LogEntry();
		entry.setCaller(SteUtils.cleanup(caller.getMethodName()));
		entry.setCallerClass(SteUtils.cleanup(caller.getClassName()));
		entry.setCallerLine(caller.getLineNumber());
		
		entry.setClazz(clazz.getCanonicalName());
		
		entry.setMethod(SteUtils.cleanup(callee.getMethodName()));
		entry.setMethodClass(SteUtils.cleanup(callee.getClassName()));
		entry.setMethodLine(callee.getLineNumber());
		
		entry.setKind(kind);
		entry.setMessage(message);
		entry.setName(name);
		entry.setObjList(objList);
		entry.setThread(Thread.currentThread().getName());
		entry.setTimestamp(LocalDateTime.now());
		log(entry);
	}

	/**
	 * Log debug message.
	 * 
	 * @param message
	 */
	@Ignore
	public void debug(String message, Object... args) {
		log(Kind.DEBUG, String.format(message, args), Collections.emptyList());
	}

	/**
	 * Log info message.
	 * 
	 * @param message
	 */
	@Ignore
	public void info(String message, Object... args) {
		log(Kind.INFO, String.format(message, args), Collections.emptyList());
	}

	/**
	 * Log warning message.
	 * 
	 * @param message
	 * @param t
	 */
	@Ignore
	public void warn(Throwable t, String message, Object... args) {
		Throwable e = WrapperException.unwrap(t);
		String msg = formatMessage(String.format(message, args), e);
		log(Kind.WARN, msg, Arrays.asList((Object[])e.getStackTrace()));
	}

	@Ignore
	public void warn(String message, Object... args) {
		String msg = formatMessage(String.format(message, args), null);
		log(Kind.WARN, msg, Collections.emptyList());
	}

	/**
	 * Log error message.
	 * 
	 * @param message
	 * @param t
	 */
	@Ignore
	public void error(Throwable t, String message, Object... args) {
		Throwable e = WrapperException.unwrap(t);
		String msg = formatMessage(String.format(message, args), e);
		log(Kind.ERROR, msg, Arrays.asList((Object[])e.getStackTrace()));
	}

	@Ignore
	public void error(String message, Object... args) {
		String msg = formatMessage(String.format(message, args), null);
		log(Kind.ERROR, msg, Collections.emptyList());
	}

	@Ignore
	private String formatMessage(String message, Throwable e) {
		String msg;
		if(e == null) {
			msg = "message: " + message;
		} else if(message == null || message.length() == 0) {
			msg = "exception: " + e.toString();
		} else {
			msg = "exception: " + e.toString() + "\r\n\tmessage: " + message;
		}
		return msg;
	}
	
	/**
	 * Log method entry message.
	 * 
	 * @param args
	 */
	@Ignore
	public void entry(Object... args) {
		log(Kind.ENTRY, "method entry", Arrays.asList(args));
	}

	/**
	 * Log method exit message.
	 * 
	 * @param retval
	 */
	@Ignore
	public <T> T exit(T retval) {
		log(Kind.EXIT, "method exit", Collections.singletonList(retval));
		return retval;
	}

}
