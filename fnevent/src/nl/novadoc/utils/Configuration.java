package nl.novadoc.utils;

import java.io.File;
import java.io.InputStream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.filenet.api.constants.PropertyNames;
import com.filenet.api.core.Document;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.property.FilterElement;
import com.filenet.api.property.PropertyFilter;

public class Configuration {
	
	private final static String CFG_DIR = "/CodeModules/";
	protected Logger log = Logger.getLogger();

	public Logger setupLogging(Config cfg, String name) {
		if(cfg == null) {
			return log;
		} else {
			String path = cfg.getProperty("logPath", "$:/logs");
			if(path.startsWith("$:")) {
				path = findPath(path);
			}
			String level = cfg.getProperty("logLevel", "DEBUG");
			log = Logger.getLogger(new File(path), name);
			int l = 0;
			switch(level.toLowerCase()) {
			case "debug":
			default:
				l = 4;
				break;
			case "info":
				l = 3;
				break;
			case "warn":
				l = 2;
				break;
			case "error":
				l = 1;
				break;
			case "none":
				l = 0;
				break;
			}
			log.setLevel(l);
			return log;
		}		
	}

	public static String findPath(String path) {
		for(char c = 'C'; c < 'Z'; c++) {
			File f = new File(c + path.substring(1));
			if(f.exists()) {
				return f.getAbsolutePath();
			}
		}
		return null;
	}

	private String cfgPath(String name) {
		return CFG_DIR + name + ".json";
	}
	
	public Config init(ObjectStore os, String name) {
		try {
			PropertyFilter pf = new PropertyFilter();
			pf.addIncludeProperty(new FilterElement(1, null, false, PropertyNames.CONTENT_ELEMENTS, null));
			Document doc = Factory.Document.fetchInstance(os, cfgPath("config"), pf);
			InputStream is = doc.accessContentStream(0);
			return getConfig(is, name);
		} catch (Exception e) {
			log.error(e, e);
			return null;
		}
	}

	public Config getConfig(InputStream is, String name) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode toplevel = mapper.readValue(is, JsonNode.class);
			JsonNode node = toplevel.get(name);
			JsonNode general = toplevel.get("General");
			return new Config(general, node);
		} catch (Exception e) {
			log.error(e, e);
			return null;
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				log.warn(e, e);
			}
		}
	}

}
