package nl.novadoc.tools;

import nl.novadoc.utils.FileUtils;
import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.ConsoleAppConfig;
import nl.novadoc.utils.config.sources.PropertiesSource;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Main {

	private static Map<String, List<File>> languages = new HashMap<>();
	
	public static void main(String... args) {
		Config cfg = new Config(new PropertiesSource("config.props"));
		File src = new File(cfg.getString("src"));
		File dst = new File(cfg.getString("dst"));
		log.info("Copying language files from " + src + " to " + dst);
		buildLanguages(src);
		copyLanguages(dst);
	}

	private static class DirAcceptor implements FileFilter {
		@Override
		public boolean accept(File pathname) {
			return pathname.isDirectory();
		}		
	}
	
	private static void buildLanguages(File src) {
		for(File f : src.listFiles(new DirAcceptor())) {
			for(File f2 : f.listFiles(new DirAcceptor())) {
				String lang = f2.getName();
				log.info("Gathering files for language " + lang);
				List<File> newfiles = Arrays.asList(f2.listFiles());
				List<File> files = languages.get(lang);
				if(files == null) {
					log.info("New language " + lang);
					files = new ArrayList<>();
				}
				files.addAll(newfiles);
				log.info("Number of files " + files.size());
				languages.put(lang, files);
			}
		}		
	}

	private static void copyLanguages(File dst) {
		if(!dst.exists()) {
			dst.mkdirs();
			log.info("Created directory " + dst);
		}
		for(Entry<String, List<File>> entry : languages.entrySet()) {
			File dir = new File(dst, entry.getKey());
			if(!dir.exists()) {
				dir.mkdir();
				log.info("Created directory " + dir);
			}
			for(File f : entry.getValue()) {
				File fcopy = new File(dir, f.getName());
				//fcopy.createNewFile();
				FileUtils.copyFile(f, fcopy);
				log.info("Coped file " + f.getName());
			}
		}
	}

}
