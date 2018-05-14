package org.mcv.mu.stdlib;

public class IText extends IString {
	
	public static String convert(Object o) {
		if(o instanceof String) return (String)o;
		if(o instanceof Integer) return new String(Character.toChars((Integer)o));
		return null; // or throw exception?
	}

}
