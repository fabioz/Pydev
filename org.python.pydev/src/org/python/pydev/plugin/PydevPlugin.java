package org.python.pydev.plugin;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.eclipse.core.internal.resources.ResourceException;
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
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.templates.ContributionContextTypeRegistry;
import org.eclipse.ui.editors.text.templates.ContributionTemplateStore;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.pychecker.PyCheckerPrefPage;
import org.python.pydev.builder.pylint.PyLintPrefPage;
import org.python.pydev.builder.todo.PyTodoPrefPage;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.editor.templates.PyContextType;
import org.python.pydev.pyunit.ITestRunListener;
import org.python.pydev.pyunit.PyUnitTestRunner;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.InterpreterManager;

/**
 * The main plugin class - initialized on startup - has resource bundle for internationalization - has preferences
 */
public class PydevPlugin extends AbstractUIPlugin implements Preferences.IPropertyChangeListener {

    public static IInterpreterManager interpreterManager;

    private static PydevPlugin plugin; //The shared instance.
    
    private ResourceBundle resourceBundle; //Resource bundle.

    /** The template store. */
    private TemplateStore fStore;

    /** The context type registry. */
    private ContributionContextTypeRegistry fRegistry = null;

    /** Key to store custom templates. */
    private static final String CUSTOM_TEMPLATES_PY_KEY = "org.python.pydev.editor.templates.PyTemplatePreferencesPage";


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
        interpreterManager = new InterpreterManager(preferences);
    }
    

    /**
     * This is called when the plugin is being stopped.
     * 
     * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
     */
    public void stop(BundleContext context) throws Exception {
        
        try {
            Preferences preferences = plugin.getPluginPreferences();
            preferences.removePropertyChangeListener(this);
            PythonShell.stopAllShells();
            
            IProject[] projects = getWorkspace().getRoot().getProjects();
            for (int i = 0; i < projects.length; i++) {
                try {
                    if (projects[i].isOpen()){
	                    IProjectNature n = projects[i].getNature(PythonNature.PYTHON_NATURE_ID);
	                    if(n instanceof PythonNature){
	                        PythonNature nature = (PythonNature) n;
	                    }
                    }
                } catch (CoreException e) {
                    e.printStackTrace();
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

    protected void initializeDefaultPluginPreferences() {
        PydevPrefs.initializeDefaultPreferences(getPluginPreferences());
        PyCodeCompletionPreferencesPage.initializeDefaultPreferences(getPluginPreferences());
        PyCheckerPrefPage.initializeDefaultPreferences(getPluginPreferences());
        PyLintPrefPage.initializeDefaultPreferences(getPluginPreferences());
        PyTodoPrefPage.initializeDefaultPreferences(getPluginPreferences());
        PyDevBuilderPrefPage.initializeDefaultPreferences(getPluginPreferences());
        PyCodeFormatterPage.initializeDefaultPreferences(getPluginPreferences());
    }

    public void propertyChange(Preferences.PropertyChangeEvent event) {
        //		System.out.println( event.getProperty()
        //		 + "\n\told setting: "
        //		 + event.getOldValue()
        //		 + "\n\tnew setting: "
        //		 + event.getNewValue());
    }

    /**
     * @param errorLevel IStatus.[OK|INFO|WARNING|ERROR]
     */
    public static void log(int errorLevel, String message, Throwable e) {
        e.printStackTrace();
        try {
	        Status s = new Status(errorLevel, getPluginID(), errorLevel, message, e);
	        getDefault().getLog().log(s);
        } catch (Exception e1) {
            //logging should not fail!
        }
    }

    public static void log(Throwable e) {
        log(IStatus.ERROR, e.getMessage() != null ? e.getMessage() : "No message gotten.", e);
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

    public static IPath getLocation(IPath path) {
        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        IWorkspaceRoot root = workspace.getRoot();
        IResource resource = root.findMember(path);
        IPath location = null;
        if (resource != null) {
            location = resource.getLocation();
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
	        IWorkspace w = ResourcesPlugin.getWorkspace();
	        
	        IFile file = w.getRoot().
	        	getFileForLocation(path);
	        
	        IWorkbenchPage wp = plugin.getWorkbench().
	        	getActiveWorkbenchWindow().
	        		getActivePage();
        
            if (file != null && file.exists()) {
                // File is inside the workspace
                return IDE.openEditor(wp, file, activate);
            } else {
                return openExternalFile(path, wp, activate);
            }
            
        } catch (Exception e) {
            log(IStatus.ERROR, "Unexpected error opening path " + path.toString(), e);
            return null;
        }
    }

    /**
     * @param path
     * @param wp
     * @param activate
     * @return
     * @throws CoreException
     */
    private static IEditorPart openExternalFile(IPath location, IWorkbenchPage wp, boolean activate) throws CoreException {
        IWorkspace ws = ResourcesPlugin.getWorkspace();
        IProject project = ws.getRoot().getProject("External Files");
        if (!project.exists())
            project.create(null);
        if (!project.isOpen())
            project.open(null);

        IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();

        IFile file = project.getFile(location.lastSegment());
        try {
            file.createLink(location, IResource.NONE, null);
        } catch (ResourceException e) {
            //That's OK
            //org.eclipse.core.internal.resources.ResourceException: Resource /External Files/GUITest.py already exists.
        }
        if (wp != null)
            return IDE.openEditor(wp, file, activate);
        return null;

        //        IStorage storage = new FileStorage(path);
        //        IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
        //        IEditorDescriptor desc = registry.getDefaultEditor(path.lastSegment());
        //        if (desc == null)
        //        	desc = registry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
        //        IEditorInput input = new ExternalEditorInput(storage);
        //        return wp.openEditor(input, desc.getId());
    }

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
    public static List[] getPyFilesBelow(File file, IProgressMonitor monitor, final boolean includeDirs) {
        FileFilter filter = new FileFilter() {
    
            public boolean accept(File pathname) {
                if (includeDirs)
                    return pathname.isDirectory() || pathname.toString().endsWith(".py");
                else
                    return pathname.isDirectory() == false && pathname.toString().endsWith(".py");
            }
    
        };
        return getPyFilesBelow(file, filter, monitor);
    }
    /**
     * Returns the directories and python files in a list.
     * 
     * @param file
     * @return tuple with files in pos 0 and folders in pos 1
     */
    public static List[] getPyFilesBelow(File file, FileFilter filter, IProgressMonitor monitor) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        List filesToReturn = new ArrayList();
        List folders = new ArrayList();

        if (file.exists() == true) {

            if (file.isDirectory()) {
                folders.add(file);
                File[] files = null;

                if (filter != null) {
                    files = file.listFiles(filter);
                } else {
                    files = file.listFiles();
                }

                for (int i = 0; i < files.length; i++) {
                    List[] below = getPyFilesBelow(files[i], filter, monitor);
                    filesToReturn.addAll(below[0]);
                    folders.addAll(below[1]);
                    monitor.worked(1);
                }
            } else if (file.isFile()) {
                filesToReturn.add(file);
                monitor.worked(1);
                monitor.setTaskName("Found:" + file.toString());
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

}