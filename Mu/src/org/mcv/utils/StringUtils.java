package org.mcv.utils;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Iterator;

/**
 * This class provides String utilities.
 * 
 * @author miguelc
 * 
 */
public final class StringUtils {

    /**
     * Returns true if the given string is null or its length is 0.
     * 
     * @param string
     * @return true if empty
     */
    public static boolean isEmpty(String string) {
        return (string == null || string.length() == 0);
    }

    /**
     * Replace ¦ with | (for use in multivalue strings a|b|c|...)
     * 
     * @param in
     *            string to be unescaped
     * @return unescaped string
     */
    public static String mvUnescape(String in) {
        return in.replace('\u00A6', '|');
    }

    /**
     * Replace | with ¦ (for use in multivalue strings a|b|c|...)
     * 
     * @param in
     *            string to be escaped
     * @return escaped string
     */
    public static String mvEscape(String in) {
        return in.replace('|', '\u00A6');
    }

    public static String xmlEncode(String s) {
        if (s == null) {
            return s;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '<' :
                    sb.append("&lt;");
                    break;
                case '>' :
                    sb.append("&gt;");
                    break;
                case '\"' :
                    sb.append("&quot;");
                    break;
                case '&' :
                    sb.append("&amp;");
                    break;
                case '\'' :
                    sb.append("&apos;");
                    break;
                default :
                    if (c > 0x7e) {
                        sb.append("&#" + ((int) c) + ";");
                    } else sb.append(c);
            }
        }
        return sb.toString();
    }
    
    /**
     * Encodes special characters for HTML.
     * 
     * @param s
     *            input string
     * @return escaped output string
     */
    public static String htmlEncode(String s) {
        StringBuffer buf = new StringBuffer();
        if (s == null) return "";

        int len = s.length();

        for (int i = 0; i < len; i++) {
            char c = s.charAt(i);
            if (c < ' ') {
                buf.append("&#" + (int) c + ";");
            } else if (c >= ' ' && c < 0x80) {
                if (c == '"') {
                    buf.append("&quot;");
                } else if (c == '&') {
                    buf.append("&amp;");
                } else if (c == '>') {
                    buf.append("&gt;");
                } else if (c == '<') {
                    buf.append("&lt;");
                } else if (c == '\'' || c == 0x7F) {
                    buf.append("&#" + (int) c + ";");
                } else {
                    buf.append(c);
                }
            } else {
                // will be utf-8 encoded
                buf.append(c);
            }
        }
        return buf.toString();
    }

    private static void addCamelWord(StringBuffer sb, StringBuffer word) {
        if (word.length() == 0) return;

        for (int index = 0; index < word.length(); index++) {
            // if all characters are upper case CAMEL => Camel
            // else only [0] toUpper, rest leave as is
            if (!Character.isUpperCase(word.charAt(index))) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
                return;
            }
        }
        sb.append(word.charAt(0)).append(word.substring(1).toLowerCase());
    }

    /**
     * Transforms a string to camel case (transformsAStringToCamelCase).
     * 
     * @param s
     *            input string
     * @param firstUpper
     *            whether the first character should be uppercased
     * @return theCamelizedString
     */
    public static String camelize(String s, boolean firstUpper) {
        StringBuffer sb = new StringBuffer();
        StringBuffer word = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (c == '_' || c == ' ') {
                addCamelWord(sb, word);
                word = new StringBuffer();
                continue;
            }

            // skip other non alphanumerics
            if (word.length() == 0 && !Character.isJavaIdentifierStart(c)) continue;
            if (word.length() > 0 && !Character.isJavaIdentifierPart(c)) continue;

            word.append(c);
        }
        addCamelWord(sb, word);
        if (firstUpper) sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        else sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    /**
     * Transforms a camel case string to normal string
     * (transformsAStringToCamelCase =&gt; transforms a string to camel case).
     * 
     * @param s
     *            input string
     * @param firstUpper
     *            whether the first character should be uppercased
     * @return the decamelized string
     */
    public static String decamelize(String s, boolean firstUpper) {
        return decamelize(s, ' ', firstUpper, false);
    }

    public static String decamelize(String s, char separator, boolean firstUpper, boolean preserveCase) {
        if (s == null) return null;
        StringBuffer sb = new StringBuffer();
        StringBuffer word = new StringBuffer();
        boolean prevState = firstUpper;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            boolean state = Character.isUpperCase(c);
            if (state != prevState && state) {
                if (sb.length() > 0) {
                    System.out.println("Add space");
                    sb.append(separator);
                }
                if (word.length() > 1 && Character.isUpperCase(word.charAt(1))) {
                    System.out.println("Add " + word);
                    sb.append(word.toString());
                    word = new StringBuffer();
                } else if (word.length() > 0) {
                    if (!preserveCase) {
                        System.out.println("Add " + word.toString().toLowerCase());
                        sb.append(word.toString().toLowerCase());
                    } else {
                        System.out.println("Add " + word);
                        sb.append(word.toString());
                    }
                    word = new StringBuffer();
                }
            }
            prevState = state;
            word.append(c);
        }
        System.out.println("Add " + word);
        sb.append(word.toString());
        if (firstUpper && sb.length() > 0) sb.setCharAt(0, Character.toUpperCase(sb.charAt(0)));
        else if (sb.length() > 0) sb.setCharAt(0, Character.toLowerCase(sb.charAt(0)));
        return sb.toString();
    }

    /**
     * Limits a string to n characters.
     * 
     * @param s
     *            the input string
     * @param n
     *            the maximum number of characters
     * @return the original string or a truncated string with "..." appended
     */
    public static String limitString(String s, int n) {
        if (s.length() < n) return s.trim();
        return s.substring(0, n).trim() + "...";
    }

    /* internal instantiation <b>is</b> allowed */
    private StringUtils() {
    }

    public static String normalize(String s) {
        return Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFKD).replaceAll("\\P{L}", "");
    }

    public static <T> String join(final Collection<T> objs, final String delimiter) {
        if (objs == null || objs.isEmpty()) return "";
        Iterator<T> iter = objs.iterator();
        StringBuilder buffer = new StringBuilder(iter.next().toString());
        while (iter.hasNext())
            buffer.append(delimiter).append(iter.next().toString());
        return buffer.toString();
    }
}
