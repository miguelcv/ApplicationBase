package org.mcv.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

/**
 * A class that gets/sets configuration items from/to a properties file
 * 
 * @author Miguelc
 * 
 */
@Slf4j
public class Config {

	private Props theProperties;
	private File theFile;
	
	/**
	 * Constructor.
	 * 
	 * @param name the name of the properties file
	 */
	public Config(String name) {
        theProperties = new Props();
        theFile = new File(name);
        
        InputStream is = null;
        try {
            if (!theFile.exists()) {
                // try to find the file in the classpath
                is = Thread.currentThread().getContextClassLoader().getResourceAsStream(name);
            } else {
                // get the file from the the current directory
                is = new FileInputStream(name);
            }
            theProperties.load(is);
        } catch (Exception ioe) {
            throw new WrapperException(ioe);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException inner) {
                }
            }
        }	    
	}
	
	/**
	 * New properties source.
	 * 
	 * @param is inputstream
	 */
	public Config(InputStream is) {
		theProperties = new Props();

		try {
			theProperties.load(is);
		} catch (Exception ioe) {
			throw new WrapperException(ioe);
		} finally {
			try {
				is.close();
			} catch (Exception e) {

			}
		}
	}

	public Object getObject(String key) {
		return theProperties.getProperty(key);
	}

	public String getString(String key) {
		Object ret = getObject(key);
		if(ret == null) return null;
		return String.valueOf(getObject(key));
	}

	public synchronized void putObject(String key, Object value) {
		theProperties.put(key, String.valueOf(value));
		storeProperties();
	}

	public void putString(String key, String value) {
		putObject(key, value);
	}

	protected void storeProperties() {
		OutputStream os = null;
		try {
			os = new FileOutputStream(theFile);
			theProperties.store(os, "");
		} catch (IOException ioe) {
			log.warn("Error storing properties file " + theFile, ioe);
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (Exception inner) {
					log.warn("Error storing properties file " + theFile, inner);
				}
			}
		}
	}

	/*
	 * Multivalued items must be given as "a|b|c|...".
	 * 
	 */
	public List<Object> getList(String key) {
		List<Object> ret = new ArrayList<>();
		try {
			String val = getString(key);
			if (val == null) {
				return ret;
			}
			String[] vals = val.split("\\|");
			for (int i = 0; i < vals.length; i++) {
				ret.add(StringUtils.mvUnescape(vals[i]));
			}
			return ret;
		} catch (ClassCastException cce) {
			return ret;
		}
	}

	public Set<String> getKeys() {
		Set<String> ret = new HashSet<>();
		for (String key : theProperties.keySet()) {
		    try{if(!key.startsWith("#") && !key.startsWith("!")) {
		        ret.add(key);
		    }}catch(Exception e){ /* zero-length key? */ }
		}
		return ret;
	}

	public boolean isPersistent() {
		return true;
	}

	public File getFile() {
		return theFile;
	}

	public String getString(String key, String def) {
		String ret = getString(key);
		if(ret == null) {
			return def;
		}
		return ret;
	}
}
