package org.mcv.git;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;

import com.alee.laf.WebLookAndFeel;

import nl.novadoc.utils.WrapperException;
import nl.novadoc.utils.config.Config;
import nl.novadoc.utils.config.ConsoleAppConfig;
import nl.novadoc.utils.crypt.Crypt;

public class EasyGit {

	private static final String MASTER = "master";
	private static final String DEVELOP = "develop";
	private static final String FEATURE = "feature";
	private static final String RELEASE = "release";
	private static final String HOTFIX = "hotfix";
	private static final String QUIT = "quit";
	private static final String CONFIG = "config";
	private static final String SWITCH = "switch branch";
	private static final String UNDO = "undo changes";
	private static final String STASHFIX = "create hotfix";
	private static final String COMMIT = "commit";
	private static final String MERGE = "pull and push";
	private static final String START_HOTFIX = "start hotfix";
	private static final String START_RELEASE = "start release";
	private static final String START_FEATURE = "start feature";
	private static final String PUBLISH = "publish";
	private static final String FINISH = "finish";
	private static final String GITDIR = "gitDir";
	private static final String CWD = ".";
	private static final String DOT_GIT = ".git";
	private static final String ORIGIN = "origin";
	private static final String SEP = "/";
	private static final String ALL_HEADS = "+refs/heads/*:refs/remotes/origin/*";
	private static final String ALL_TAGS = "+refs/heads/*:refs/remotes/origin/*";
	private static final String TITLE = "Git menu";

	private static final int LOCAL = 0;
	private static final int REMOTE = 1;
	private static final int BRANCH = 2;
	private static final int CLEAR = 3;

	Config cfg;
	Git git;
	String currentBranch;
	boolean clean;
	int stage = LOCAL;
	List<String> menu = new ArrayList<String>();
	boolean interactive = true;
	Object cannedResponse;
	Date lastFetch = new Date(0);
	Shell shell;

	public EasyGit() {
		try {
			UIManager.setLookAndFeel(new WebLookAndFeel());
		} catch (UnsupportedLookAndFeelException e) {
			// ignore
		}
	}

	public EasyGit(Git git, Config cfg) {
		this();
		this.git = git;
		this.cfg = cfg;
		interactive = cfg.getString("interactive", "true").equals("true");
	}

	public void setShell(Shell shell) {
		this.shell = shell;
	}
	
	public static void main(String... args) {
		EasyGit eg = new EasyGit();
		eg.cfg = new ConsoleAppConfig(CONFIG, args);
		eg.interactive = eg.cfg.getString("interactive", "true").equals("true");
		eg.git = eg.init();
		eg.execute();
	}

	public void execute() {
		try {
			String command = null;
			while (true) {
				command = getResponse();
				if(command == null || command.equals(QUIT)) break;
				doCommand(command);
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		} finally {
			git.close();
		}
	}

	String getResponse() {
		menu.clear();
		currentBranch = getBranch();
		
		if (stage == LOCAL) {
			makeLocalMenu();
		}
		if (stage == REMOTE) {
			makeRemoteMenu();
		}
		if (stage == BRANCH) {
			makeBranchesMenu();
		}
		menu.add(SWITCH);
		menu.add(QUIT);
		return selectResponse(menu);
	}

	private void makeLocalMenu() {
		clean = getStatus();
		if (clean) {
			stage = REMOTE;
		} else {
			if (currentBranch.equals(MASTER)) {
				menu.add(UNDO);
				menu.add(STASHFIX);
			} else {
				menu.add(COMMIT);
				menu.add(UNDO);
			}
		}
	}

	private void makeRemoteMenu() {
		fetchAll();
		boolean exists = getRemoteExists();
		if (exists) {
			clean = getRemoteStatus();
		} else {
			clean = true;
		}

		if (!clean) {
			menu.add(MERGE);
		} else {
			stage = BRANCH;
		}
	}

	private void makeBranchesMenu() {
		fetchAll();
		boolean exists = getRemoteExists();
		if (exists) {
			clean = getBranchesStatus();
		} else {
			clean = true;
		}
		if (currentBranch.equals(MASTER)) {
			menu.add(START_HOTFIX);
		} else if (currentBranch.equals(DEVELOP)) {
			if (!clean) {
				menu.add(START_RELEASE);
			}
			menu.add(START_FEATURE);
		} else {
			menu.add(FINISH);
			if (!exists) {
				menu.add(PUBLISH);
			}
		}
		if(clean) stage = CLEAR;
	}

	public Git init() {
		try {
			File parentDir = new File(cfg.getString(GITDIR, CWD)).getCanonicalFile();
			if (!parentDir.exists()) {
				throw new GitException("Directory " + parentDir + " does not exist");
			}
			File gitDir = new File(parentDir, DOT_GIT);
			while (!gitDir.exists()) {
				gitDir = new File(gitDir.getParentFile().getParentFile().getCanonicalPath(), DOT_GIT);
			}

			git = Git.open(gitDir.getParentFile());
			
			if (!listLocalBranches().contains(MASTER)) {
				throw new GitException("Repository does not have a MASTER branch");
			}
			if (!listLocalBranches().contains(DEVELOP)) {
				throw new GitException("Repository does not have a DEVELOP branch");
			}
			if (!git.branchAllList().contains(ORIGIN + SEP + MASTER)) {
				throw new GitException("Repository does not have a remote MASTER branch");
			}
			if (!git.branchAllList().contains(ORIGIN + SEP + DEVELOP)) {
				throw new GitException("Repository does not have a remote DEVELOP branch");
			}
			
			return git;
			
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	String getBranch() {
		try {
			return git.getBranch();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	boolean getStatus() {
		try {
			return git.status();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	boolean getRemoteStatus() {
		try {
			fetchAll();
			List<String> diffs = git.diff(getBranch(), ORIGIN + SEP + getBranch());
			return diffs.isEmpty();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	boolean getBranchesStatus() {
		try {
			List<String> diffs = git.diff(DEVELOP, MASTER);
			return diffs.isEmpty();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	boolean getRemoteExists() {
		try {
			return git.branchAllList().contains(ORIGIN + SEP + getBranch());
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void fetchAll() {
		if (lastFetch.before(aMinuteAgo())) {
			return;
		}
		try {
			String username = cfg.getString("user");
			String password = Crypt.decrypt(cfg.getString("password"));
			git.remote(username, password);
			git.fetch(ALL_HEADS, ALL_TAGS);
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private Date aMinuteAgo() {
		return new Date(new Date().getTime() - 60000);
	}

	void doCommand(String command) {
		try {
			switch (command) {
			case SWITCH:
				String name = getParam("Branch name", listLocalBranches());
				if(name != null) switchBranch(name);
				break;
			case QUIT:
				break;
			case UNDO:
				undo();
				break;
			case STASHFIX:
				name = getParam("Hotfix name");
				if(name != null) stashHotfix(name);
				break;
			case COMMIT:
				String message = getParam("Commit message");
				if(message != null) commit(message);
				break;
			case MERGE:
				merge();
				break;
			case START_HOTFIX:
				name = getParam("Hotfix name");
				if(name != null) startHotfix(name);
				break;
			case START_RELEASE:
				name = getParam("Release name");
				if(name != null) startRelease(name);
				break;
			case START_FEATURE:
				name = getParam("Feature name");
				if(name != null) startFeature(name);
				break;
			case PUBLISH:
				publish();
				break;
			case FINISH:
				finish();
				break;
			default:
				break;
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	List<String> listLocalBranches() {
		try {
			return git.branchList();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void commit(String message) {
		try {
			git.add();
			git.commit(message);
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void undo() {
		try {
			git.reset("hard");
			stage = LOCAL;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void stashHotfix(String name) {
		try {
			git.stash();
			undo();
			git.createBranch(HOTFIX + SEP + name);
			currentBranch = getBranch();
			git.stashApply();
			stage = LOCAL;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void switchBranch(String branch) {
		try {
			git.checkout(branch);
			currentBranch = getBranch();
			stage = LOCAL;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	boolean merge() {
		try {
			boolean result = git.merge(ORIGIN + SEP + getBranch());
			if (result) {
				git.push();
				return true;
			} else {
				forceMerge(oursOrTheirs());
				return true;
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private void forceMerge(String strat) {
		try {
			if(strat == null) {
				throw new GitException("Merge failed");				
			}
			// clear the merge state
			git.clearMerge();
			// try merge again with OURS or THEIRS
			boolean result = git.merge(strat, ORIGIN + SEP + getBranch());
			if (result) {
				git.push();
			} else {
				throw new GitException("Merge failed");
			}
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void startHotfix(String name) {
		try {
			git.createBranch(HOTFIX + SEP + name);
			currentBranch = getBranch();
			stage = LOCAL;
		} catch (Exception e) {
			// branch already exists?
			doCommand(START_HOTFIX);
		}
	}

	void startRelease(String name) {
		try {
			git.createBranch(RELEASE + SEP + name);
			currentBranch = getBranch();
			stage = LOCAL;
		} catch (Exception e) {
			// branch already exists?
			doCommand(START_RELEASE);
		}
	}

	void startFeature(String name) {
		try {
			git.createBranch(FEATURE + SEP + name);
			currentBranch = getBranch();
			stage = LOCAL;
		} catch (Exception e) {
			// branch already exists?
			doCommand(START_FEATURE);
		}
	}

	void publish() {
		try {
			// push branch to origin
			git.push(getBranch());
			stage = LOCAL;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	void finish() {
		try {
			// fetch all
			fetchAll();
			currentBranch = getBranch();
			if (currentBranch.startsWith(HOTFIX) || currentBranch.startsWith(RELEASE)) {
				// merge hotfix into master
				git.checkout(MASTER);
				git.merge(currentBranch);
				// tag hotfix
				git.tag(postfix(currentBranch));
				// merge master into develop
				git.checkout(DEVELOP);
				git.merge(MASTER);
				// delete remote hotfix
				git.branchDeleteRemote(currentBranch);
				// delete local hotfix
				git.branchDelete(currentBranch);
				// push all
				pushAll();
			} else {
				// merge feature into develop
				git.checkout(DEVELOP);
				git.merge(currentBranch);
				// delete remote feature
				git.branchDeleteRemote(currentBranch);
				// delete local feature
				git.branchDelete(currentBranch);
				// push all (i.e. develop)
				pushAll();
			}
			stage = LOCAL;
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private void pushAll() {
		try {
			git.push();
			git.pushTags();
		} catch (Exception e) {
			throw new WrapperException(e);
		}
	}

	private String postfix(String name) {
		return name.substring(name.indexOf('/') + 1);
	}

	void close() {
		git.close();
	}

	/* INTERACTIVE */
	String selectResponse(List<String> menu) {
		if (interactive) {
			String title = "On branch " + getBranch();
			switch(stage) {
			case LOCAL:
				title += " (Local changes)";
				break;
			case REMOTE:
				title += " (Unmerged changes)";
				break;
			case BRANCH:
				title += " (Unreleased changes)";
				break;
			case CLEAR:
			default:
				title += " (All clear)";
				break;					
			}
			title += "  Please select an action";
			if (shell != null) {
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
				dialog.setElements(menu.toArray());
				dialog.setTitle(title);
				if (dialog.open() != Window.OK) {
					return null;
				}
				return (String) dialog.getFirstResult();
			} else {
				Object reply = JOptionPane.showInputDialog(null, title, TITLE,
						JOptionPane.QUESTION_MESSAGE, null, menu.toArray(), menu.get(0));
				if (reply == null) {
					return null;
				}
				return reply.toString();
			}
		} else {
			return (String) cannedResponse;
		}
	}

	String getParam(String msg) {
		if (interactive) {
			if (shell != null) {
				InputDialog dlg = new InputDialog(shell, TITLE, msg, "", null);
				dlg.open();
				if (dlg.getReturnCode() != Window.OK) {
					return null;
				}
				return dlg.getValue();
			} else {
				Object reply = JOptionPane.showInputDialog(null, msg, TITLE, JOptionPane.QUESTION_MESSAGE);
				if (reply == null)
					return null;
				return reply.toString();
			}
		} else {
			return (String) cannedResponse;
		}
	}

	String getParam(String msg, List<String> options) {
		if (interactive) {
			if (shell != null) {
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
				dialog.setElements(options.toArray());
				dialog.setTitle("Please provide a value");
				if (dialog.open() != Window.OK) {
					return null;
				}
				return (String) dialog.getFirstResult();
			} else {
				Object reply = JOptionPane.showInputDialog(null, msg, TITLE, JOptionPane.QUESTION_MESSAGE, null,
						options.toArray(), options.get(0));
				return reply.toString();
			}
		} else {
			return (String) cannedResponse;
		}
	}

	private String oursOrTheirs() {
		if (interactive) {
			String[] options = new String[] { "ours", "theirs" };
			if (shell != null) {
				ElementListSelectionDialog dialog = new ElementListSelectionDialog(shell, new LabelProvider());
				dialog.setElements(options);
				dialog.setTitle("Please select a merge strategy");
				if (dialog.open() != Window.OK) {
					return null;
				}
				return (String) dialog.getFirstResult();
			} else {
				Object reply = JOptionPane.showInputDialog(null, "Please select a merge strategy", TITLE,
						JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
				if (reply == null)
					return null;
				return reply.toString();
			}
		} else {
			return (String) cannedResponse;
		}
	}

}
