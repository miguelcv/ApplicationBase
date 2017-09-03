package org.mcv.git.actions;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.mcv.git.EasyGit;
import org.mcv.git.Git;

import nl.novadoc.utils.config.Config;

public class GitGoAction extends ContributionItem {

	@Override
    public void fill(Menu menu, int index) {
        MenuItem menuItem = new MenuItem(menu, SWT.CHECK, index);
        menuItem.setText("GitGo");
        menuItem.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent ev) {
        		try {
        			IFile file = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage()
        		              .getActivePart().getSite().getPage().getActiveEditor().getEditorInput()
        		              .getAdapter(IFile.class);
   					IProject project = file.getProject();
   					IPath path = project.getLocation();
   					Config cfg = new Config();
					Git git = Git.open(path.toFile());
  					EasyGit eg = new EasyGit(git, cfg);
   					eg.setShell(new Shell());
   					eg.execute();
        		} catch (Exception e) {
        			MessageDialog.openError(new Shell(), "GitGo", e.getMessage());
        		}
            }
        });
    }
	
	/**
	 * Constructor for GitGoAction.
	 */
	public GitGoAction() {
		super();
	}

	public GitGoAction(String id) {
		super(id);
	}


}
