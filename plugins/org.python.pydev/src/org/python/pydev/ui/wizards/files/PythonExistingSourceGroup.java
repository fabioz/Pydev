package org.python.pydev.ui.wizards.files;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
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

public class PythonExistingSourceGroup {

    private Text singleSelectionText;
    private Button singleBrowseButton;

    private IPath sourcePath;

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
    protected List<IPath> confirmedSourcePaths = new LinkedList<IPath>();

    public PythonExistingSourceGroup(Composite parent, ModifyListener sourceChangeListener) {
        createContents(parent, sourceChangeListener);
    }

    /**
     * Use this constructor only when extending with {@link PythonExistingSourceListGroup}.
     */
    protected PythonExistingSourceGroup() {
    }

    /**
     * Tell this group what the active project is. Doing so will update its list of source paths
     * that are already included in the project, which is necessary for proper conflict-checking. 
     * @param project
     */
    public void activeProject(IProject project) {
        //TODO set up confirmedSourcePaths
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
                sourcePath = null;
                clearAllProblems();
                selectSourcePath(Path.fromOSString(singleSelectionText.getText()));
            }
        });

        singleBrowseButton = new Button(group, SWT.PUSH);
        singleBrowseButton.setText("Browse...");

        singleBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
                singleSelectionText.setText(dialog.open());
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
    protected void selectSourcePath(IPath linkPath) {
        if (validateSourcePath(linkPath)) {
            sourcePath = linkPath;
        }
    }

    protected boolean validateSourcePath(IPath linkPath) {
        if (!linkPath.toFile().exists()) {
            String segment = linkPath.lastSegment();
            if (segment == null) {
                errorMessage = "External source location must not be null.";
            }
            else {
                errorMessage = "External source location '" + segment + "' does not exist.";
            }
            return false;
        }

        IPath rootPath = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        if (linkPath.isPrefixOf(rootPath)) {
            errorMessage = "External source location '" + linkPath.lastSegment()
                    + "' overlaps with the project directory.";
            return false;
        }

        /*List<IPath> allSourcePaths = new LinkedList<IPath>();
        allSourcePaths.addAll(sourcePaths);
        allSourcePaths.addAll(confirmedSourcePaths);*/

        for (IPath otherPath : confirmedSourcePaths) {
            if (linkPath.isPrefixOf(otherPath) || otherPath.isPrefixOf(linkPath)) {
                warningMessage = "Location '" + linkPath.lastSegment()
                        + "' overlaps with the project resource '"
                        + otherPath.lastSegment()
                        + "'. This can cause unexpected side-effects.";
                break;
            }
        }

        if (!hasPyFile(linkPath.toFile())) {
            warningMessage = "Folder '" + linkPath.lastSegment()
                    + "' does not contain any Python files.";
        }

        return true;
    }

    /**
     * Recursively search for the existence of Python files in the selected folder and its subdirectories.
     * @param file The folder to search through.
     * @return true if the folder contains a Python file; false otherwise. 
     */
    private boolean hasPyFile(File file) {
        if (file.isDirectory()) {
            File[] listFiles = file.listFiles();
            for (File innerFile : listFiles) {
                if (hasPyFile(innerFile)) {
                    return true;
                }
            }
            return false;
        }
        else {
            return file.toString().endsWith(".py");
        }
    }

    /**
     * Return the selected source.
     * @return
     */
    public IPath getSourceTarget() {
        return sourcePath;
    }
}
