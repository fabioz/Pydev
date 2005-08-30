package org.python.pydev.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.osgi.framework.BundleContext;
import org.python.copiedfromeclipsesrc.PydevFileEditorInput;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.pyunit.ITestRunListener;
import org.python.pydev.pyunit.PyUnitTestRunner;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.interpreters.IInterpreterManager;
import org.python.pydev.ui.interpreters.JythonInterpreterManager;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;

/**
 * The main plugin class - initialized on startup - has resource bundle for internationalization - has preferences
 */
public class PydevPlugin extends AbstractUIPlugin implements Preferences.IPropertyChangeListener {

    private static IInterpreterManager pythonInterpreterManager;
    private static IInterpreterManager jythonInterpreterManager;
    public static void setPythonInterpreterManager(IInterpreterManager interpreterManager) {
        PydevPlugin.pythonInterpreterManager = interpreterManager;
    }
    public static IInterpreterManager getPythonInterpreterManager() {
        return pythonInterpreterManager;
    }

    public static void setJythonInterpreterManager(IInterpreterManager interpreterManager) {
        PydevPlugin.jythonInterpreterManager = interpreterManager;
    }
    public static IInterpreterManager getJythonInterpreterManager() {
        return jythonInterpreterManager;
    }
    /**
     * returns the interpreter manager for a given nature
     * @param nature the nature from where we want to get the associated interpreter manager
     * 
     * @return the interpreter manager
     */
    public static IInterpreterManager getInterpreterManager(IPythonNature nature) {
        try {
            if (nature.isJython()) {
                return jythonInterpreterManager;
            } else if (nature.isPython()) {
                return pythonInterpreterManager;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unable to get the interpreter manager for the nature passed.");
    }
    
    
    private static PydevPlugin plugin; //The shared instance.
    
    private ResourceBundle resourceBundle; //Resource bundle.

    /** The template store. */
    private TemplateStore fStore;

    /** The context type registry. */
    private ContributionContextTypeRegistry fRegistry = null;

    /** Key to store custom templates. */
    private static final String CUSTOM_TEMPLATES_PY_KEY = "org.python.pydev.editor.templates.PyTemplatePreferencesPage";

    public static final String DEFAULT_PYDEV_SCOPE = "org.python.pydev";


    /**
     * The constructor.
     */
    public PydevPlugin() {
        super();
        plugin = this;
    }

    public void start(BundleContext context) throws Exception {
        super.start(context);
        try {
            resourceBundle = ResourceBundle.getBundle("org.python.pydev.PyDevPluginResources");
        } catch (MissingResourceException x) {
            resourceBundle = null;
        }
        Preferences preferences = plugin.getPluginPreferences();
        preferences.addPropertyChangeListener(this);
        setPythonInterpreterManager(new PythonInterpreterManager(preferences));
        setJythonInterpreterManager(new JythonInterpreterManager(preferences));
        

        //restore the nature for all python projects
        new Job("PyDev: Restoring projects python nature"){

            protected IStatus run(IProgressMonitor monitor) {
                IProject[] projects = getWorkspace().getRoot().getProjects();
                for (int i = 0; i < projects.length; i++) {
                    IProject project = projects[i];
                    try {
                        if (project.isOpen() && project.hasNature(PythonNature.PYTHON_NATURE_ID)) {
                            PythonNature.addNature(project, monitor);
                        }
                    } catch (Exception e) {
                        PydevPlugin.log(e);
                    }
                }
                return Status.OK_STATUS;
            }
            
        }.schedule();
        
    }
    

    /**
     * This is called when the plugin is being stopped.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        
        try {
            //stop the running shells
            AbstractShell.shutdownAllShells();

            Preferences preferences = plugin.getPluginPreferences();
            preferences.removePropertyChangeListener(this);
            
            //save the natures (code completion stuff).
            IProject[] projects = getWorkspace().getRoot().getProjects();
            for (int i = 0; i < projects.length; i++) {
                try {
                    IProject project = projects[i];
                    if (project.isOpen()){
	                    IProjectNature n = project.getNature(PythonNature.PYTHON_NATURE_ID);
	                    if(n instanceof PythonNature){
	                        PythonNature nature = (PythonNature) n;
	                        nature.saveAstManager(true);
	                    }
                    }
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }

        } finally{
	        super.stop(context);
        }
    }

    public static PydevPlugin getDefault() {
        return plugin;
    }

    public static String getPluginID() {
        return BundleInfo.getBundleInfo().getPluginID();
    }

    /**
     * Returns the workspace instance.
     */
    public static IWorkspace getWorkspace() {
        return ResourcesPlugin.getWorkspace();
    }

    public static Status makeStatus(int errorLevel, String message, Throwable e) {
        return new Status(errorLevel, getPluginID(), errorLevel, message, e);
    }

    /**
     * Returns the string from the plugin's resource bundle, or 'key' if not found.
     */
    public static String getResourceString(String key) {
        ResourceBundle bundle = plugin.getResourceBundle();
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return key;
        }
    }

    public ResourceBundle getResourceBundle() {
        return resourceBundle;
    }
    
    
    public void propertyChange(Preferences.PropertyChangeEvent event) {
        //		System.out.println( event.getProperty()
        //		 + "\n\told setting: "
        //		 + event.getOldValue()
        //		 + "\n\tnew setting: "
        //		 + event.getNewValue());
    }

    public static void log(int errorLevel, String message, Throwable e) {
        log(errorLevel, message, e, true);
    }
    
    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     */
    public static void log(int errorLevel, String message, Throwable e, boolean printToConsole) {
        if(printToConsole){
            e.printStackTrace();
        }
        
        try {
	        Status s = new Status(errorLevel, getPluginID(), errorLevel, message, e);
	        getDefault().getLog().log(s);
        } catch (Exception e1) {
            //logging should not fail!
        }
    }

    public static void log(Throwable e) {
        log(e, true);
    }
    
    public static void log(Throwable e, boolean printToConsole) {
        log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten.", e, printToConsole);
    }

    public static CoreException log(String msg) {
        IStatus s = PydevPlugin.makeStatus(IStatus.ERROR, msg, new RuntimeException(msg));
        CoreException e = new CoreException(s);
        PydevPlugin.log(e);
        return e;
    }

    /**
     *  
     */
    public static IPath getPath(IPath location) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();

        IFile[] files = root.findFilesForLocation(location);
        IContainer[] folders =  root.findContainersForLocation(location);
        if (files.length > 0) {
            for (int i = 0; i < files.length; i++)
                return files[i].getProjectRelativePath();
        } else {
            for (int i = 0; i < folders.length; i++)
                return folders[i].getProjectRelativePath();
        }

        return null;
    }

    public static IPath getLocationFromWorkspace(IPath path) {
        return getLocationFromWorkspace(path, 0);
    }
    /**
     * This one should only be used if the root (project) is unknown.
     * 
     * @see PydevPlugin.getLocation#IPath, IContainer
     * @param path
     * @return
     */
    public static IPath getLocationFromWorkspace(IPath path, int repetitions) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IContainer root = workspace.getRoot();
        repetitions++;
        return getLocation(path, root, repetitions);
    }

    public static IPath getLocation(IPath path, IContainer root) {
        return getLocation(path, root, 0);
    }
    
    /**
     * Returns the location in the filesystem for the given path
     * 
     * @param path
     * @param root the path must be inside this root
     * @return
     */
    public static IPath getLocation(IPath path, IContainer root, int repetitions) {
        if(repetitions > 3){
            return null;
        }
        repetitions++;
        IResource resource = root.findMember(path);
        IPath location = null;
        if (resource != null) {
            location = resource.getLocation();
        }
        
        if(location == null){
            location = getLocationFromWorkspace(path, repetitions);
        }
        return location;
    }
    /**
     * Utility function that opens an editor on a given path.
     * 
     * @return part that is the editor
     */
    public static IEditorPart doOpenEditor(IPath path, boolean activate) {
        if (path == null)
            return null;

        try {
            
            IEditorInput file = createEditorInput(path);
	        
	        final IWorkbench workbench = plugin.getWorkbench();
	        if(workbench == null){
	        	throw new RuntimeException("workbench cannot be null");
	        }

	        IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
	        if(activeWorkbenchWindow == null){
	        	throw new RuntimeException("activeWorkbenchWindow cannot be null (we have to be in a ui thread for this to work)");
	        }
	        
			IWorkbenchPage wp = activeWorkbenchWindow.getActivePage();
        
            // File is inside the workspace
            return IDE.openEditor(wp, file, PyEdit.EDITOR_ID);
            
        } catch (Exception e) {
            log(IStatus.ERROR, "Unexpected error opening path " + path.toString(), e);
            return null;
        }
    }

    
    
    

//  =====================
//  ===================== ALL BELOW IS COPIED FROM org.eclipse.ui.internal.editors.text.OpenExternalFileAction
//  =====================

    /**
     * @param path
     * @return
     */
    public static IEditorInput createEditorInput(IPath path) {
        IEditorInput edInput;
        IWorkspace w = ResourcesPlugin.getWorkspace();      
        IFile file = w.getRoot().getFileForLocation(path);
        if (file == null  || !file.exists()){
            //it is probably an external file
            File file2 = path.toFile();
            edInput = createEditorInput(file2);
        }else{
            edInput = new FileEditorInput(file);
        }
        return edInput;
    }

    private static IEditorInput createEditorInput(File file) {
        IFile workspaceFile= getWorkspaceFile(file);
        if (workspaceFile != null)
            return new FileEditorInput(workspaceFile);
        return new PydevFileEditorInput(file);
    }

    private static IFile getWorkspaceFile(File file) {
        IWorkspace workspace= ResourcesPlugin.getWorkspace();
        IPath location= Path.fromOSString(file.getAbsolutePath());
        IFile[] files= workspace.getRoot().findFilesForLocation(location);
        files= filterNonExistentFiles(files);
        if (files == null || files.length == 0){
            return null;
        }
        
        if (files.length > 1){
            return files[0];
        } else {
            return null;
        }
        //we are out of the loop from the interface when this is called
//        if (files.length == 1)
//            return files[0];
//        return selectWorkspaceFile(files);
    }
    

    private static IFile[] filterNonExistentFiles(IFile[] files){
        if (files == null)
            return null;

        int length= files.length;
        ArrayList<IFile> existentFiles= new ArrayList<IFile>(length);
        for (int i= 0; i < length; i++) {
            if (files[i].exists())
                existentFiles.add(files[i]);
        }
        return (IFile[])existentFiles.toArray(new IFile[existentFiles.size()]);
    }
//    private IFile selectWorkspaceFile(IFile[] files) {
//        ElementListSelectionDialog dialog= new ElementListSelectionDialog(fWindow.getShell(), new FileLabelProvider());
//        dialog.setElements(files);
//        dialog.setTitle(TextEditorMessages.OpenExternalFileAction_title_selectWorkspaceFile);
//        dialog.setMessage(TextEditorMessages.OpenExternalFileAction_message_fileLinkedToMultiple);
//        if (dialog.open() == Window.OK)
//            return (IFile) dialog.getFirstResult();
//        return null;
//    }

    
//  =====================
//  ===================== END COPY FROM org.eclipse.ui.internal.editors.text.OpenExternalFileAction
//  =====================


    /**
	 * Returns this plug-in's template store.
	 * 
	 * @return the template store of this plug-in instance
	 */
    public TemplateStore getTemplateStore() {
        if (fStore == null) {
            fStore = new ContributionTemplateStore(getContextTypeRegistry(), getPreferenceStore(), CUSTOM_TEMPLATES_PY_KEY);
            try {
                fStore.load();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        return fStore;
    }

    /**
     * Returns this plug-in's context type registry.
     * 
     * @return the context type registry for this plug-in instance
     */
    public ContextTypeRegistry getContextTypeRegistry() {
        if (fRegistry == null) {
            // create an configure the contexts available in the template editor
            fRegistry = new ContributionContextTypeRegistry();
            fRegistry.addContextType(PyContextType.PY_CONTEXT_TYPE);
        }
        return fRegistry;
    }

    
    
    /**
     * 
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getScriptWithinPySrc(String targetExec) throws CoreException {
        IPath relative = new Path("PySrc").addTrailingSeparator().append(targetExec);
        return getRelativePath(relative);
    }

    /**
     * @param relative
     * @return
     * @throws CoreException
     */
    public static File getRelativePath(IPath relative) throws CoreException {
        return BundleInfo.getBundleInfo().getRelativePath(relative);
    }
    
    public static ImageCache getImageCache(){
        return BundleInfo.getBundleInfo().getImageCache();
    }
    

    public static File getImageWithinIcons(String icon) throws CoreException {

        IPath relative = new Path("icons").addTrailingSeparator().append(icon);

        return getRelativePath(relative);

    }

    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @return tuple with files in pos 0 and folders in pos 1
     */
    public static List<File>[] getPyFilesBelow(File file, IProgressMonitor monitor, final boolean includeDirs) {
        return getPyFilesBelow(file, monitor, true, true);
    }
    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @return tuple with files in pos 0 and folders in pos 1
     */
    public static List<File>[] getPyFilesBelow(File file, IProgressMonitor monitor, final boolean includeDirs, boolean checkHasInit) {
        FileFilter filter = new FileFilter() {
    
            public boolean accept(File pathname) {
                if (includeDirs)
                    return pathname.isDirectory() || pathname.toString().endsWith(".py");
                else
                    return pathname.isDirectory() == false && pathname.toString().endsWith(".py");
            }
    
        };
        return getPyFilesBelow(file, filter, monitor, true, checkHasInit);
    }


    public static List<File>[] getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean checkHasInit) {
        return getPyFilesBelow(file, filter, monitor, true, checkHasInit);
    }
    
    public static List<File>[] getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean addSubFolders, boolean checkHasInit) {
        return getPyFilesBelow(file, filter, monitor, addSubFolders, 0, checkHasInit);
    }
    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @param addSubFolders: indicates if sub-folders should be added
     * @return tuple with files in pos 0 and folders in pos 1
     */
    private static List<File>[] getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor, boolean addSubFolders, int level, boolean checkHasInit) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        List<File> filesToReturn = new ArrayList<File>();
        List<File> folders = new ArrayList<File>();

        if (file.exists() == true) {

            if (file.isDirectory()) {
                File[] files = null;

                if (filter != null) {
                    files = file.listFiles(filter);
                } else {
                    files = file.listFiles();
                }

                boolean hasInit = false;

                List<File> foldersLater = new LinkedList<File>();
                
                for (int i = 0; i < files.length; i++) {
                    File file2 = files[i];
                    
                    if(file2.isFile()){
	                    filesToReturn.add(file2);
	                    monitor.worked(1);
	                    monitor.setTaskName("Found:" + file2.toString());
	                    
                        if (checkHasInit){
    	                    if(file2.getName().equals("__init__.py")){
    	                        hasInit = true;
    	                    }
                        }
	                    
                    }else{
                        foldersLater.add(file2);
                    }
                }
                
                if(!checkHasInit  || hasInit || level == 0){
	                folders.add(file);

	                for (Iterator iter = foldersLater.iterator(); iter.hasNext();) {
	                    File file2 = (File) iter.next();
	                    if(file2.isDirectory() && addSubFolders){
		                    List[] below = getPyFilesBelow(file2, filter, monitor, addSubFolders, level+1, checkHasInit);
		                    filesToReturn.addAll(below[0]);
		                    folders.addAll(below[1]);
		                    monitor.worked(1);
	                    }
	                }
                }

                
            } else if (file.isFile()) {
                filesToReturn.add(file);
                
            } else{
                throw new RuntimeException("Not dir nor file... what is it?");
            }
        }
        
        return new List[] { filesToReturn, folders };

    }

    


    //PyUnit integration
    
	/** Listener list **/
    private List listeners = new ArrayList();


	public void addTestListener(ITestRunListener listener) {
		listeners.add(listener);
	}
	
	public void removeTestListener(ITestRunListener listener) {
		listeners.remove(listener);
	}

	public List getListeners() {
		return listeners;
	}
	
	public void runTests(String moduleDir, String moduleName, IProject project) throws IOException, CoreException {
		new PyUnitTestRunner().runTests(moduleDir, moduleName, project);
	}
	
	public void fireTestsStarted(int count) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testsStarted(count);
		}
	}

	public void fireTestsFinished() {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testsFinished();
		}
	}

	public void fireTestStarted(String klass, String methodName) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testStarted(klass, methodName);
		}
	}

	public void fireTestFailed(String klass, String methodName, String trace) {
		for (Iterator all=getListeners().iterator(); all.hasNext();) {
			ITestRunListener each = (ITestRunListener) all.next();
			each.testFailed(klass, methodName, trace);
		}
	}
    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    public static IPreferenceStore getChainedPrefStore() {
        IPreferenceStore general = EditorsUI.getPreferenceStore();
        IPreferenceStore preferenceStore = getDefault().getPreferenceStore();
        ChainedPreferenceStore store = new ChainedPreferenceStore(new IPreferenceStore[] { general, preferenceStore });
        return store;
    }
}