/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.ui.wizards.files;

import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.utils.PyFileListing;

public class PythonExistingSourceGroup {

    private Text singleSelectionText;
    private Button singleBrowseButton;

    private IPath linkTarget;

    protected String errorMessage;
    protected String warningMessage;

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getWarningMessage() {
        return warningMessage;
    }

    protected void clearAllProblems() {
        errorMessage = null;
        warningMessage = null;
    }

    /**
     * The source paths that are already referenced by the project.
     */
    protected List<IPath> projectLinkTargets = new LinkedListWarningOnSlowOperations<IPath>();
    protected IProject iProject;

    public PythonExistingSourceGroup(Composite parent, IProject project, ModifyListener sourceChangeListener) {
        createContents(parent, sourceChangeListener);
        setActiveProject(project);
    }

    /**
     * Use this constructor only when extending with {@link PythonExistingSourceListGroup}.
     */
    protected PythonExistingSourceGroup() {
    }

    /**
     * Tell this group what the active project is. Doing so will update its list of linked source paths
     * that are already included in the project, which is necessary for proper conflict-checking. 
     * @param project
     */
    public void setActiveProject(IProject project) {
        if (iProject == project) {
            return;
        }

        iProject = project;
        projectLinkTargets.clear();

        if (project != null) {
            try {
                IResource[] members = project.members();
                for (IResource member : members) {
                    if (member.isLinked()) {
                        projectLinkTargets.add(member.getLocation());
                    }
                }
            } catch (CoreException e) {
                Log.log(e);
            }
            conflictCheck();
        }
    }

    protected void conflictCheck() {
        clearAllProblems();
        selectLinkTarget(Path.fromOSString(singleSelectionText.getText()));
    }

    private void createContents(final Composite parent, ModifyListener sourceChangeListener) {
        Font font = parent.getFont();

        Group group = new Group(parent, SWT.NONE);
        //setControl(group);
        GridLayout topLayout = new GridLayout();
        topLayout.numColumns = 2;
        group.setLayout(topLayout);
        GridData gd = new GridData(GridData.FILL_HORIZONTAL);
        group.setLayoutData(gd);
        group.setFont(font);
        group.setText("Location of existing source");

        singleSelectionText = new Text(group, SWT.SINGLE | SWT.BORDER);
        gd = new GridData(GridData.FILL_HORIZONTAL);
        singleSelectionText.setLayoutData(gd);
        singleSelectionText.setFont(font);
        singleSelectionText.addModifyListener(new ModifyListener() {
            public void modifyText(ModifyEvent evt) {
                clearAllProblems();
                selectLinkTarget(Path.fromOSString(singleSelectionText.getText()));
            }
        });

        singleBrowseButton = new Button(group, SWT.PUSH);
        singleBrowseButton.setText("Browse...");

        singleBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
                String selection = dialog.open();
                if (selection != null) {
                    singleSelectionText.setText(selection);
                }
            }
        });

        singleSelectionText.addModifyListener(sourceChangeListener);
    }

    /**
     * Add the selected folder path to the list of all chosen paths. While doing so, check for conflicts.
     * Issue a warning for the following selections:
     *  -folders that are subdirectories of other chosen folders, or contain other chosen folders
     *  -folders that have no .py files in them, or in one of their subdirectories
     *  
     * Issue an error if the selection contains the destination of the link to be created. Don't add
     * the selection to the list of source paths in case of an error.
     */
    protected void selectLinkTarget(IPath linkPath) {
        if (validateLinkPath(linkPath)) {
            linkTarget = linkPath;
        } else {
            linkTarget = null;
        }
    }

    protected boolean validateLinkPath(IPath linkPath) {
        if (!linkPath.toFile().exists()) {
            String segment = linkPath.lastSegment();
            if (segment == null) {
                errorMessage = "External source location must not be null.";
            } else {
                errorMessage = "External source location '" + segment + "' does not exist.";
            }
            return false;
        }

        IPath rootPath = (iProject == null ? ResourcesPlugin.getWorkspace().getRoot() : iProject).getLocation();
        if (linkPath.isPrefixOf(rootPath) || (iProject != null && rootPath.isPrefixOf(linkPath))) {
            errorMessage = "External source location '" + linkPath.lastSegment()
                    + "' overlaps with the project directory.";
            return false;
        }

        for (IPath otherPath : projectLinkTargets) {
            if (linkPath.isPrefixOf(otherPath) || otherPath.isPrefixOf(linkPath)) {
                warningMessage = "Location '" + linkPath.lastSegment()
                        + "' overlaps with the project resource '"
                        + otherPath.lastSegment()
                        + "'. This can cause unexpected side-effects.";
                break;
            }
        }

        PyFileListing pyFileListing = PythonPathHelper.getModulesBelow(linkPath.toFile(), null);
        if (pyFileListing == null || pyFileListing.getFoundPyFileInfos().size() == 0) {
            warningMessage = "Folder '" + linkPath.lastSegment()
                    + "' does not contain any Python files.";
        }

        return true;
    }

    /**
     * Return the selected source.
     * @return
     */
    public IPath getLinkTarget() {
        return linkTarget;
    }
}
