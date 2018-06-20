package org.mcv.mu;

import java.net.URL;
import java.net.URLClassLoader;

public class AddableURLClassLoader extends URLClassLoader {
	
	public AddableURLClassLoader(URL[] urls, ClassLoader parent) {
		super(urls, parent);
	}

	public void addURL(URL url) {
		super.addURL(url);
	}
}