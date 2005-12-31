package org.python.pydev.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.eclipse.debug.ui.IDebugUIConstants;

/**
 * Python perspective constructor
 * 
 * @author Mikko Ohtamaa
 */
public class PythonPerspectiveFactory implements IPerspectiveFactory {
	
	public static final String PERSPECTIVE_ID = "org.python.pydev.ui.PythonPerspective";

	/**
	 * Creates Python perspective layout
	 * 
	 * Copied from org.eclipse.jdt.internal.ui.JavaPerspectiveFactory
	 */
	public void createInitialLayout(IPageLayout layout) {
 		String editorArea = layout.getEditorArea();
		
		IFolderLayout folder= layout.createFolder("left", IPageLayout.LEFT, (float)0.25, editorArea); //$NON-NLS-1$
		//folder.addView(JavaUI.ID_PACKAGES);
		//folder.addView(JavaUI.ID_TYPE_HIERARCHY);
		folder.addView(IPageLayout.ID_RES_NAV);
		folder.addPlaceholder(IPageLayout.ID_RES_NAV);
		
		IFolderLayout outputfolder= layout.createFolder("bottom", IPageLayout.BOTTOM, (float)0.75, editorArea); //$NON-NLS-1$
		outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);		
		outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
		outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
		outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
		outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);
		
		layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float)0.75, editorArea);
		
		layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
		layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

		// views - search
		layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
		
		// views - debugging
		layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);

		// views - standard workbench
		layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
		layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
		layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);
				
		// new actions - Python project creation wizard
		layout.addNewWizardShortcut("org.python.pydev.ui.PythonProjectWizard"); //$NON-NLS-1$
		layout.addNewWizardShortcut("org.python.pydev.ui.PythonFileWizard"); //$NON-NLS-1$
	}

}
