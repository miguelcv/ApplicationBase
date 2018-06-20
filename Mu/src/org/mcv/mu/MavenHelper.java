package org.mcv.mu;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jsoup.Connection;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.mcv.mu.Dependency.Coords;
import org.mcv.mu.Expr.Import;
import org.mcv.utils.Config;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Miguelc
 *
 */
@Slf4j
public class MavenHelper {

	private static class Repo {
		Repo(String url, boolean noSsl, String auth) {
			this.url = url;
			this.noSsl = noSsl;
			this.auth = auth;
		}

		String url;
		boolean noSsl;
		String auth;
	}

	private static Repo mavenCentral = new Repo("http://repo2.maven.org/maven2/", false, null);
	private static Repo jCenter = new Repo("http://jcenter.bintray.com/", false, null);
	private static String proxy;
	private static Config cfg = new Config("mu.props");
	private static String[] jarcache = cfg.getString("JARCACHE").split(",");

	static List<File> download(Coords coords, List<Repo> repos, Dependency deps) {

		if (coords.version == null) {
			coords.version = slash(getLatest(coords, repos));
			if (coords.version == null) {
				throw new Interpreter.InterpreterError("Version not found");
			} else {
				deps.coords.version = coords.version;
			}
		}

		String baseUrl = coords.path();
		String baseFile = jarcache[0] + "/" + baseUrl;

		try {
			File pom = new File(baseFile + ".pom");
			pom.getParentFile().mkdirs();
			File jar = new File(baseFile + ".jar");

			for (Repo repo : repos) {
				String name = repo.url + baseUrl + ".pom";
				Connection conn = mkConnection(name, repo);
				Response response = null;
				FileOutputStream out = null;
				try {
					response = conn.ignoreContentType(true).execute();
					out = new FileOutputStream(pom);
					out.write(response.bodyAsBytes());
					out.flush();
				} catch (Exception e) {
					throw new Interpreter.InterpreterError(e);
				} finally {
					if (out != null)
						out.close();
				}

				out = null;
				name = repo.url + baseUrl + ".jar";
				conn = mkConnection(name, repo);
				try {
					response = conn.ignoreContentType(true).execute();
					out = (new FileOutputStream(jar));
					out.write(response.bodyAsBytes());
					out.flush();
					log.debug("Written file to " + jar.getAbsolutePath());
					// all done
					List<File> list = new ArrayList<>();
					list.add(jar);
					list.add(pom);
					return list;
				} catch (Exception e) {
					throw new Interpreter.InterpreterError(e);
				} finally {
					if (out != null)
						out.close();
				}
			}
			throw new Interpreter.InterpreterError("Artifact %s not found", coords);
		} catch (Exception e) {
			throw new Interpreter.InterpreterError(e);
		}
	}

	static String getLatest(Coords coords, List<Repo> repos) {
		Map<String, String> highest = new HashMap<>();
		String href = "";
		for (Repo repo : repos) {
			String url = repo.url + coords.path();
			try {
				Connection conn = mkConnection(url, repo);
				Document doc = conn.get();
				Elements elts = doc.getElementsByTag("a");
				for (Element elt : elts) {
					String ver = slash(elt.text());
					if (ver.equals(".."))
						continue;
					if (ver.startsWith("maven-metadata"))
						continue;
					if (ver.startsWith("versions.props"))
						continue;
					String major = majorVersion(ver);
					if (isHigher(ver, highest.get(major))) {
						if (isClean(ver) || highest.get(major) == null || !isClean(highest.get(major))) {
							highest.put(major, ver);
							href = elt.attr("href");
						}
					}
				}
			} catch (Exception e) {
				log.warn("Try next repo");
			}
		}
		if (highest.isEmpty()) {
			return null;
		} else if (highest.size() == 1) {
			return href;
		} else {
			String hi = null;
			for (String key : highest.keySet()) {
				if (isHigher(key, hi))
					hi = key;
			}
			return hi;
		}
	}

	private static boolean isClean(String ver) {
		for (int i = 0; i < ver.length(); i++) {
			char c = ver.charAt(i);
			if (c != '.' && !Character.isDigit(c)) {
				return false;
			}
		}
		return true;
	}

	@SuppressWarnings("deprecation")
	private static Connection mkConnection(String url, Repo repo) {
		Connection conn = Jsoup.connect(url);
		if (proxy != null) {
			String[] parts = proxy.split("\\:");
			conn.proxy(parts[0], Integer.valueOf(parts[1]));
		}
		if (repo.noSsl) {
			conn.validateTLSCertificates(false);
		}
		if (repo.auth != null) {
			conn.header("Authorization", "Basic " + repo.auth);
		}
		conn.request().maxBodySize(0);
		conn.request().timeout(0);
		return conn;
	}

	private static String majorVersion(String version) {
		if (version.indexOf('.') >= 0) {
			return version.split("\\.")[0];
		}
		return version;
	}

	private static String slash(String text) {
		if (text.endsWith("/"))
			return text.substring(0, text.length() - 1);
		return text;
	}

	private static boolean isHigher(String text, String highest) {
		if (highest == null)
			return true;
		return new ComparableVersion(text).compareTo(new ComparableVersion(highest)) > 0;
	}

	public static File mavenImport(Import expr, Map<String, Dependency> deps, Interpreter mu) {
		
		String repo = expr.repo;
		Coords coords = new Coords(repo);
		if(deps.get(coords.toKey()) == null) {
			deps.put(coords.toKey(), new Dependency(coords));
		}
		if (coords.version == null) {
			coords.version = deps.get(coords.toKey()).coords.version;
		}
		if (coords.version != null) {
			for(Dependency dep : deps.get(coords.toKey()).dependencies) {
				File jarFile = findCached(dep.coords);
				if (jarFile != null) {
					addToClasspath(mu, jarFile);
				}				
			}
			File jarFile = findCached(coords);
			if (jarFile != null) {
				addToClasspath(mu, jarFile);
				return jarFile;
			}
		}

		// must go online
		List<Repo> repos = new ArrayList<>();
		repos.add(mavenCentral);
		repos.add(jCenter);
		for (int i = 1;; i++) {
			String url = cfg.getString("MVNREPO." + i + ".Url");
			if (url == null)
				break;
			boolean noSsl = cfg.getString("MVNREPO." + i + ".NoSsl", "false").equals("true");
			String auth = cfg.getString("MVNREPO." + i + ".Auth", "");
			repos.add(new Repo(url, noSsl, auth));
		}
		proxy = cfg.getString("proxy");
		List<File> result = download(coords, repos, deps.get(coords.toKey()));
		if (result != null) {
			addToClasspath(mu, result.get(0));
			coords.check(result.get(0));
			downloadDependencies(mu, result, repos, deps.get(coords.toKey()));
			return result.get(0);
		}
		throw new Interpreter.InterpreterError("Artifact not found: %s", coords);
	}

	private static void downloadDependencies(Interpreter mu, List<File> jarAndPom, List<Repo> repos, Dependency deps) {
		File pomFile = jarAndPom.get(1);
		List<String> dependencies = PomDeps.execute(pomFile.getAbsolutePath());
		for (String pomDep : dependencies) {
			Coords coords = new Coords(pomDep);
			Dependency dep = new Dependency(coords);
			if (coords.version.startsWith("${")) {
				coords.version = null;
			}
			List<File> result = findCachedPair(coords);
			if (result == null) {
				result = download(coords, repos, dep);				
			}
			addToClasspath(mu, result.get(0));
			downloadDependencies(mu, result, repos, dep);
			deps.dependencies.add(dep);
		}
	}

	static File findCached(Coords coords) {
		if (coords.version == null)
			return null;

		for (String cache : jarcache) {
			File jarFile = new File(cache + "/" + coords.path() + ".jar");
			if (jarFile.exists() && coords.check(jarFile)) {
				return jarFile;
			}
		}
		return null;
	}

	static List<File> findCachedPair(Coords coords) {
		if (coords.version == null)
			return null;

		List<File> result = new ArrayList<>();
		for (String cache : jarcache) {
			File jarFile = new File(cache + "/" + coords.path() + ".jar");
			if (jarFile.exists() && coords.check(jarFile)) {
				result.add(jarFile);
				File pomFile = new File(cache + "/" + coords.path() + ".pom");
				if (pomFile.exists() && coords.check(pomFile)) {
					result.add(pomFile);
					return result;
				}
			}
		}
		return null;
	}

	public static void addToClasspath(Interpreter mu, File jarFile) {
		try {
			URL url = jarFile.toURI().toURL();
			if(mu.classLoader == null) {
				mu.classLoader = new AddableURLClassLoader(new URL[] {url}, ClassLoader.getSystemClassLoader());
			} else {
			    mu.classLoader.addURL(url);
			}
		} catch (Exception e) {
			throw new Interpreter.InterpreterError(e);
		}
	}
}
