package nl.novadoc.tools;

import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.ConsoleAppConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	private static Config cfg;
	
	public static void main(String... args) {
		cfg = new ConsoleAppConfig("config", args);
		// TODO Auto-generated method stub
	}

}
