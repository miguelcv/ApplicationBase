package org.mcv.git.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.mcv.git.EasyGit;
import org.mcv.git.Git;

import nl.novadoc.utils.config.Config;

public class GitGoAction extends AbstractHandler {

	/**
	 * Constructor for GitGoAction.
	 */
	public GitGoAction() {
		super();
	}

	@Override
	public Object execute(ExecutionEvent ev) throws ExecutionException {
		try {
			IFile file = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart().getSite()
					.getPage().getActiveEditor().getEditorInput().getAdapter(IFile.class);
			IProject project = file.getProject();
			IPath path = project.getLocation();
			Config cfg = new Config();
			Git git = Git.open(path.toFile());
			EasyGit eg = new EasyGit(git, cfg);
			//eg.setShell(new Shell());
			eg.execute();
		} catch (Exception e) {
			MessageDialog.openError(new Shell(), "GitGo", e.getMessage());
		}
		return null;
	}

}
