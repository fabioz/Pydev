/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.dialogs.ResourceSelectionDialog;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.dialogs.ProjectFolderSelectionDialog;
import org.python.pydev.ui.editors.TreeWithAddRemove;

/**
 * This page is specified to appear in the plugin.xml
 */
public class PyProjectProperties extends PropertyPage {

    /**
     * This is the project we are editing
     */
    private IProject project;

    /**
     * Tree with source folders
     */
    private TreeWithAddRemove treeSourceFolders;

    /**
     * Tree with external folders
     */
    private TreeWithAddRemove treeExternalLibs;

    /**
     * Variables are edited here 
     */
    private TabVariables tabVariables;

    /**
     * Yes: things are tab-separated
     */
    private TabFolder tabFolder;

    /**
     * Creates contents given its parent.
     */
    @Override
    protected Control createContents(Composite p) {
        project = (IProject) getElement().getAdapter(IProject.class);

        Composite topComp = new Composite(p, SWT.NONE);
        GridLayout innerLayout = new GridLayout();
        innerLayout.numColumns = 1;
        innerLayout.marginHeight = 0;
        innerLayout.marginWidth = 0;
        topComp.setLayout(innerLayout);
        GridData gd = new GridData(GridData.FILL_BOTH);
        topComp.setLayoutData(gd);
        Label label = new Label(topComp, SWT.None);
        label.setText("The final PYTHONPATH used for a launch is composed of the paths\n"
                + "defined here, joined with the paths defined by the selected interpreter.");

        tabFolder = new TabFolder(topComp, SWT.None);
        gd = new GridData();
        gd.horizontalAlignment = SWT.FILL;
        gd.verticalAlignment = SWT.FILL;
        gd.grabExcessVerticalSpace = true;
        gd.grabExcessHorizontalSpace = true;
        gd.horizontalSpan = 1;
        tabFolder.setLayoutData(gd);

        if (project != null) {
            try {
                IPythonPathNature nature = PythonNature.getPythonPathNature(project);

                createTabProjectSourceFolders(nature.getProjectSourcePath(false));
                createTabExternalSourceFolders(nature.getProjectExternalSourcePath(false));
                tabVariables = new TabVariables(tabFolder, nature.getVariableSubstitution(false));

                createRestoreButton(topComp);
            } catch (Exception e) {
                Log.log(e);
            }

        }
        return topComp;
    }

    private void createRestoreButton(Composite topComp) {
        Button button = new Button(topComp, SWT.NONE);
        button.setText("Force restore internal info");
        button.addSelectionListener(new SelectionListener() {

            public void widgetSelected(SelectionEvent e) {
                doIt(true);
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });
    }

    private void createTabExternalSourceFolders(String externalSourcePath) {
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("External Libraries");
        Composite topComp = new Composite(tabFolder, SWT.None);
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.LIB_SYSTEM));
        topComp.setLayout(new GridLayout(1, false));

        GridData gd;
        GridData data;
        Label l2;
        l2 = new Label(topComp, SWT.None);
        l2.setText("External libraries (source folders/zips/jars/eggs) outside of the workspace.\n\n"
                + "When using variables, the final paths resolved must be filesystem absolute.\n\n"
                + "Changes in external libraries are not monitored, so, the 'Force restore internal info'\ns"
                + "hould be used if an external library changes. ");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        treeExternalLibs = new TreeWithAddRemove(topComp, 0, PythonNature.getStrAsStrItems(externalSourcePath)) {

            @Override
            protected String getImageConstant() {
                return UIConstants.LIB_SYSTEM;
            }

            @Override
            protected String getButtonLabel(int i) {
                switch (i) {
                    case 0:
                        return "Add source folder";

                    case 1:
                        return "Add zip/jar/egg";

                    case 2:
                        return "Add based on variable";

                    default:
                        throw new AssertionError("Unexpected: " + i);

                }
            }

            @Override
            protected void handleAddButtonSelected(int nButton) {
                if (nButton == 0) {
                    addItemWithDialog(new DirectoryDialog(getShell()));

                } else if (nButton == 1) {
                    addItemWithDialog(new FileDialog(getShell(), SWT.MULTI));

                } else if (nButton == 2) {
                    addItemWithDialog(new InputDialog(getShell(), "Add path to resolve with variable",
                            "Add path to resolve with variable in the format: ${VARIABLE}", "", null));

                } else {
                    throw new AssertionError("Unexpected");
                }
            }
        };
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeExternalLibs.setLayoutData(data);

        tabItem.setControl(topComp);
    }

    private void createTabProjectSourceFolders(String sourcePath) {
        TabItem tabItem = new TabItem(tabFolder, SWT.None);
        tabItem.setText("Source Folders");
        tabItem.setImage(PydevPlugin.getImageCache().get(UIConstants.SOURCE_FOLDER_ICON));
        Composite topComp = new Composite(tabFolder, SWT.None);
        topComp.setLayout(new GridLayout(1, false));

        GridData gd;
        GridData data;
        Label l2 = new Label(topComp, SWT.None);
        l2.setText("Project Source Folders (and zips/jars/eggs).\n\n"
                + "When using variables, the final paths resolved must be workspace-relative.");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        treeSourceFolders = new TreeWithAddRemove(topComp, 0, PythonNature.getStrAsStrItems(sourcePath)) {

            @Override
            protected String getButtonLabel(int i) {
                switch (i) {
                    case 0:
                        return "Add source folder";

                    case 1:
                        return "Add zip/jar/egg";

                    case 2:
                        return "Add based on variable";

                    default:
                        throw new AssertionError("Unexpected: " + i);

                }
            }

            @Override
            protected void handleAddButtonSelected(int nButton) {
                if (nButton == 0) {
                    addItemWithDialog(new ProjectFolderSelectionDialog(getShell(), project, true,
                            "Choose source folders to add to PYTHONPATH"), project);

                } else if (nButton == 1) {
                    addItemWithDialog(new ResourceSelectionDialog(getShell(), project,
                            "Choose zip/jar/egg to add to PYTHONPATH"), project);

                } else if (nButton == 2) {
                    addItemWithDialog(new InputDialog(getShell(), "Add path to resolve with variable",
                            "Add path to resolve with variable in the format: ${VARIABLE}", "", null));

                } else {
                    throw new AssertionError("Unexpected");
                }
            }

            @Override
            protected String getImageConstant() {
                return UIConstants.SOURCE_FOLDER_ICON;
            }

        };
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeSourceFolders.setLayoutData(data);

        tabItem.setControl(topComp);
    }

    /**
     * Apply only saves the new value. does not do code completion update.
     * 
     * @see org.eclipse.jface.preference.PreferencePage#performApply()
     */
    @Override
    protected void performApply() {
        doIt(false);
    }

    /**
     * Saves values into the project and updates the code completion. 
     */
    @Override
    public boolean performOk() {
        return doIt(false);
    }

    /**
     * Save the pythonpath - only updates model if asked to.
     * @return
     */
    private boolean doIt(boolean force) {
        if (project != null) {
            try {
                boolean changed = false;
                IPythonPathNature pythonPathNature = PythonNature.getPythonPathNature(project);

                String sourcePath = pythonPathNature.getProjectSourcePath(false);
                String externalSourcePath = pythonPathNature.getProjectExternalSourcePath(false);
                Map<String, String> variableSubstitution = pythonPathNature.getVariableSubstitution(false);

                String newSourcePath = StringUtils.leftAndRightTrim(treeSourceFolders.getTreeItemsAsStr(), '|');
                String newExternalSourcePath = StringUtils.leftAndRightTrim(treeExternalLibs.getTreeItemsAsStr(), '|');
                Map<String, String> newVariableSubstitution = tabVariables.getTreeItemsAsMap();

                if (checkIfShouldBeSet(sourcePath, newSourcePath)) {
                    pythonPathNature.setProjectSourcePath(newSourcePath);
                    changed = true;
                }

                if (checkIfShouldBeSet(externalSourcePath, newExternalSourcePath)) {
                    pythonPathNature.setProjectExternalSourcePath(newExternalSourcePath);
                    changed = true;
                }

                if (checkIfShouldBeSet(variableSubstitution, newVariableSubstitution)) {
                    pythonPathNature.setVariableSubstitution(newVariableSubstitution);
                    changed = true;
                }

                PythonNature pythonNature = PythonNature.getPythonNature(project);
                if (pythonNature != null && (changed || force || pythonNature.getAstManager() == null)) {
                    pythonNature.rebuildPath();
                }

            } catch (Exception e) {
                Log.log(IStatus.ERROR, "Unexpected error setting project properties", e);
            }
        }
        return true;
    }

    @SuppressWarnings({ "rawtypes" })
    private boolean checkIfShouldBeSet(Object oldVal, Object newVal) {
        if (oldVal == null) {
            if (newVal == null) {
                return false;//both null
            }
            if (newVal instanceof String) {

                String string = (String) newVal;
                if (string.trim().length() == 0) {
                    return false; //both are empty
                }

            } else if (newVal instanceof Map) {
                Map map = (Map) newVal;
                if (map.size() == 0) {
                    return false; //both are empty
                }
            } else {
                throw new AssertionError("Unexpected: " + newVal);
            }

            return true;
        }

        if (!oldVal.equals(newVal)) {
            return true;
        }
        return false;
    }
}
