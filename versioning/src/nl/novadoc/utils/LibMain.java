package nl.novadoc.utils;

import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.sources.PropertiesSource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LibMain {

	private Config cfg = new Config(new PropertiesSource("config.props"));
	
	private LibMain() {
		
	}
	
	// PUBLIC METHODS HERE
}