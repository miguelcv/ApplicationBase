package nl.novadoc.utils;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.Data;

@Data
public class Config {

	ObjectMapper mapper = new ObjectMapper();
	private JsonNode cfg;
	private JsonNode general;
	private static Logger log = Logger.getLogger("config");
	
	public Config(JsonNode general, JsonNode node) {
		this.cfg = node;
		this.general = general;
	}

	public String getProperty(String key, String def) {
		JsonNode node = cfg.get(key); 
		if(node == null) {
			node = general.get(key);
		}
		if(node == null) return def;
		return node.asText(def);
	}

	public String getProperty(String key) {
		JsonNode node = cfg.get(key); 
		if(node == null) {
			node = general.get(key);
		}
		if(node == null) return null;
		return node.asText();
	}

	public List<String> getList(String key) {
		try {
			return mapper.readValue(cfg.get(key).traverse(), new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			log.error(e, e);
			return new ArrayList<String>();
		}
	}

	public String getEncryptedProperty(String key) {
		String crypt = getProperty(key);
		if(crypt.startsWith("$encrypt(")) {
			return crypt.substring("$encrypt(".length(), crypt.length() - 1);
		} else {
			return CryptoUtils.decrypt(crypt);
		}
	}

}
