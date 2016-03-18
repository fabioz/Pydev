/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.perspective;

import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.search.ui.NewSearchUI;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.console.IConsoleConstants;
import org.eclipse.ui.progress.IProgressConstants;
import org.python.pydev.overview_ruler.MinimapOverviewRulerPreferencesPage;
import org.python.pydev.ui.wizards.files.PythonModuleWizard;
import org.python.pydev.ui.wizards.files.PythonPackageWizard;
import org.python.pydev.ui.wizards.files.PythonSourceFolderWizard;
import org.python.pydev.ui.wizards.project.PythonProjectWizard;

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
    @Override
    public void createInitialLayout(IPageLayout layout) {
        defineLayout(layout);
        defineActions(layout);
    }

    /**
     * @param layout
     * @param editorArea
     */
    public void defineLayout(IPageLayout layout) {
        String editorArea = layout.getEditorArea();
        IFolderLayout topLeft = layout.createFolder("topLeft", IPageLayout.LEFT, (float) 0.26, editorArea); //$NON-NLS-1$
        topLeft.addView("org.python.pydev.navigator.view");

        IFolderLayout outputfolder = layout.createFolder("bottom", IPageLayout.BOTTOM, (float) 0.75, editorArea); //$NON-NLS-1$
        //outputfolder.addView(IPageLayout.ID_PROBLEM_VIEW);
        outputfolder.addPlaceholder(NewSearchUI.SEARCH_VIEW_ID);
        outputfolder.addPlaceholder(IConsoleConstants.ID_CONSOLE_VIEW);
        outputfolder.addPlaceholder(IPageLayout.ID_BOOKMARKS);
        outputfolder.addPlaceholder(IProgressConstants.PROGRESS_VIEW_ID);

        //Add the outline only if we're not using the minimap.
        if (!MinimapOverviewRulerPreferencesPage.getShowMinimapContents()) {
            layout.addView(IPageLayout.ID_OUTLINE, IPageLayout.RIGHT, (float) 0.75, editorArea);
        }
    }

    /**
     * @param layout
     */
    public void defineActions(IPageLayout layout) {
        layout.addNewWizardShortcut(PythonProjectWizard.WIZARD_ID); //$NON-NLS-1$        
        layout.addNewWizardShortcut(PythonSourceFolderWizard.WIZARD_ID); //$NON-NLS-1$        
        layout.addNewWizardShortcut(PythonPackageWizard.WIZARD_ID); //$NON-NLS-1$        
        layout.addNewWizardShortcut(PythonModuleWizard.WIZARD_ID); //$NON-NLS-1$        
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.folder");//$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.ui.wizards.new.file");//$NON-NLS-1$
        layout.addNewWizardShortcut("org.eclipse.ui.editors.wizards.UntitledTextFileWizard");//$NON-NLS-1$

        layout.addShowViewShortcut("org.python.pydev.views.PyCodeCoverageView");
        layout.addShowViewShortcut("org.python.pydev.navigator.view");
        layout.addShowViewShortcut("org.python.pydev.debug.pyunit.pyUnitView");
        layout.addShowViewShortcut(NewSearchUI.SEARCH_VIEW_ID);
        layout.addShowViewShortcut(IConsoleConstants.ID_CONSOLE_VIEW);
        layout.addShowViewShortcut(IPageLayout.ID_OUTLINE);
        layout.addShowViewShortcut(IPageLayout.ID_PROBLEM_VIEW);
        //layout.addShowViewShortcut(IPageLayout.ID_RES_NAV);-- Navigator no longer supported
        layout.addShowViewShortcut("org.eclipse.pde.runtime.LogView");
        layout.addShowViewShortcut(IPageLayout.ID_TASK_LIST);

        layout.addActionSet(IDebugUIConstants.LAUNCH_ACTION_SET);
        layout.addActionSet(IPageLayout.ID_NAVIGATE_ACTION_SET);

    }

}
