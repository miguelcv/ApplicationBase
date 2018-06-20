package org.mcv.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Miguelc
 *
 */
@Slf4j
public class Props extends LinkedHashMap<String, String> {

    private static final long serialVersionUID = 1L;
    private static final int BUFSIZE = 2048;
    private File src;

    /**
     * 
     */
    public Props() {
    }

    /**
     * @param name
     */
    public Props(String name) {
        src = new File(name);
        if (!src.exists()) {
            InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            if (is != null) {
                try {
                    load(is);
                } catch (Exception e) {
                    log.error("Error accessing file " + name);
                }
            }
        } else {
            try {
                load(src);
            } catch (Exception e) {
                log.error("Error accessing file " + src.getAbsolutePath());
            }
        }
    }

    /**
     * @param f
     */
    public Props(File f) {
        try {
            src = f;
            load(f);
        } catch (Exception e) {
            log.error("Error accessing file " + f.getAbsolutePath());
        }
    }

    /**
     * @param is
     */
    public Props(InputStream is) {
        try {
            src = null;
            load(is);
        } catch (Exception e) {
            log.error("Error accessing input stream");
        } finally {
            try {
                is.close();
            } catch (Exception e) {

            }
        }
    }

    /**
     * @param key
     * @return value
     */
    public String getProperty(String key) {
        String val = get(key);
        if (val == null && !containsKey(key)) put(key, null);
        if (val == null || isComment(val)) return null;
        return val;
    }

    /**
     * @param key
     * @param def
     * @return value
     */
    public String getProperty(String key, String def) {
        String val = getProperty(key);
        return val == null ? def : val;
    }

    /**
     * @param key
     * @param value
     * @return old value
     */
    public String setProperty(String key, String value) {
        if (key.startsWith("#") || key.startsWith("!")) {
            return put(key.trim() + "=" + value.trim(), "");
        }
        return put(key.trim(), value.trim());
    }

    private class ScanLineContinuation {
        boolean haveKey;
        String key;
        StringBuilder sb;
    }

    ScanLineContinuation continuation;

    private Tuple<String, String> scanLine(String line) {
        if (line.trim().length() == 0) return null;
        boolean haveKey = false, esc = false;
        String key = null, value = null;
        StringBuilder sb = new StringBuilder();
        int i = 0;

        if (continuation != null) {
            haveKey = continuation.haveKey;
            key = continuation.key;
            sb = continuation.sb;
            /* skip initial white space */
            while (Character.isSpaceChar(line.charAt(i)))
                ++i;
            continuation = null;
        }

        for (; i < line.length(); i++) {
            char c = line.charAt(i);
            if (esc) {
                esc = false;
                switch (c) {
                    case 'r' :
                        sb.append('\r');
                        break;
                    case 'n' :
                        sb.append('\n');
                        break;
                    case 'u' :
                        String escSeq = line.substring(i + 1, i + 5);
                        int code = Integer.parseInt(escSeq);
                        i += 4;
                        sb.append((char) code);
                        break;
                    default :
                        sb.append(c);
                        break;
                }
            } else {
                switch (c) {
                    case '\\' :
                        esc = true;
                        break;
                    case '=' :
                    case ':' :
                        if (!haveKey) {
                            haveKey = true;
                            key = sb.toString().trim();
                            if (key.length() == 0) return null;
                            sb = new StringBuilder();
                            break;
                        } else {
                            sb.append(c);
                        }
                        break;
                    case '#' :
                    case '!' :
                        if (i == 0) {
                            return new Tuple<String, String>(line, "");
                        }
                        sb.append(c);
                        break;
                    default :
                        sb.append(c);
                        break;
                }
            }
        }
        if (esc) {
            continuation = new ScanLineContinuation();
            continuation.haveKey = haveKey;
            continuation.key = key;
            continuation.sb = sb;
            return null;
        }
        if (haveKey) {
            value = sb.toString().trim();
            if (value.length() == 0) value = null;
        } else {
            key = sb.toString().trim();
            if (key.length() == 0) return null;
        }
        return new Tuple<String, String>(key, value);
    }

    private boolean isComment(String s) {
        return s.startsWith("#") || s.startsWith("!");
    }

    /**
     * 
     */
    public void load() {
        try {
            if (src == null) {
                throw new WrapperException("No file assciated with Props");
            }
            @Cleanup
            FileInputStream fis = new FileInputStream(src);
            load(fis);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param f
     */
    public void load(File f) {
        src = f;
        load();
    }

    /**
     * @param is
     */
    public void load(InputStream is) {
        try {
            @Cleanup
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "utf-8"));
            load(reader);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param reader
     */
    public void load(Reader reader) {
        try {
            clear();
            StringBuilder sb = new StringBuilder(BUFSIZE);
            char[] buf = new char[BUFSIZE];
            int len = 0;
            boolean skipHeader = true;

            while ((len = reader.read(buf)) != -1) {
                sb.append(FileUtils.stripBOM(String.valueOf(buf, 0, len)));
            }
            String[] lines = sb.toString().split("\r?\n");
            for (String line : lines) {
                Tuple<String, String> t = scanLine(line);
                if (t != null) {
                    if (isComment(t.getFirst()) && skipHeader) continue;
                    skipHeader = false;
                    put(t.getFirst(), t.getSecond());
                }
            }
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param comments
     */
    public void store(String comments) {
        try {
            if (src == null) {
                throw new WrapperException("No file associated with this Props");
            }
            @Cleanup
            FileOutputStream fos = new FileOutputStream(src);
            store(fos, comments);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param f
     * @param comments
     */
    public void store(File f, String comments) {
        try {
            @Cleanup
            FileOutputStream fos = new FileOutputStream(f);
            store(fos, comments);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param os
     * @param comments
     */
    public void store(OutputStream os, String comments) {
        try {
            @Cleanup
            Writer writer = new BufferedWriter(new OutputStreamWriter(os, "utf-8"));
            store(writer, comments);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    private String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
    }

    /**
     * @param writer
     * @param comments
     */
    public void store(Writer writer, String comments) {
        try {
            if (comments == null) {
                comments = "#" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            }
            writer.write(comments);
            writer.write(System.getProperty("line.separator"));
            for (Entry<String, String> e : entrySet()) {
                writer.write(escape(e.getKey()));
                if (!isComment(e.getKey())) writer.write("=");
                if (e.getValue() != null) writer.write(escape(e.getValue()));
                writer.write(System.getProperty("line.separator"));
            }
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * @param args
     */
    public static void main(String... args) {
        try {
            File f = new File("test.properties");
            /* test File constructor */
            Props props = new Props(f);
            for (Entry<String, String> e : props.entrySet()) {
                System.out.println(e.getKey() + "=" + e.getValue());
            }
            /* test no-arg contructor */
            props = new Props();
            /* test putProperty */
            props.setProperty("hash", "#hash");
            props.setProperty("hash2", "ha#sh");
            props.setProperty("#hash", "hash");
            props.setProperty("ha#sh2", "ha#sh");

            props.setProperty("status", "ok\r\nthere");
            /* test getProperty */
            String val = props.getProperty("status");
            System.out.println(val);
            /* test GetProperty def */
            val = props.getProperty("extendedStatus", "okidoki");
            System.out.println(val);
            /* test store */
            props.store(f, null);
            /* test load */
            props.load(f);
            for (Entry<String, String> e : props.entrySet()) {
                System.out.println(e.getKey() + "=" + e.getValue());
            }
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }
}
