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
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.ui.dialogs.PythonPackageSelectionDialog;
import org.python.pydev.ui.dialogs.SourceFolder;

/**
 * The default creation page may be found at org.eclipse.ui.dialogs.WizardNewFileCreationPage
 */
public abstract class PythonAbstractPathPage extends WizardPage implements KeyListener{

    private IStructuredSelection selection;
    private Text textSourceFolder;
    private Button btBrowseSourceFolder;
    private Text textPackage;
    private Button btBrowsePackage;
    private Text textName;
    private String initialTextName = "";
    /**
     * It is not null only when the source folder was correctly validated
     */
    private IContainer validatedSourceFolder;
    /**
     * It is not null only when the package was correctly validated
     */
    private IContainer validatedPackage;
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
    
    public IContainer getValidatedSourceFolder(){
        return validatedSourceFolder;
    }
    public IContainer getValidatedPackage(){
        return validatedPackage;
    }
    public String getValidatedName(){
        return validatedName;
    }
    public IProject getValidatedProject(){
        return validatedProject;
    }

    protected PythonAbstractPathPage(String pageName, IStructuredSelection selection) {
        super(pageName);
        setPageComplete(false);
        this.selection = selection;
    }

    private Text lastWithFocus;
    protected String lastWithFocusStr;
    private void setFocusOn(Text txt, String string) {
        if(txt != null){
            //System.out.println("seting focus on:"+string);
            txt.setFocus();
            lastWithFocus = txt;
            lastWithFocusStr = string;
        }
    }
    public void resetFocusOnLast(){
        if(lastWithFocus != null){
            //System.out.println("reseting focus on:"+lastWithFocusStr);
            lastWithFocus.setFocus();
        }
    }
    
    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if(visible == true){
            resetFocusOnLast();
        }
    }

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
        if(shouldCreateSourceFolderSelect()){
            previousFilled = createSourceFolderSelect(topLevel);
        }else{
            //or the project selection
            previousFilled = createProjectSelect(topLevel);
        }
        
        //always call the package create (but not always will it create
        if(shouldCreatePackageSelect()){
            previousFilled = createPackageSelect(topLevel, previousFilled);
        }else{
            createPackageSelect(topLevel, previousFilled);
        }

        //always create the name
        createNameSelect(topLevel, previousFilled);
        
        // Show description on opening
        setErrorMessage(null);
        setMessage(null);
        setControl(topLevel);
    }
    protected boolean shouldCreateSourceFolderSelect() {
        return true;
    }
    protected abstract boolean shouldCreatePackageSelect() ;

    
    /**
     * @param topLevel
     */
    private void createNameSelect(Composite topLevel, boolean setFocus) {
        Label label;
        label = new Label(topLevel, SWT.NONE);
        label.setText("Name");
        textName = new Text(topLevel, SWT.BORDER);
        textName.addKeyListener(this);
        setLayout(label, textName, null);
        if(initialTextName != null){
            textName.setText(initialTextName);
        }
        if(setFocus){
            setFocusOn(textName, "name");
            textName.setSelection(textName.getText().length());
        }
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

        btBrowseProject.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                ElementListSelectionDialog dialog = new ElementListSelectionDialog(getShell(), new WorkbenchLabelProvider());
                dialog.setTitle("Project selection");
                dialog.setTitle("Select a project.");
                dialog.setElements(ResourcesPlugin.getWorkspace().getRoot().getProjects());
                dialog.open();
                
                Object[] result = dialog.getResult();
                if(result != null && result.length > 0){
                    textProject.setText(((IProject)result[0]).getName());
                }
            }

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
            textProject.setText(((IProject)element).getName());
            return true;
        }
            

        return false;
    }

    /**
     * @param topLevel
     * @return 
     */
    private boolean createPackageSelect(Composite topLevel, boolean setFocus) {
        if(shouldCreatePackageSelect()){
            Label label;
            label = new Label(topLevel, SWT.NONE);
            label.setText("Package");
            textPackage = new Text(topLevel, SWT.BORDER);
            textPackage.addKeyListener(this);
            btBrowsePackage = new Button(topLevel, SWT.NONE);
            setLayout(label, textPackage, btBrowsePackage);
    
            if(setFocus){
                setFocusOn(textPackage, "package");
            }
    
            btBrowsePackage.addSelectionListener(new SelectionListener(){
    
                public void widgetSelected(SelectionEvent e) {
                    try {
                        PythonPackageSelectionDialog dialog = new PythonPackageSelectionDialog(getShell(), false);
                        dialog.open();
                        Object firstResult = dialog.getFirstResult();
                        if(firstResult instanceof SourceFolder){ //it is the default package
                            SourceFolder f = (SourceFolder) firstResult;
                            textPackage.setText("");
                            textSourceFolder.setText(f.folder.getFullPath().toString());
                            
                        }
                        if(firstResult instanceof org.python.pydev.ui.dialogs.Package){
                            org.python.pydev.ui.dialogs.Package f = (org.python.pydev.ui.dialogs.Package) firstResult;
                            textPackage.setText(f.getPackageName());
                            textSourceFolder.setText(f.sourceFolder.folder.getFullPath().toString());
                        }
                    } catch (Exception e1) {
                        PydevPlugin.log(e1);
                    }
                    
                }
    
                public void widgetDefaultSelected(SelectionEvent e) {
                }
                
            });
        }
        
        Object element = selection.getFirstElement();
        
        try {
            if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                element = adaptable.getAdapter(IFile.class);
                if(element == null){
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
                if(srcPath == null){
                    return false;
                }
                String complete = f.getFullPath().toString();
                if(complete.startsWith(srcPath)){
                    complete = complete.substring(srcPath.length()).replace('/', '.');
                    if(complete.startsWith(".")){
                        complete = complete.substring(1);
                    }
                    if(shouldCreatePackageSelect()){
                        textPackage.setText(complete);
                    }else{
                        initialTextName = complete;
                    }
                }
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e);
        }

        if(shouldCreatePackageSelect()){
            return textPackage.getText().length() > 0;
        }else{
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
        
        
        btBrowseSourceFolder.addSelectionListener(new SelectionListener(){

            public void widgetSelected(SelectionEvent e) {
                try {
                    PythonPackageSelectionDialog dialog = new PythonPackageSelectionDialog(getShell(), true);
                    dialog.open();
                    Object firstResult = dialog.getFirstResult();
                    if(firstResult instanceof SourceFolder){
                        SourceFolder f = (SourceFolder) firstResult;
                        textSourceFolder.setText(f.folder.getFullPath().toString());
                    }
                } catch (Exception e1) {
                    PydevPlugin.log(e1);
                }
                
            }

            public void widgetDefaultSelected(SelectionEvent e) {
            }
            
        });

        
        
        Object element = selection.getFirstElement();
        
        try {
            
            if (element instanceof IAdaptable) {
                IAdaptable adaptable = (IAdaptable) element;
                element = adaptable.getAdapter(IFile.class);
                if(element == null){
                    element = adaptable.getAdapter(IProject.class);
                }
                if(element == null){
                    element = adaptable.getAdapter(IFolder.class);
                }
            }

            if (element instanceof IFile) {
                IFile f = (IFile) element;
                element = f.getParent();
            }
            
            if (element instanceof IProject) {
                IPythonPathNature nature = PythonNature.getPythonPathNature((IProject) element);
                if(nature != null){
                    String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath());
                    if(srcPaths.length > 0){
                        textSourceFolder.setText(srcPaths[0]);
                        return true;
                    }
                }
                
            }
            
            if (element instanceof IFolder) {
                IFolder f = (IFolder) element;
                String srcPath = getSrcFolderFromFolder(f);
                if(srcPath == null){
                    return true;
                }
                textSourceFolder.setText(srcPath);
                return true;
            }

            
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        return false;
    }

    /**
     * @param f
     * @return
     * @throws CoreException
     */
    public String getSrcFolderFromFolder(IFolder f) throws CoreException {
        IPythonPathNature nature = PythonNature.getPythonPathNature(f.getProject());
        if(nature != null){
            String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath());
            String relFolder = f.getFullPath().toString()+"/";
            for (String src : srcPaths) {
                if(relFolder.startsWith(src+"/")){
                    return src;
                }
            }
        }
        return null;
    }

    private void setLayout(Label label, Text text, Button bt) {
        GridData data;
        
        data = new GridData();
        data.grabExcessHorizontalSpace = false;
        label.setLayoutData(data);
        
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.grabExcessHorizontalSpace = true;
        text.setLayoutData(data);
        
        if(bt != null){
            data = new GridData();
            bt.setLayoutData(data);
            bt.setText("Browse...");
        }
    }

    
    
    
    
    //listener interface
    public void keyPressed(KeyEvent e) {
    }
    
    public void keyReleased(KeyEvent e) {
        validatePage();
    }

    private void validatePage() {
        try {
            if (textProject != null) {
                if(checkError(checkValidProject(textProject.getText()))){
                    return;
                }
            }
            if (textSourceFolder != null) {
                if(checkError(checkValidSourceFolder(textSourceFolder.getText()))){
                    return;
                }
            }
            if (textPackage != null) {
                if(checkError(checkValidPackage(textPackage.getText()))){
                    return;
                }

            }
            if (textName != null) {
                if(checkError(checkValidName(textName.getText()))){
                    return;
                }
            }
            setErrorMessage(null);
            setMessage("Page validated.");
            setPageComplete(true);
        } catch (Exception e) {
            PydevPlugin.log(e);
            setErrorMessage("Error while validating page:"+e.getMessage());
            setPageComplete(false);
        }
    }
    
    private String checkValidProject(String text) {
        validatedProject = null;
        if(text == null || text.trim().length() == 0 ){
            return "The project name must be filled.";
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject project = root.getProject(text);
        if(!project.exists()){
            return "The project selected does not exist in the workspace.";
        }
        validatedProject = project;
        return null;
    }
    
    private boolean checkError(String error) {
        if (error != null) {
            setErrorMessage(error);
            setPageComplete(false);
            return true;
        }
        return false;
    }


    private String checkValidName(String text) {
        validatedName = null;
        if(text == null || text.trim().length() == 0 ){
            return "The name must be filled.";
        }
        if(shouldCreateSourceFolderSelect()){
            if(validatedSourceFolder == null){
                return "The source folder was not correctly validated.";
            }
        }else if(validatedProject == null){
            return "The project was not correctly validated.";
        }
        if(shouldCreatePackageSelect()){
            if(validatedPackage == null){
                return "The package was not correctly validated.";
            }
        }
        if(text.indexOf(' ') != -1){
            return "The name may not contain spaces";
        }
        if(shouldCreatePackageSelect()){
            //it is a new file not a new package
            if(text.indexOf('.') != -1){
                return "The name may not contain dots";
            }
        }
        //there are certainly other invalid chars, but let's leave it like that...
        char[] invalid = new char[]{'/', '\\', ',', '*', '(', ')', '{', '}','[',']'
                };
        for (char c : invalid){
            if(text.indexOf(c) != -1){
                return "The name must not contain '"+c+"'.";
            }
        }

        if(text.endsWith(".")){
            return "The name may not end with a dot";
        }
        validatedName = text;
        return null;
    }

    private String checkValidPackage(String text) {
        validatedPackage = null;
        //there is a chance that the package is the default project, so, the validation below may not be valid.
        //if(text == null || text.trim().length() == 0 ){
        //}
        
        if(text.indexOf('/') != -1){
            return "The package name must not contain '/'.";
        }
        if(text.indexOf('\\') != -1){
            return "The package name must not contain '\\'.";
        }
        if(text.endsWith(".")){
            return "The package may not end with a dot";
        }
        text = text.replace('.', '/');
        if(validatedSourceFolder == null){
            return "The source folder was not correctly validated.";
        }
        IPath path = validatedSourceFolder.getFullPath().append(text);
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(path);
        if(resource == null){
            return "The package was not found in the workspace.";
        }
        if(!(resource instanceof IContainer)){
            return "The resource found for the package is not a valid container.";
        }
        if(!resource.exists()){
            return "The package selected does not exist in the filesystem.";
        }
        validatedPackage = (IContainer) resource;
        return null;
    }

    private String checkValidSourceFolder(String text) throws CoreException {
        validatedSourceFolder = null;
        if(text == null || text.trim().length() == 0 ){
            return "The source folder must be filled.";
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IResource resource = root.findMember(new Path(text));
        if(resource == null){
            return "The source folder was not found in the workspace.";
        }
        if(!(resource instanceof IContainer)){
            return "The source folder was found in the workspace but is not a container.";
        }
        IProject project = resource.getProject();
        if(project == null){
            return "Unable to find the project related to the source folder.";
        }
        IPythonPathNature nature = PythonNature.getPythonPathNature(project);
        if(nature == null){
            return "The pydev nature is not configured on the project: "+project.getName();
        }
        String full = resource.getFullPath().toString();
        String[] srcPaths = PythonNature.getStrAsStrItems(nature.getProjectSourcePath());
        for (String str : srcPaths) {
            if(str.equals(full)){
                validatedSourceFolder = (IContainer) resource;
                return null;
            }
        }
        return "The selected source folder is not recognized as a valid source folder.";
    }
}

