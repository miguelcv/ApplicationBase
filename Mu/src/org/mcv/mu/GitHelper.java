package org.mcv.mu;

import java.io.File;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;

import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.sources.PropertiesSource;

public class GitHelper {
	
	private static Config cfg = new Config(new PropertiesSource("mu.props"));
	private static String[] gitcache = cfg.getString("GITCACHE").split(",");

	static File checkoutFile(File repo, String treeish, String path) {
		try (Git git = Git.open(repo)) {
			git.checkout().setStartPoint(treeish).addPath(path).call();
			return new File(repo, path);
		} catch(Exception e) {
			throw new Interpreter.InterpreterError(e);			
		}
	}
	
	static String getRefHash(File repo, String treeish) {
		try (Git git = Git.open(repo)) {
			Ref head = git.getRepository().findRef(treeish);
	        return head.getObjectId().getName();
		} catch(Exception e) {
			throw new Interpreter.InterpreterError(e);			
		}
	}

	public static File gitImport(String repo, String treeish, String path, Map<String, Dependency> deps) {
		for(String base : gitcache) {
			File gitrepo = new File(base, repo.replace(':', '/'));
			if(gitrepo.exists()) {
				File ret = checkoutFile(gitrepo, treeish, path);
				Dependency dep = deps.get(repo);
				dep.coords.version = getRefHash(gitrepo, treeish);
				return ret;
			}
		}
		throw new Interpreter.InterpreterError("No such file: " + path);
	}
	
}
