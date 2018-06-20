package org.mcv.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

/**
 * This class provides utilities for file and filesystem path handling
 * 
 * @author Miguelc
 * 
 */
@Slf4j
public final class FileUtils {

    static Properties mimeProps = null;
    private static final int BUFSIZE = 1024;
    private static final char BOM = '\uFEFF';

    // static private Logger log = Logger.getLogger(FileUtils.class);

    /**
     * This class cannot be instantiated.
     */
    private FileUtils() {
        throw new InstantiationError("This class cannot be instantiated.");
    }

    /**
     * Replaces the existing file extension (if any) with the provided one
     * 
     * @param f
     *            File object
     * @param extension
     *            new extension
     * @return new File object
     */
    public static File replaceFileExtension(File f, String extension) {
        String ext = extension;
        if (!ext.startsWith(".")) ext = "." + ext;
        return new File(f.getParent() + File.separator + getFileNameWithoutExtension(f) + ext);
    }

    /**
     * Gets the filename without the extension (if any)
     * 
     * @param f
     *            a File object
     * @return the file's name with the extension stripped off
     */
    public static String getFileNameWithoutExtension(File f) {
        int index = f.getName().lastIndexOf('.');
        if (index > -1) {
            return f.getName().substring(0, index);
        }
        return f.getName();
    }

    /**
     * Gets the extension of the filename (if any)
     * 
     * @param f
     *            a File object
     * @return the file's extension (including .)
     */
    public static String getExtension(File f) {
        int index = f.getName().lastIndexOf('.');
        if (index > -1) {
            return f.getName().substring(index);
        }
        return "";
    }

    /**
     * Copies a stream to another stream. Must be file streams.
     * 
     * @param source
     *            input stream
     * @param dest
     *            output stream
     */
    public static void copyStream(FileInputStream source, FileOutputStream dest) {
        try {
            @Cleanup
            FileChannel in = source.getChannel();
            @Cleanup
            FileChannel out = dest.getChannel();
            long size = in.size();
            MappedByteBuffer buf = in.map(FileChannel.MapMode.READ_ONLY, 0, size);
            out.write(buf);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * Copies a file (using fast NIO channels).
     * 
     * @param src
     *            input File
     * @param dest
     *            output File
     */
    public static void copyFile(File src, File dest) {
        try {
            log.info("Copy file " + src);
            @SuppressWarnings("resource")
            @Cleanup
            FileChannel in = new FileInputStream(src).getChannel();
            @SuppressWarnings("resource")
			@Cleanup
            FileChannel out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            throw new WrapperException(e);
        }

    }

    /**
     * Copies a file (using fast NIO channels).
     * 
     * @param source
     *            input stream
     * @param dest
     *            output File
     */
    public static void copyFile(FileInputStream source, File dest) {
        try {
            log.info("Copy file " + source + " => " + dest);

            @Cleanup
            FileChannel in = source.getChannel();
            @SuppressWarnings("resource")
			@Cleanup
            FileChannel out = new FileOutputStream(dest).getChannel();
            in.transferTo(0, in.size(), out);
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    /**
     * Returns the basename of a pathname.
     * 
     * @param path
     *            input pathname
     * @return everything after the last directory separator
     */
    public static String basename(File path) {
        String ret = path.getName();
        int ix = ret.lastIndexOf('.');
        if (ix > -1) return ret.substring(0, ix);
        else return ret;
    }

    /**
     * Recursively deletes a directory and its contents (rm -r).
     * 
     * @param dir
     */
    public static void deleteDir(File dir) {
        File[] contents = dir.listFiles();
        for (int i = 0; i < contents.length; i++) {
            if (contents[i].isDirectory()) {
                deleteDir(contents[i]);
            }
            contents[i].delete();
        }
        dir.delete();
    }

    /**
     * Copies a directory to another.
     * 
     * @param in
     *            source directory
     * @param out
     *            destination directory
     */
    public static void copyDir(File in, File out) {
        try {
            if (!out.exists()) out.mkdirs();
            File[] list = in.listFiles();
            for (int i = 0; i < list.length; i++) {
                if (list[i].isFile()) copyFile(list[i], new File(out.getAbsolutePath() + "/" + list[i].getName()));
                else if (list[i].isDirectory() && !list[i].getName().startsWith(".")) copyDir(list[i], new File(out.getAbsolutePath() + "/" + list[i].getName()));
            }
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    public static String readFile(File f) {
        try {
            StringBuffer fileData = new StringBuffer(BUFSIZE);
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), CharsetDetector.determineCharset(f)));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(stripBOM(readData));
            }
            reader.close();
            return fileData.toString();
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    public static String validFilename(String name) {
        if (StringUtils.isEmpty(name)) return "(untitled)";
        return name.replaceAll("[?:\\\\/*\"<>|]", "_");
    }

    public static String stripBOM(String in) {
        if (in == null || in.length() < 1) return in;
        if (in.charAt(0) == BOM) return in.substring(1);
        return in;
    }

    public static String[] readLines(File f) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(f), CharsetDetector.determineCharset(f)));
            String line;
            List<String> lines = new ArrayList<>();
            while ((line = reader.readLine()) != null) {
                lines.add(stripBOM(line));
            }
            return lines.toArray(new String[0]);
        } catch (Exception e) {
            log.error(e.toString(), e);
            return new String[0];
        } finally {
            try {
                if(reader != null) {
                	reader.close();
                }
            } catch (Exception e) {
            }
        }
    }

    public static String readFile(InputStream is) {
        try {
            StringBuffer fileData = new StringBuffer(BUFSIZE);

            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            char[] buf = new char[1024];
            int numRead = 0;
            while ((numRead = reader.read(buf)) != -1) {
                String readData = String.valueOf(buf, 0, numRead);
                fileData.append(stripBOM(readData));
            }
            reader.close();
            return fileData.toString();
        } catch (Exception e) {
            throw new WrapperException(e);
        }
    }

    public static boolean writeFile(File f, String s) {
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(f), "utf-8"));
            out.write(s);
            out.flush();
            out.close();
            return true;
        } catch (IOException e) {
            log.error("{}", e, e);
            return false;
        }
    }

    public static boolean areEqual(File src, File dest) {
        if (!src.exists()) return true;
        if (!dest.exists()) return true;
        if (src.length() == dest.length() && computeHash(src).equals(computeHash(dest))) {
            return false;
        }
        return true;
    }

    public static String computeHash(File file) {
        if (!file.exists()) return "";
        DigestInputStream dis = null;
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            dis = new DigestInputStream(bis, sha1);
            // read the file and update the hash calculation
            while (dis.read() != -1);
            // get the hash value as byte array
            byte[] hash = sha1.digest();
            String str = asHex(hash);
            // log.debug("hash = " + str);
            return str;
        } catch (Exception e) {
            log.debug("hash = (not computed) because of: " + e);
            return "";
        } finally {
            try {
                dis.close();
            } catch (Exception e) {
            }
        }
    }

    public static String computeHash256(File file) {
        if (!file.exists()) return "";
        DigestInputStream dis = null;
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            dis = new DigestInputStream(bis, sha);
            // read the file and update the hash calculation
            while (dis.read() != -1);
            // get the hash value as byte array
            byte[] hash = sha.digest();
            return asHex(hash);
        } catch (Exception e) {
            log.debug("hash = (not computed) because of: " + e);
            return "";
        } finally {
            try {
                if(dis != null ) {
                	dis.close();
                }
            } catch (Exception e) {
            }
        }
    }

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private static String asHex(byte[] buf) {
        char[] chars = new char[2 * buf.length];
        for (int i = 0; i < buf.length; ++i) {
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }
        return new String(chars);
    }

    public static File findFile(String name) {
        File f = new File(name);
        if (f.exists()) return f;
        URL url = Thread.currentThread().getContextClassLoader().getResource(name);
        if (url != null) {
            try {
                f = new File(url.toURI());
            } catch (URISyntaxException e) {
                f = new File(url.getPath());
            }
            if (f.exists()) return f;
        }
        return null;
    }

}
