package org.mcv.app;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
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
@ToString
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
			@Cleanup InputStream is = new FileInputStream("conf/config.props");
			props.load(is);
			String logLoc = props.getProperty(clazz.getCanonicalName()+"."+name+".logLocation", 
					props.getProperty(clazz.getCanonicalName()+".logLocation", 
							props.getProperty("app.logLocation")));
			info("Log location = " + logLoc);
			logLocation = new File(logLoc);
			if(!logLocation.exists()) {
				warn("Location "+logLoc+" does not exist", null);
			}
			String level = props.getProperty(clazz.getCanonicalName()+"."+name+".logLevel", 
					props.getProperty(clazz.getCanonicalName()+".logLevel", 
							props.getProperty("app.logLevel", "DEBUG")));
			logLevel = Kind.valueOf(level);
			info("Log level = " + level);
		} catch(Exception e) {
			warn("Could not load conf/config.props", e);
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
	@JsonIgnore @Setter
	File logLocation;
	@JsonIgnore
	Kind logLevel;

	public void setLogLevel(Kind kind) {
		logLevel = kind;
		if(kind == Kind.ENTRY) logLevel = Kind.INFO;
		if(kind == Kind.EXIT) logLevel = Kind.INFO;
		if(kind == Kind.SETTER) logLevel = Kind.INFO;
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
		return children;
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
		if(app == null) return;
		app.storeLogEntry(entry);
		if (filter(entry)) {
			if (logLocation != null) {
				if (logLocation.isDirectory()) {
					app.writeLog(new File(logLocation, clazz.getCanonicalName()
							+ "." + name + ".log"), entry);
				} else {
					app.writeLog(logLocation, entry);
				}
			} else if (app.logLocation != null) {
				if (app.logLocation.isDirectory()) {
					app.writeLog(
							new File(app.logLocation, clazz.getCanonicalName()
									+ "." + name + ".log"), entry);

				} else {
					app.writeLog(app.logLocation, entry);
				}
			}
		}
	}

	private boolean filter(LogEntry entry) {
		// loglevel = DEBUG, INFO, WARN, ERROR, NONE
		
		if(logLevel == null) {
			logLevel = app.logLevel;
		}
		
		switch(entry.kind) {
		case DEBUG:
			return logLevel == Kind.DEBUG;
		case INFO:
		case ENTRY:
		case EXIT:
		case SETTER:
			return logLevel == Kind.DEBUG || logLevel == Kind.INFO;
		case WARN:
			return logLevel != Kind.ERROR || logLevel != Kind.NONE;
		case ERROR:
		default:
			return logLevel != Kind.NONE;
		}
	}

	@Ignore
	void log(Kind kind, String message, Object object1, Object object2) {
		StackTraceElement caller = Thread.currentThread().getStackTrace()[3];
		StackTraceElement callee = Thread.currentThread().getStackTrace()[2];

		LogEntry entry = new LogEntry();
		entry.setCaller(caller.getMethodName());
		entry.setCallerClass(caller.getClassName());
		entry.setCallerLine(caller.getLineNumber());
		entry.setClazz(clazz.getCanonicalName());
		entry.setKind(kind);
		entry.setMessage(message);
		entry.setMethod(callee.getMethodName());
		entry.setName(name);
		entry.setObject1(object1);
		entry.setObject2(object2);
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
	public void debug(String message) {
		log(Kind.DEBUG, message, null, null);
	}

	/**
	 * Log info message.
	 * 
	 * @param message
	 */
	@Ignore
	public void info(String message) {
		log(Kind.INFO, message, null, null);
	}

	/**
	 * Log warning message.
	 * 
	 * @param message
	 * @param t
	 */
	@Ignore
	public void warn(String message, Throwable t) {
		Throwable e = WrapperException.unwrap(t);
		log(Kind.WARN, message, e.toString(), e.getStackTrace());
	}

	/**
	 * Log error message.
	 * 
	 * @param message
	 * @param t
	 */
	@Ignore
	public void error(String message, Throwable t) {
		Throwable e = WrapperException.unwrap(t);
		log(Kind.ERROR, message, e.toString(), e.getStackTrace());
	}

}
