/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 17, 2006
 */
package org.python.pydev.ui.wizards.files;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.ui.dialogs.PythonPackageSelectionDialog;
import org.python.pydev.ui.dialogs.SourceFolder;

/**
 * The default creation page may be found at org.eclipse.ui.dialogs.WizardNewFileCreationPage
 */
public abstract class AbstractPythonWizardPage extends WizardPage implements KeyListener {

    private IStructuredSelection selection;
    private Text textSourceFolder;
    private Button btBrowseSourceFolder;
    protected Text textPackage;
    private Button btBrowsePackage;
    protected Text textName;
    private String initialTextName = "";

    private PythonExistingSourceGroup existingSourceGroup;
    private IPath sourceToLink;
    /**
     * It is not null only when the source folder was correctly validated
     */
    private IContainer validatedSourceFolder;
    /**
     * It is not null only when the package was correctly validated
     */
    private IContainer validatedPackage;
    private String packageText;
    /**
     * This is the project
     */
    private IProject validatedProject;
    /**
     * It is not null only when the name was correctly validated
     */
    private String validatedName;
    private Text textProject;
    private Button btBrowseProject;

    public IContainer getValidatedSourceFolder() {
        return validatedSourceFolder;
    }

    public IContainer getValidatedPackage() {
        return validatedPackage;
    }

    public String getPackageText() {
        return packageText;
    }

    public String getValidatedName() {
        return validatedName;
    }

    public IProject getValidatedProject() {
        return validatedProject;
    }

    public IPath getSourceToLink() {
        return sourceToLink;
    }

    protected AbstractPythonWizardPage(String pageName, IStructuredSelection selection) {
        super(pageName);
        setPageComplete(false);
        this.selection = selection;
    }

    private Text lastWithFocus;
    protected String lastWithFocusStr;
    private Label labelWarningWillCreate;
    private Label labelWarningImageWillCreate;

    private void setFocusOn(Text txt, String string) {
        if (txt != null) {
            //System.out.println("seting focus on:"+string);
            txt.setFocus();
            lastWithFocus = txt;
            lastWithFocusStr = string;
        }
    }

    public void resetFocusOnLast() {
        if (lastWithFocus != null) {
            //System.out.println("reseting focus on:"+lastWithFocusStr);
            lastWithFocus.setFocus();
        }
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible == true) {
            resetFocusOnLast();
        }
    }

    @Override
    public void createControl(Composite parent) {
        // top level group
        Composite topLevel = new Composite(parent, SWT.NONE);
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = 3;
        topLevel.setLayout(gridLayout);
        topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
        topLevel.setFont(parent.getFont());

        boolean previousFilled = true;
        //create either source folder
        if (shouldCreateSourceFolderSelect()) {
            previousFilled = createSourceFolderSelect(topLevel);
        } else {
            //or the project selection
            previousFilled = createProjectSelect(topLevel);
        }

        //always call the package create (but note that it may either create the package or not).
        boolean createPackageSelectFilled = createPackageSelect(topLevel, previousFilled);
        if (shouldCreatePackageSelect()) {
            previousFilled = createPackageSelectFilled;
        }

        //always create the name
        createNameSelect(topLevel, previousFilled);

        if (shouldCreateExistingSourceFolderSelect()) {
            Label label = new Label(topLevel, SWT.NONE); //placeholder
            createSourceListGroup(topLevel);
        }

        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(topLevel);
    }

    /**
     * Decide whether a source folder must be selected to complete the dialog.
     * 
     * Subclasses can override.
     * 
     * @return true if a source folder should be selected and false if it shouldn't
     */
    protected boolean shouldCreateSourceFolderSelect() {
        return true;
    }

    /**
     * Decide whether an external source folder must be selected to complete the dialog.
     * 
     * Subclasses can override.
     * 
     * @return true if an external source should be selected and false if it shouldn't
     */
    protected boolean shouldCreateExistingSourceFolderSelect() {
        return false;
    };

    /**
     * Subclasses should override to decide whether a package must be selected to complete the dialog.
     * @return true if a package should be selected and false if it shouldn't
     */
    protected abstract boolean shouldCreatePackageSelect();

    /**
     * @param topLevel
     */
    protected void createNameSelect(Composite topLevel, boolean setFocus) {
        createNameLabel(topLevel);
        textName = new Text(topLevel, SWT.BORDER);
        textName.addKeyListener(this);
        setLayout(null, textName, null);
        if (initialTextName != null) {
            textName.setText(initialTextName);
        }
        if (setFocus) {
            setFocusOn(textName, "name");
            textName.setSelection(textName.getText().length());
        }

        //just create an empty to complete the line (that needs 3 items in the layout)
        Label label = new Label(topLevel, SWT.NONE);
        label.setText("");
    }

    protected Label createNameLabel(Composite topLevel) {
        Label label = new Label(topLevel, SWT.NONE);
        label.setText("Name");
        GridData data = new GridData();
        data.grabExcessHorizontalSpace = false;
        label.setLayoutData(data);
        return label;
    }

    private boolean createProjectSelect(Composite topLevel) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Project");
        textProject = new Text(topLevel, SWT.BORDER);
        textProject.addKeyListener(this);
        btBrowseProject = new Button(topLevel, SWT.NONE);
        setLayout(label, textProject, btBrowseProject);
        setFocusOn(textProject, "project");

        btBrowseProject.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(),
                        new WorkbenchLabelProvider());
                dialog.setTitle("Project selection");
                dialog.setMessage("Select a project.");
                dialog.setElements(ResourcesPlugin.getWorkspace().getRoot().getProjects());
                dialog.open();

                Object[] result = dialog.getResult();
                if (result != null && result.length > 0) {
                    textProject.setText(((IProject) result[0]).getName());
                    validatePage();
                }
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        Object element = selection.getFirstElement();

        if (element instanceof IAdaptable) {
            IAdaptable adaptable = (IAdaptable) element;
            element = adaptable.getAdapter(IResource.class);
        }

        if (element instanceof IResource) {
            IResource f = (IResource) element;
            element = f.getProject();
        }

        if (element instanceof IProject) {
            textProject.setText(((IProject) element).getName());
            validatePage();
            return true;
        }

        return false;
    }

    /**
     * @param topLevel
     * @return 
     */
    private boolean createPackageSelect(Composite topLevel, boolean setFocus) {
        if (shouldCreatePackageSelect()) {
            Label label;
            label = new Label(topLevel, SWT.NONE);
            label.setText("Package");
            textPackage = new Text(topLevel, SWT.BORDER);
            textPackage.addKeyListener(this);
            btBrowsePackage = new Button(topLevel, SWT.NONE);
            setLayout(label, textPackage, btBrowsePackage);

            labelWarningImageWillCreate = new Label(topLevel, SWT.NONE);
            labelWarningImageWillCreate.setVisible(false);
            labelWarningImageWillCreate.setImage(PydevPlugin.getImageCache().get(UIConstants.WARNING));

            labelWarningWillCreate = new Label(topLevel, SWT.NONE);
            labelWarningWillCreate.setText("Note: package not found (will be created).");
            labelWarningWillCreate.setVisible(false);
            setLayout(labelWarningImageWillCreate, labelWarningWillCreate, null);

            //just create an empty to complete the line (that needs 3 items in the layout)
            new Label(topLevel, SWT.NONE);

            if (setFocus) {
                setFocusOn(textPackage, "package");
            }

            btBrowsePackage.addSelectionListener(new SelectionListener() {

                @Override
                public void widgetSelected(SelectionEvent e) {
                    try {
                        PythonPackageSelectionDialog dialog = new PythonPackageSelectionDialog(getShell(), false);
                        dialog.setTitle("Package selection");
                        dialog.setMessage("Select a package (or a source folder). You may also enter the\nname of a new package in the text bar on the previous page.");
                        dialog.open();
                        Object firstResult = dialog.getFirstResult();
                        if (firstResult instanceof SourceFolder) { //it is the default package
                            SourceFolder f = (SourceFolder) firstResult;
                            textPackage.setText("");
                            textSourceFolder.setText(f.folder.getFullPath().toString());

                        }
                        if (firstResult instanceof org.python.pydev.ui.dialogs.Package) {
                            org.python.pydev.ui.dialogs.Package f = (org.python.pydev.ui.dialogs.Package) firstResult;
                            textPackage.setText(f.getPackageName());
                            textSourceFolder.setText(f.sourceFolder.folder.getFullPath().toString());
                        }
                    } catch (Exception e1) {
                        Log.log(e1);
                    }

                }

                @Override
                public void widgetDefaultSelected(SelectionEvent e) {
                }

            });

        }

        Object element = selection.getFirstElement();

        try {
            if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                element = adaptable.getAdapter(IFile.class);
                if (element == null) {
                    element = adaptable.getAdapter(IFolder.class);
                }
            }

            if (element instanceof IFile) {
                IFile f = (IFile) element;
                element = f.getParent();
            }

            if (element instanceof IFolder) {
                IFolder f = (IFolder) element;
                String srcPath = getSrcFolderFromFolder(f);
                if (srcPath == null) {
                    return false;
                }
                String complete = f.getFullPath().toString();
                if (complete.startsWith(srcPath)) {
                    complete = complete.substring(srcPath.length()).replace('/', '.');
                    if (complete.startsWith(".")) {
                        complete = complete.substring(1);
                    }
                    if (shouldCreatePackageSelect()) {
                        textPackage.setText(complete);
                    } else {
                        initialTextName = complete;
                    }
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }

        if (shouldCreatePackageSelect()) {
            return textPackage.getText().length() > 0;
        } else {
            return false;
        }
    }

    /**
     * @param topLevel
     * @return 
     */
    private boolean createSourceFolderSelect(Composite topLevel) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Source Folder");
        textSourceFolder = new Text(topLevel, SWT.BORDER);
        textSourceFolder.addKeyListener(this);
        btBrowseSourceFolder = new Button(topLevel, SWT.NONE);
        setLayout(label, textSourceFolder, btBrowseSourceFolder);

        btBrowseSourceFolder.addSelectionListener(new SelectionListener() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                try {
                    PythonPackageSelectionDialog dialog = new PythonPackageSelectionDialog(getShell(), true);
                    dialog.setTitle("Source folder selection");
                    dialog.setMessage("Select a source folder.");
                    dialog.open();
                    Object firstResult = dialog.getFirstResult();
                    if (firstResult instanceof SourceFolder) {
                        SourceFolder f = (SourceFolder) firstResult;
                        textSourceFolder.setText(f.folder.getFullPath().toString());
                    }
                } catch (Exception e1) {
                    Log.log(e1);
                }

            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {
            }

        });

        Object element = selection.getFirstElement();

        try {

            if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                element = adaptable.getAdapter(IFile.class);
                if (element == null) {
                    element = adaptable.getAdapter(IProject.class);
                }
                if (element == null) {
                    element = adaptable.getAdapter(IFolder.class);
                }
            }

            if (element instanceof IFile) {
                IFile f = (IFile) element;
                element = f.getParent();
            }

            if (element instanceof IProject) {
                IPythonPathNature nature = PythonNature.getPythonPathNature((IProject) element);
                if (nature != null) {
                    String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath(true));
                    if (srcPaths.length > 0) {
                        textSourceFolder.setText(srcPaths[0]);
                        return true;
                    }
                }

            }

            if (element instanceof IFolder) {
                IFolder f = (IFolder) element;
                String srcPath = getSrcFolderFromFolder(f);
                if (srcPath == null) {
                    return true;
                }
                textSourceFolder.setText(srcPath);
                return true;
            }

        } catch (Exception e) {
            Log.log(e);
        }
        return false;
    }

    private void createSourceListGroup(Composite parent) {
        existingSourceGroup = new PythonExistingSourceGroup(parent, getValidatedProject(), new ModifyListener() {

            @Override
            public void modifyText(ModifyEvent e) {
                sourceToLink = existingSourceGroup.getLinkTarget();
                if (sourceToLink != null) {
                    textName.setText(sourceToLink.lastSegment());
                }
                validatePage();
            }
        });
    }

    /**
     * @param f
     * @return
     * @throws CoreException
     */
    public String getSrcFolderFromFolder(IFolder f) throws CoreException {
        IPythonPathNature nature = PythonNature.getPythonPathNature(f.getProject());
        if (nature != null) {
            String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath(true));
            String relFolder = f.getFullPath().toString() + "/";
            for (String src : srcPaths) {
                if (relFolder.startsWith(src + "/")) {
                    return src;
                }
            }
        }
        return null;
    }

    private void setLayout(Label label, Control text, Control bt) {
        GridData data;

        if (label != null) {
            data = new GridData();
            data.grabExcessHorizontalSpace = false;
            label.setLayoutData(data);
        }

        if (text != null) {
            data = new GridData(GridData.FILL_HORIZONTAL);
            data.grabExcessHorizontalSpace = true;
            text.setLayoutData(data);
        }

        if (bt != null) {
            data = new GridData();
            bt.setLayoutData(data);
            if (bt instanceof Button) {
                ((Button) bt).setText("Browse...");
            }
        }
    }

    //listener interface
    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
        validatePage();
    }

    protected void validatePage() {
        try {
            if (textProject != null) {
                if (checkError(checkValidProject(textProject.getText()))) {
                    return;
                }
            }
            if (textSourceFolder != null) {
                if (checkError(checkValidSourceFolder(textSourceFolder.getText()))) {
                    return;
                }
            }
            if (textPackage != null) {
                if (checkError(checkValidPackage(textPackage.getText()))) {
                    return;
                }

            }
            if (textName != null) {
                if (checkError(checkValidName(textName.getText()))) {
                    return;
                }
            }
            if (existingSourceGroup != null) {
                if (checkError(checkValidExistingSourceFolder())) {
                    return;
                }
            }
            if (checkAdditionalErrors()) {
                return;
            }
            setErrorMessage(null);
            if (getMessage() == null) {
                setMessage(getDescription());
            }
            setPageComplete(true);
        } catch (Exception e) {
            Log.log(e);
            setErrorMessage("Error while validating page:" + e.getMessage());
            setPageComplete(false);
        }
    }

    /**
     * Subclasses may override to do additional error-checking.
     * @return
     */
    protected boolean checkAdditionalErrors() {
        return false;
    }

    private String checkValidProject(String text) {
        validatedProject = null;
        if (text == null || text.trim().length() == 0) {
            return "The project name must be filled.";
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(text);
        if (!project.exists()) {
            return "The project selected does not exist in the workspace.";
        }
        validatedProject = project;
        if (existingSourceGroup != null) {
            existingSourceGroup.setActiveProject(project);
        }
        return null;
    }

    protected boolean checkError(String error) {
        if (error != null) {
            setErrorMessage(error);
            setPageComplete(false);
            return true;
        }
        return false;
    }

    private String checkValidName(String text) {
        validatedName = null;
        String error = checkNameText(text);
        if (error != null) {
            return error;
        }
        validatedName = text;
        return null;
    }

    protected String checkNameText(String text) {
        if (text == null || text.trim().length() == 0) {
            return "The name must be filled.";
        }
        if (shouldCreateSourceFolderSelect()) {
            if (validatedSourceFolder == null) {
                return "The source folder was not correctly validated.";
            }
        } else if (validatedProject == null) {
            return "The project was not correctly validated.";
        }
        if (shouldCreatePackageSelect()) {
            if (validatedPackage == null && packageText == null) {
                return "The package was not correctly validated.";
            }
        }
        if (text.indexOf(' ') != -1) {
            return "The name may not contain spaces";
        }
        if (shouldCreatePackageSelect()) {
            //it is a new file not a new package
            if (text.indexOf('.') != -1) {
                return "The name may not contain dots";
            }
        }
        //there are certainly other invalid chars, but let's leave it like that...
        char[] invalid = new char[] { '/', '\\', ',', '*', '(', ')', '{', '}', '[', ']' };
        for (char c : invalid) {
            if (text.indexOf(c) != -1) {
                return "The name must not contain '" + c + "'.";
            }
        }

        if (text.endsWith(".")) {
            return "The name may not end with a dot";
        }
        return null;
    }

    protected String checkValidPackage(String text) {
        validatedPackage = null;
        packageText = null;
        //there is a chance that the package is the default project, so, the validation below may not be valid.
        //if(text == null || text.trim().length() == 0 ){
        //}
        String initialText = text;

        if (text.indexOf('/') != -1) {
            labelWarningImageWillCreate.setVisible(false);
            labelWarningWillCreate.setVisible(false);
            labelWarningWillCreate.getParent().layout();
            return "The package name must not contain '/'.";
        }
        if (text.indexOf('\\') != -1) {
            labelWarningImageWillCreate.setVisible(false);
            labelWarningWillCreate.setVisible(false);
            labelWarningWillCreate.getParent().layout();
            return "The package name must not contain '\\'.";
        }
        if (text.endsWith(".")) {
            labelWarningImageWillCreate.setVisible(false);
            labelWarningWillCreate.setVisible(false);
            labelWarningWillCreate.getParent().layout();
            return "The package may not end with a dot";
        }
        text = text.replace('.', '/');
        if (validatedSourceFolder == null) {
            labelWarningImageWillCreate.setVisible(false);
            labelWarningWillCreate.setVisible(false);
            labelWarningWillCreate.getParent().layout();
            return "The source folder was not correctly validated.";
        }

        IPath path = validatedSourceFolder.getFullPath().append(text);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(path);
        if (resource == null) {
            packageText = initialText;
            labelWarningImageWillCreate.setVisible(true);
            labelWarningWillCreate.setVisible(true);
            labelWarningWillCreate.getParent().layout();

            return null;
        }
        labelWarningImageWillCreate.setVisible(false);
        labelWarningWillCreate.setVisible(false);
        labelWarningWillCreate.getParent().layout();

        if (!(resource instanceof IContainer)) {
            return "The resource found for the package is not a valid container.";
        }
        if (!resource.exists()) {
            return "The package selected does not exist in the filesystem.";
        }
        validatedPackage = (IContainer) resource;
        return null;
    }

    private String checkValidSourceFolder(String text) throws CoreException {
        validatedSourceFolder = null;
        if (text == null || text.trim().length() == 0) {
            return "The source folder must be filled.";
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(text));
        if (resource == null) {
            return "The source folder was not found in the workspace.";
        }
        if (!(resource instanceof IContainer)) {
            return "The source folder was found in the workspace but is not a container.";
        }
        IProject project = resource.getProject();
        if (project == null) {
            return "Unable to find the project related to the source folder.";
        }
        IPythonPathNature nature = PythonNature.getPythonPathNature(project);
        if (nature == null) {
            return "The pydev nature is not configured on the project: " + project.getName();
        }
        String full = resource.getFullPath().toString();
        String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath(true));
        for (String str : srcPaths) {
            if (str.equals(full)) {
                validatedSourceFolder = (IContainer) resource;
                return null;
            }
        }
        return "The selected source folder is not recognized as a valid source folder.";
    }

    private String checkValidExistingSourceFolder() {
        if (existingSourceGroup.getErrorMessage() != null) {
            return existingSourceGroup.getErrorMessage();
        }
        if (sourceToLink == null) {
            return "Must enter an existing resource to link to.";
        }
        setMessage(existingSourceGroup.getWarningMessage(), WARNING);
        return null;
    }
}
