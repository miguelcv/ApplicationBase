package org.mcv.app;

public class SteUtils {
	
	private SteUtils() {
		// do not instantiate me
	}
	
	public static String cleanup(String s) {
		int index = s.indexOf('$');
		if(index > 0) return s.substring(0, index);
		if(index == 0) return s.replaceAll("[_$0-9]", "");
		return s;
	}

	public static int[] findCalleeAndCaller(StackTraceElement[] elts) {
		int callee = 0;
		int caller = 0;
		String calleeName = null;
		for(int i=3; i < elts.length; i++) {
			if(calleeName == null) {
				if(isNoLoggingMethod(cleanup(elts[i].getMethodName()))) {
					callee = i;
					calleeName = cleanup(elts[i].getMethodName());
				}
			} else {
				if(!cleanup(elts[i].getMethodName()).equals(calleeName)) {
					if(isNoLoggingMethod(cleanup(elts[i].getMethodName()))) {
						caller = i;
						break;
					}
				} else {
					callee = i;
					calleeName = cleanup(elts[i].getMethodName());
				}
			}
		}
		return new int[] {callee,caller};
	}

	public static boolean isNoLoggingMethod(String name) {
		return !name.equals("log") && 
				!name.equals("entry") &&
				!name.equals("exit") &&
				!name.equals("error") &&	
				!name.equals("setter") &&
				!name.equals("info") && 
				!name.equals("warn") &&
				!name.equals("debug");
	}
}
