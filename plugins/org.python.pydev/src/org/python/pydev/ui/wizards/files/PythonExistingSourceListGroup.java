/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.ui.wizards.files;

import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Label;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.editors.TreeWithAddRemove;

public class PythonExistingSourceListGroup extends PythonExistingSourceGroup {

    /**
     * Tree with source folders
     */
    private TreeWithAddRemove treeLinkTargets;

    /**
     * The source paths that are selected by this group, and are to be added to a project's
     * list of referenced source locations.
     */
    private List<IPath> linkTargets = new LinkedListWarningOnSlowOperations<IPath>();

    /**
     * Creates a new instance of the widget.
     * 
     * @param parent The parent widget of the group.
     * @param sourceChangeListener The listener that reacts to when a selection is made, or when a
     * selection is removed.
     */
    public PythonExistingSourceListGroup(Composite parent, SelectionListener sourceChangeListener) {
        super();
        createContents(parent, sourceChangeListener);
    }

    protected void createContents(Composite parent, final SelectionListener sourceChangeListener) {
        GridData gd;
        GridData data;
        Label l2 = new Label(parent, SWT.NONE);
        l2.setText(
                "Project External Source Folders\n\nChoose external folders containing source that should be used for this project."
                        + "\nThese folders will be automatically added to the PYTHONPATH\n(unless the 'Don't configure PYTHONPATH' option was selected).");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        treeLinkTargets = new TreeWithAddRemove(parent, 0, null) {

            private String sourceFolders = "";

            @Override
            protected String getButtonLabel(int i) {
                switch (i) {
                    case 0:
                        return "Add external source folder";

                    default:
                        throw new AssertionError("Unexpected: " + i);

                }
            }

            @Override
            protected void customizeAddSomethingButton(Button addButton, final int nButton) {
                super.customizeAddSomethingButton(addButton, nButton);
                if (sourceChangeListener != null) {
                    addButton.addSelectionListener(sourceChangeListener);
                }
            }

            @Override
            protected void customizeRemSourceFolderButton(Button buttonRem) {
                super.customizeRemSourceFolderButton(buttonRem);
                if (sourceChangeListener != null) {
                    buttonRem.addSelectionListener(sourceChangeListener);
                }
            }

            @Override
            protected void handleAddButtonSelected(int nButton) {
                if (nButton == 0) {
                    addItemWithDialog(new DirectoryDialog(getShell()));
                    IPath selected = getSelectedFolder();
                    if (selected != null) {
                        selectLinkTarget(selected);
                    }
                } else {
                    throw new AssertionError("Unexpected");
                }
            }

            @Override
            protected void handleRemove() {
                super.handleRemove();
                if (folderWasSelected()) {
                    conflictCheck();
                }
            }

            private IPath getSelectedFolder() {
                if (!folderWasSelected()) {
                    return null;
                }

                String linkTarget = sourceFolders.substring(sourceFolders.lastIndexOf('|') + 1);
                return Path.fromOSString(linkTarget);
            }

            private boolean folderWasSelected() {
                String newSourceFolders = StringUtils.leftAndRightTrim(treeLinkTargets.getTreeItemsAsStr(), '|');
                if (sourceFolders.equals(newSourceFolders)) {
                    // cancelled
                    return false;
                }

                sourceFolders = newSourceFolders;
                return true;
            }

            @Override
            protected String getImageConstant() {
                return UIConstants.SOURCE_FOLDER_ICON;
            }

            @Override
            protected int getNumberOfAddButtons() {
                return 1;
            }

        };
        data = new GridData(GridData.FILL_BOTH);
        data.grabExcessHorizontalSpace = true;
        data.grabExcessVerticalSpace = true;
        treeLinkTargets.setLayoutData(data);
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
    @Override
    protected void selectLinkTarget(IPath linkPath) {
        if (validateLinkPath(linkPath)) {
            linkTargets.add(linkPath);
        }
    }

    @Override
    protected boolean validateLinkPath(IPath linkPath) {
        if (!super.validateLinkPath(linkPath)) {
            return false;
        }

        for (IPath otherPath : linkTargets) {
            if (linkPath.isPrefixOf(otherPath) || otherPath.isPrefixOf(linkPath)) {
                warningMessage = "Location '" + linkPath.lastSegment()
                        + "' overlaps with the selected resource '"
                        + otherPath.lastSegment()
                        + "'. This can cause unexpected side-effects.";
                break;
            }
        }

        return true;
    }

    @Override
    protected void conflictCheck() {
        linkTargets.clear();
        clearAllProblems();
        String sourceFolders = StringUtils.leftAndRightTrim(treeLinkTargets.getTreeItemsAsStr(), '|');
        if (sourceFolders.equals("")) {
            return;
        }

        for (String pathString : StringUtils.splitAndRemoveEmptyTrimmed(sourceFolders, '|')) {
            selectLinkTarget(Path.fromOSString(pathString));
        }
    }

    /**
     * Return all existing source paths chosen.
     * @return
     */
    public List<IPath> getLinkTargets() {
        return linkTargets;
    }

    /**
     * Return the most-recently selected source.
     * @return
     */
    @Override
    public IPath getLinkTarget() {
        if (linkTargets.size() == 0) {
            return null;
        }
        return linkTargets.get(linkTargets.size() - 1);
    }

}
