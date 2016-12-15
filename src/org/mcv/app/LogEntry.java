package org.mcv.app;

import java.time.LocalDateTime;

import lombok.Data;

/**
 * @author Miguelc
 *
 */
@Data
public class LogEntry {

	public enum Kind {
		DEBUG, INFO, ENTRY, EXIT, SETTER, WARN, ERROR, NONE;

		public static Kind fromInteger(int val) {
			switch (val) {
			case 0:
				return DEBUG;
			case 1:
				return INFO;
			case 2:
				return ENTRY;
			case 3:
				return EXIT;
			case 4:
				return SETTER;
			case 5:
				return WARN;
			case 6:
				return ERROR;
			case 7:
			default:
				return NONE;
			}
		}
	};

	String name;
	String clazz;
	LocalDateTime timestamp;
	String thread;
	String method;
	String caller;
	int callerLine;
	String callerClass;
	Kind kind;
	Object object1;
	Object object2;
	String message;

}
