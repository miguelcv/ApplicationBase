package org.mcv.mu;

import java.io.File;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Ref;

import org.mcv.utils.Config;

public class GitHelper {
	
	private static Config cfg = new Config("mu.props");
	private static String[] gitcache = cfg.getString("GITCACHE").split(",");

	static File checkoutFile(File repo, String treeish, String path) {
		try (Git git = Git.open(repo)) {
			git.checkout().setStartPoint(treeish).setForce(true).addPath(path).call();
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
			File gitrepo = new File(base, repo.replace(':', '/').replace('.', '/'));
			if(gitrepo.exists()) {
				Dependency dep = deps.get(repo);
				File file = new File(gitrepo, path);
				if(file.exists() && dep.coords.checksum != null && dep.coords.check(file)) {
					return file;
				}
				file = checkoutFile(gitrepo, treeish, path);
				dep.coords.check(file);
				dep.coords.version = getRefHash(gitrepo, treeish);
				return file;
			}
		}
		throw new Interpreter.InterpreterError("No such file: " + path);
	}
	
}
