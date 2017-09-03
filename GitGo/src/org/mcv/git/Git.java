package org.mcv.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import nl.novadoc.utils.JavaUtils;
import nl.novadoc.utils.JavaUtils.ProcResult;
import nl.novadoc.utils.StringUtils;
import nl.novadoc.utils.collections.CollectionUtils;

public class Git {
	
	File gitDir;
	
	public static Git open(File gitDir) {
		Git git = new Git();
		git.gitDir = gitDir;
		return git;
	}

	public void close() {
		/* no op */	
	}

	private List<String> runGit(String command) {
		ProcResult p = JavaUtils.runProcess("git -C " + gitDir.getAbsolutePath() + " " + command);
		if(p.getExitCode() != 0) {
			throw new GitException(p.getErrOutput());
		}
		String[] lines = p.getOutput().split("\r\n");
		List<String> ret = new ArrayList<>();
		for(String line : lines) {
			if(!StringUtils.isEmpty(line.trim())) {
				ret.add(line);
			}
		}
		return ret;
	}
	
	public boolean status() {
		try {
			return runGit("status --porcelain").isEmpty();
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public List<String> diff(String branch, String other) {
		try {
			return runGit("diff --name-only " + branch + " " + other);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public String getBranch() {
		try {
			return runGit("symbolic-ref HEAD").get(0).replace("refs/heads/", "");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public List<String> branchList() {
		try {
			List<String> branches = runGit("branch");
			List<String> ret = new ArrayList<>();
			for(String s : branches) {
				ret.add(s.replace("*", "").trim());
			}
			return ret;
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public List<String> branchAllList() {
		try {
			List<String> branches = runGit("branch -a");
			List<String> ret = new ArrayList<>();
			for(String s : branches) {
				ret.add(s.replace("*", "").replace("remotes/", "").trim());
			}
			return ret;
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void branchDelete(String branch) {
		try {
			runGit("branch -d " + branch);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void branchDeleteRemote(String branch) {
		try {
			runGit("push -d origin " + branch);
		} catch(Exception e) {
			// does not exist, OK
		}
	}

	public void add() {
		try {
			runGit("add -A");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void commit(String message) {
		try {
			runGit("commit -m \"" + message + "\"");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void reset(String type) {
		try {
			runGit("reset --" + type);
		} catch(Exception e) {
			throw new GitException(e);
		}				
	}

	public void checkout(String branch) {
		try {
			runGit("checkout " + branch);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void createBranch(String branch) {
		try {
			runGit("checkout -b " + branch);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void stash() {
		try {
			runGit("stash");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void stashApply() {
		try {
			runGit("stash apply");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void fetch(String ... what) {
		try {
			String cmd = CollectionUtils.join(Arrays.asList(what), " ");
			runGit("fetch origin " + cmd);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void push() {
		try {
			runGit("push --all");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void push(String branch) {
		try {
			runGit("push " + branch);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public boolean merge(String branch) {
		try {
			runGit("merge " + branch);
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public boolean merge(String strategy, String branch) {
		try {
			runGit("merge -s " + strategy.toLowerCase() + " " + branch);
			return true;
		} catch(Exception e) {
			return false;
		}
	}

	public void clearMerge() {
		try {
			runGit("reset --merge");  // or merge --abort
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void pushTags() {
		try {
			runGit("push --tags");
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void tag(String tag) {
		try {
			runGit("tag " + tag);
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void remote(String username, String password) {
		try {
			/* no-op ?? */
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public static Git init(File file) {
		try {
			Git git = new Git();
			git.gitDir = file.getParentFile();
			git.runGit("init " + file.getName());
			git.gitDir = file;
			return git;
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public static Git initBare(File file) {
		try {
			Git git = new Git();
			git.gitDir = file.getParentFile();
			git.runGit("init --bare " + file.getName());
			git.gitDir = file;
			return git;
		} catch(Exception e) {
			throw new GitException(e);
		}
	}

	public void addRemote(String name, String url) {
		try {
			runGit("remote add " + name + " " + url);
		} catch(Exception e) {
			throw new GitException(e);
		}		
	}

	public void undo() {
		try {
			runGit("reset --hard HEAD~1");
		} catch(Exception e) {
			throw new GitException(e);
		}		
	}

}
