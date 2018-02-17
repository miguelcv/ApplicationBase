package nl.novadoc.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Logger {

	private static File logFolder;
	private static Logger defaultLogger;
	private final File logFile;
	private DateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	public static final int ALL = 5;
	public static final int DEBUG = 4;
	public static final int INFO = 3;
	public static final int WARN = 2;
	public static final int ERROR = 1;
	public static final int NONE = 0;
	public static final String[] levels = { "NONE", "ERROR", "WARN", "INFO",
			"DEBUG", "ALL" };

	private int level = DEBUG; 

	private static Logger DUMMY;

	static {
		DUMMY = new Logger(null);
		DUMMY.setLevel(NONE);
	}


	public int getlevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	private Logger(File logFile) {
		defaultLogger = this;
		this.logFile = logFile;
	}

	public static Logger getLogger() {
		if(defaultLogger != null) {
			return defaultLogger;
		}
		return DUMMY;
	}
	
	public static Logger getLogger(String name) {
		try {
			if (!logFolder.exists())
				logFolder.mkdirs();
			File f = new File(logFolder, name + ".log");
			f.createNewFile();
			return new Logger(f);
			
		} catch (Exception e) {
			return DUMMY;
		}
	}

	public static Logger getLogger(File dir, String name) {
		try {
			if (!dir.exists())
				dir.mkdirs();
			logFolder = dir;
			File f = new File(logFolder, name + ".log");			
			f.createNewFile();
			return new Logger(f);
		} catch (Exception e) {
			return DUMMY;
		}
	}

	public void debug(Object msg) {
		if (level >= DEBUG)
			appendFile(logFile, msg);
	}

	public void info(Object msg) {
		if (level >= INFO)
			appendFile(logFile, msg);
	}

	public void warn(Object msg) {
		if (level >= WARN)
			appendFile(logFile, msg);
	}

	public void warn(Object msg, Throwable t) {
		if (level >= WARN) {
			appendFile(logFile, msg);
			appendFileNoHeader(logFile, t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				appendFileNoHeader(logFile, ste.toString());
			}
		}
	}

	public void error(Object msg) {
		if (level >= ERROR)
			appendFile(logFile, msg);
	}

	public void error(Object msg, Throwable t) {
		if (level >= ERROR) {
			appendFile(logFile, msg);
			appendFileNoHeader(logFile, t.toString());
			for (StackTraceElement ste : t.getStackTrace()) {
				appendFileNoHeader(logFile, ste.toString());
			}
		}
	}

	private boolean appendFile(File f, Object o) {
		return appendFile(f, o, true);
	}

	private boolean appendFileNoHeader(File f, Object o) {
		return appendFile(f, o, false);
	}

	private boolean appendFile(File f, Object o, boolean header) {
		Writer out = null;
		try {
			out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(f, true), "utf-8"));
			if (header) {
				out.write(fmt.format(new Date()));
				out.write(" " + levels[level]);
			}
			out.write("\t");
			out.write(String.valueOf(o));
			out.write("\r\n");
			out.flush();
			return true;
		} catch (IOException e) {
			return false;
		} finally {
			try {
				out.close();
			} catch (Exception e) {
			}
		}
	}

	public void setLevel(String logLevel) {
		for (int i = 0; i < ALL; i++) {
			if (logLevel.equals(levels[i])) {
				setLevel(i);
				return;
			}
		}
		setLevel(DEBUG);
	}
	
}
