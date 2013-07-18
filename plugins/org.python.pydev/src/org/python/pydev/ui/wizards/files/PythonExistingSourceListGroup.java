package org.python.pydev.ui.wizards.files;

import java.util.LinkedList;
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
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.editors.TreeWithAddRemove;

public class PythonExistingSourceListGroup extends PythonExistingSourceGroup {

    /**
     * Tree with source folders
     */
    private TreeWithAddRemove treeSourceFolders;

    /**
     * The source paths that are selected by this group, and are to be added to a project's
     * list of referenced source locations.
     */
    private List<IPath> sourcePaths = new LinkedList<IPath>();

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
        l2.setText("Project External Source Folders\n\nChoose external folders containing source that should be used for this project."
                + "\nThese folders will be automatically added to the PYTHONPATH\n(unless the 'Don't configure PYTHONPATH' option was selected).");
        gd = new GridData();
        gd.grabExcessHorizontalSpace = true;
        gd.grabExcessVerticalSpace = false;
        l2.setLayoutData(gd);

        treeSourceFolders = new TreeWithAddRemove(parent, 0, null) {

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
                        selectSourcePath(selected);
                    }
                } else {
                    throw new AssertionError("Unexpected");
                }
            }

            @Override
            protected void handleRemove() {
                super.handleRemove();
                if (folderWasSelected()) {
                    removeFromSourceList();
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
                String newSourceFolders = StringUtils.leftAndRightTrim(treeSourceFolders.getTreeItemsAsStr(), '|');
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
        treeSourceFolders.setLayoutData(data);
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
    protected void selectSourcePath(IPath linkPath) {
        if (validateSourcePath(linkPath)) {
            sourcePaths.add(linkPath);
        }
    }

    /**
     * Remove the selected folder, and check the remaining ones to see if old conflics have been resolved.
     */
    private void removeFromSourceList() {
        sourcePaths.clear();
        clearAllProblems();
        String sourceFolders = StringUtils.leftAndRightTrim(treeSourceFolders.getTreeItemsAsStr(), '|');
        if (sourceFolders.equals("")) {
            return;
        }

        for (String pathString : StringUtils.splitAndRemoveEmptyTrimmed(sourceFolders, '|')) {
            selectSourcePath(Path.fromOSString(pathString));
        }
    }

    @Override
    protected boolean validateSourcePath(IPath linkPath) {
        if (!super.validateSourcePath(linkPath)) {
            return false;
        }

        for (IPath otherPath : sourcePaths) {
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

    public List<IPath> getExistingSourceFolders() {
        return sourcePaths;
    }

    /**
     * Return the most-recently selected source.
     * @return
     */
    @Override
    public IPath getSourceTarget() {
        return sourcePaths.get(sourcePaths.size() - 1);
    }

}
