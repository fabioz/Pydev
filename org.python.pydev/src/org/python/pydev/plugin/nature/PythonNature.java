/*
 * License: Common Public License v1.0
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 * @author atotic
 */
package org.python.pydev.plugin.nature;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.interpreters.IInterpreterObserver;
import org.python.pydev.utils.JobProgressComunicator;

/**
 * PythonNature is currently used as a marker class.
 * 
 * When python nature is present, project gets extra properties. Project gets assigned python nature when: - a python file is edited - a
 * python project wizard is created
 * 
 *  
 */
public class PythonNature implements IPythonNature {

    /**
     * This is the nature ID
     */
    public static final String PYTHON_NATURE_ID = "org.python.pydev.pythonNature";

    /**
     * This is the nature name
     */
    public static final String PYTHON_NATURE_NAME = "pythonNature";

    /**
     * Builder id for pydev (code completion todo and others)
     */
    public static final String BUILDER_ID = "org.python.pydev.PyDevBuilder";
    
    /**
     * constant that stores the name of the python version we are using for the project with this nature
     */
    private static final QualifiedName PYTHON_PROJECT_VERSION = new QualifiedName(PydevPlugin.getPluginID(), "PYTHON_PROJECT_VERSION");
    
    /**
     * Project associated with this nature.
     */
    private IProject project;

    /**
     * This is the completions cache for the nature represented by this object (it is associated with a project).
     */
    private ICodeCompletionASTManager astManager;

    /**
     * We have to know if it has already been initialized.
     */
    private boolean initializationStarted;
    
    /**
     * We have to know if it has already been initialized.
     */
    private boolean initializationFinished;

    /**
     * Manages pythonpath things
     */
    private IPythonPathNature pythonPathNature = new PythonPathNature();

    /**
     * This method is called only when the project has the nature added..
     * 
     * @see org.eclipse.core.resources.IProjectNature#configure()
     */
    public void configure() throws CoreException {
    }

    /**
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     */
    public void deconfigure() throws CoreException {
    }

    /**
     * Returns the project
     * 
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    public IProject getProject() {
        return project;
    }

    /**
     * Sets this nature's project - called from the eclipse platform.
     * 
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(IProject project) {
        this.project = project;
        this.pythonPathNature.setProject(project);
    }

    public static synchronized IPythonNature addNature(IEditorInput element) {
        if(element instanceof FileEditorInput){
			IFile file = (IFile)((FileEditorInput)element).getAdapter(IFile.class);
			if (file != null){
				try {
	                return PythonNature.addNature(file.getProject(), null);
	            } catch (CoreException e) {
	                PydevPlugin.log(e);
	            }
			}
		}
        return null;
    }

    /**
     * Utility routine to add PythonNature to the project
     * @return 
     */
    public static synchronized IPythonNature addNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (project == null) {
            return null;
        }
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }

        IProjectDescription desc = project.getDescription();

        //only add the nature if it still hasn't been added.
        if (project.hasNature(PYTHON_NATURE_ID) == false) {

            String[] natures = desc.getNatureIds();
            String[] newNatures = new String[natures.length + 1];
            System.arraycopy(natures, 0, newNatures, 0, natures.length);
            newNatures[natures.length] = PYTHON_NATURE_ID;
            desc.setNatureIds(newNatures);
            project.setDescription(desc, monitor);
        }

        //add the builder. It is used for pylint, pychecker, code completion, etc.
        ICommand[] commands = desc.getBuildSpec();

        //now, add the builder if it still hasn't been added.
        if (hasBuilder(commands) == false && PyDevBuilderPrefPage.usePydevBuilders()) {

            ICommand command = desc.newCommand();
            command.setBuilderName(BUILDER_ID);
            ICommand[] newCommands = new ICommand[commands.length + 1];

            System.arraycopy(commands, 0, newCommands, 1, commands.length);
            newCommands[0] = command;
            desc.setBuildSpec(newCommands);
            project.setDescription(desc, monitor);
        }

        IProjectNature n = project.getNature(PYTHON_NATURE_ID);
        if (n instanceof PythonNature) {
            PythonNature nature = (PythonNature) n;
            //call initialize always - let it do the control.
            nature.init();
            return nature;
        }
        return null;
    }

    /**
     * Utility to know if the pydev builder is in one of the commands passed.
     * 
     * @param commands
     */
    private static boolean hasBuilder(ICommand[] commands) {
        for (int i = 0; i < commands.length; i++) {
            if (commands[i].getBuilderName().equals(BUILDER_ID)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Initializes the python nature if it still has not been for this session.
     * 
     * Actions includes restoring the dump from the code completion cache
     */
    private void init() {
        final PythonNature nature = this;
        if (initializationStarted == false) {
            initializationStarted = true;
            
            Job codeCompletionLoadJob = new Job("Pydev code completion") {

                @SuppressWarnings("unchecked")
                protected IStatus run(IProgressMonitor monitorArg) {
                    //begins task automatically
                    JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(monitorArg, "Pydev restoring cache info...", IProgressMonitor.UNKNOWN, this);

                    try {
						astManager = (ICodeCompletionASTManager) ASTManager.loadFromFile(getAstOutputFile());
						if (astManager != null) {
							synchronized (astManager) {
								astManager.setProject(getProject(), true); // this is the project related to it, restore the deltas (we may have some crash)

								//just a little validation so that we restore the needed info if we did not get the modules
								if (astManager.getModulesManager().getOnlyDirectModules().length < 5) {
									astManager = null;
								}

								if (astManager != null) {
									List<IInterpreterObserver> participants = ExtensionHelper
											.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
									for (IInterpreterObserver observer : participants) {
										try {
											observer.notifyNatureRecreated(nature, jobProgressComunicator);
										} catch (Exception e) {
											//let's not fail because of other plugins
											PydevPlugin.log(e);
										}
									}
								}
							}
						}
					} catch (Exception e) {
						PydevPlugin.log(e);
						astManager = null;
					}
                    
                    //errors can happen when restoring it
                    if(astManager == null){
                        try {
                            rebuildPath();
                        } catch (Exception e) {
                            PydevPlugin.log(e);
                        }
                    }
                    initializationFinished = true;
                    jobProgressComunicator.done();
                    return Status.OK_STATUS;
                }
            };
            
            codeCompletionLoadJob.schedule();
                        
        }
    }


    /**
     * Returns the directory that should store completions.
     * 
     * @param p
     * @return
     */
    public static File getCompletionsCacheDir(IProject p) {
        IPath location = p.getWorkingLocation(PydevPlugin.getPluginID());
        IPath path = location;
    
        File file = new File(path.toOSString());
        return file;
    }
    
    
    public File getCompletionsCacheDir() {
        return getCompletionsCacheDir(getProject());
    }

    /**
     * @param dir: parent directory where file should be.
     * @return the file where the python path helper should be saved.
     */
    private File getAstOutputFile() {
        return new File(getCompletionsCacheDir(), "asthelper.completions");
    }

    /**
     * Can be called to refresh internal info (or after changing the path in the preferences).
     */
    public void rebuildPath() {
		this.rebuildPath(null, new NullProgressMonitor());
    }
    
    
	public void rebuildPath(String defaultSelectedInterpreter, IProgressMonitor monitor) {
		try {
			String paths = this.pythonPathNature.getOnlyProjectPythonPathStr();
			this.rebuildPath(defaultSelectedInterpreter, paths);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

    /**
     * This method is called whenever the pythonpath for the project with this nature is changed. 
     */
    private void rebuildPath(final String defaultSelectedInterpreter, final String paths) {
        final PythonNature nature = this;
        Job myJob = new Job("Pydev code completion: rebuilding modules") {

            @SuppressWarnings("unchecked")
            protected IStatus run(IProgressMonitor monitorArg) {

                JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(monitorArg, "Rebuilding modules", IProgressMonitor.UNKNOWN, this);
            	try {
            		ICodeCompletionASTManager tempAstManager = astManager;
                    if (tempAstManager == null) {
                    	tempAstManager = new ASTManager();
                    }
                    synchronized(tempAstManager){
                    	astManager = tempAstManager;
                    	tempAstManager.setProject(getProject(), false); //it is a new manager, so, remove all deltas
	
	                    //begins task automatically
                    	tempAstManager.changePythonPath(paths, project, jobProgressComunicator, defaultSelectedInterpreter);
	                    saveAstManager();
	
	                    List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
	                    for (IInterpreterObserver observer : participants) {
	                        try {
	                            observer.notifyProjectPythonpathRestored(nature, jobProgressComunicator, defaultSelectedInterpreter);
	                        } catch (Exception e) {
	                        	//let's keep it safe
	                            PydevPlugin.log(e);
	                        }
	                    }
                    }
                } catch (Throwable e) {
                    PydevPlugin.log(e);
                }

                initializationFinished = true;
                //end task
                jobProgressComunicator.done();
                return Status.OK_STATUS;
            }
        };
        myJob.schedule();
        
    }
    
    /**
     * @return Returns the completionsCache.
     */
    public ICodeCompletionASTManager getAstManager() {
        if(astManager == null){
        	//this is needed because it may not be restarted already...
        	//also, this will only happen when initializing eclipse with some editors already open
        	
        	for(int i=0; i<10 && astManager == null && !initializationFinished; i++){ //we will wait 10 seconds for it
        		try {
					Thread.sleep(1000);
        		} catch (InterruptedException e) {
                    //ignore
				} catch (Exception e) {
					e.printStackTrace();
				}
        	}
        	//next time we won't wait as long.
        	initializationFinished = true;
        }

        return astManager;
    }
    
    public void setAstManager(ICodeCompletionASTManager astManager){
        this.astManager = astManager;
    }

    public IPythonPathNature getPythonPathNature() {
        return pythonPathNature;
    }
    
    public static IPythonPathNature getPythonPathNature(IProject project) {
        PythonNature pythonNature = getPythonNature(project);
        if(pythonNature != null){
            return pythonNature.pythonPathNature;
        }
        return null;
    }

    /**
     * @return all the python natures available in the workspace 
     */
    public static List<IPythonNature> getAllPythonNatures() {
    	List<IPythonNature> natures = new ArrayList<IPythonNature>();
    	IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
    	IProject[] projects = root.getProjects();
    	for (IProject project : projects) {
			PythonNature nature = getPythonNature(project);
			if(nature != null){
				natures.add(nature);
			}
		}
    	return natures;
    }
    
    public static PythonNature getPythonNature(IResource resource) {
    	if(resource == null){
    		return null;
    	}
    	return getPythonNature(resource.getProject());
    	
    }
    /**
     * @param project the project we want to know about (if it is null, null is returned)
     * @return the python nature for a project (or null if it does not exist for the project)
     */
    public static PythonNature getPythonNature(IProject project) {
        if(project != null){
            try {
            	if(!project.isOpen()){
            		return null;
            	}
                if(project.hasNature(PYTHON_NATURE_ID)){
	                IProjectNature n = project.getNature(PYTHON_NATURE_ID);
	                if(n instanceof PythonNature){
	                    return (PythonNature) n;
	                }
                }
            } catch (CoreException e) {
                PydevPlugin.logInfo(e);
            }
        }
        return null;
    }

    /**
     * Stores the version (the actual version is as a persistent property in the project).
     * This is so that we don't have a runtime penalty for it.
     */
    private String persistentProperty = null;
    
    /**
     * @return the python version for the project
     * @throws CoreException 
     */
    public String getVersion() throws CoreException {
        if(project != null){
        	if(persistentProperty == null){
	            persistentProperty = project.getPersistentProperty(PYTHON_PROJECT_VERSION);
	            if(persistentProperty == null){ //there is no such property set (let's set it to the default
	                String defaultVersion = getDefaultVersion();
	                setVersion(defaultVersion);
	                persistentProperty = defaultVersion;
	            }
        	}
            return persistentProperty;
        }
        return null;
    }
    /**
     * set the project version given the constants provided
     * @throws CoreException 
     */
    public void setVersion(String version) throws CoreException{
        if(project != null){
        	this.persistentProperty = version;
            project.setPersistentProperty(PYTHON_PROJECT_VERSION, version);
        }
    }

    public String getDefaultVersion(){
        return PYTHON_VERSION_2_4;
    }

    public boolean isJython() throws CoreException {
        return getVersion().equals(JYTHON_VERSION_2_1);
    }

    public boolean isPython() throws CoreException {
        return !isJython();
    }
    
    public boolean acceptsDecorators() throws CoreException {
        return getVersion().equals(PYTHON_VERSION_2_4);
    }
    
    public void saveAstManager() {
    	if(astManager == null){
    		REF.writeToFile(null, getAstOutputFile());
    		
    	}else{
	    	synchronized(astManager){
	    		REF.writeToFile(astManager, getAstOutputFile());
	    	}
    	}
    }

    public int getRelatedId() throws CoreException {
    	return getRelatedId(this);
    }
    
    public static int getRelatedId(IPythonNature nature) throws CoreException {
        if(nature.isPython()){
            return PYTHON_RELATED;
        }else if(nature.isJython()){
            return JYTHON_RELATED;
        }
        throw new RuntimeException("Unable to get the id to which this nature is related");
    }

    /**
     * @param resource the resource we want to get the name from
     * @return the name of the module in the environment
     */
    public static String getModuleNameForResource(IResource resource) {
        String moduleName = null;
        PythonNature nature = getPythonNature(resource.getProject());
        
        if(nature != null){
            String file = PydevPlugin.getIResourceOSString(resource);
            ICodeCompletionASTManager astManager = nature.getAstManager();
            
            if(astManager != null){
                moduleName = astManager.getModulesManager().resolveModule(file);
            }
        }
        return moduleName;
    }
    
    /**
     * @param resource the resource we want info on
     * @return whether the passed resource is in the pythonpath or not (it must be in a source folder for that).
     */
    public static boolean isResourceInPythonpath(IResource resource) {
    	return getModuleNameForResource(resource) != null; 
    }
    
    public String resolveModule(File file) {
    	String moduleName = null;
    	
    	if(astManager != null){
    		moduleName = astManager.getModulesManager().resolveModule(REF.getFileAbsolutePath(file));
    	}
    	return moduleName;
    }

    public static String[] getStrAsStrItems(String str){
        return str.split("\\|");
    }

    public IInterpreterManager getRelatedInterpreterManager() {
        try {
            if (isPython()) {
                return PydevPlugin.getPythonInterpreterManager();
            } else if (isJython()) {
                return PydevPlugin.getJythonInterpreterManager();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new RuntimeException("Unable to find the related interpreter manager.");
        
    }

}


