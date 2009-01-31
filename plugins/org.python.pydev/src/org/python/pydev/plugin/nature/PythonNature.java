/*
 * License: Common Public License v1.0
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 * @author atotic
 */
package org.python.pydev.plugin.nature;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

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
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.REF;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
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
public class PythonNature extends AbstractPythonNature implements IPythonNature {

    /**
     * Contains a list with the natures created.
     */
    private static List<WeakReference<PythonNature>> createdNatures = new ArrayList<WeakReference<PythonNature>>();
    
    /**
     * @return the natures that were created.
     */
    public static List<PythonNature> getInitializedPythonNatures(){
        ArrayList<PythonNature> ret = new ArrayList<PythonNature>();
        synchronized(createdNatures){
            for(Iterator<WeakReference<PythonNature>> it=createdNatures.iterator();it.hasNext();){
                PythonNature pythonNature = it.next().get();
                if(pythonNature == null){
                    it.remove();
                }else if(pythonNature.getProject() != null){
                    ret.add(pythonNature);
                }
            }
        }
        return ret;
    }
    
    /**
     * Constructor
     * 
     * Adds the nature to the list of created natures.
     */
    public PythonNature(){
        synchronized(createdNatures){
            createdNatures.add(new WeakReference<PythonNature>(this));
        }
    }
    
    
    
    
    /**
     * This is the job that is used to rebuild the python nature modules.
     * 
     * @author Fabio
     */
    protected class RebuildPythonNatureModules extends Job {
        private volatile String submittedPaths;

        protected RebuildPythonNatureModules() {
          super("Python Nature: rebuilding modules");
        }
    
        public synchronized void setParams(String paths) {
            submittedPaths = paths;
        }

        @SuppressWarnings("unchecked")
        protected IStatus run(IProgressMonitor monitorArg) {

            String paths;
            paths = submittedPaths;
            
            try {
                final JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(monitorArg, "Rebuilding modules", IProgressMonitor.UNKNOWN, this);
                final PythonNature nature = PythonNature.this;
                try {
                    ICodeCompletionASTManager tempAstManager = astManager;
                    if (tempAstManager == null) {
                        tempAstManager = new ASTManager();
                    }
                    synchronized(tempAstManager){
                        astManager = tempAstManager;
                        tempAstManager.setProject(getProject(), nature, false); //it is a new manager, so, remove all deltas

                        //begins task automatically
                        tempAstManager.changePythonPath(paths, project, jobProgressComunicator);
                        saveAstManager();

                        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
                        for (IInterpreterObserver observer : participants) {
                            try {
                                observer.notifyProjectPythonpathRestored(nature, jobProgressComunicator);
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
                PythonNatureListenersManager.notifyPythonPathRebuilt(project, 
                        nature.pythonPathNature.getCompleteProjectPythonPath(nature.getProjectInterpreter(), nature.getRelatedInterpreterManager())); 
                //end task
                jobProgressComunicator.done();
            }catch (Exception e) {
                Log.log(e);
            }
            return Status.OK_STATUS;
        }
    }

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
     * Used to actually store settings for the pythonpath
     */
    private IPythonNatureStore pythonNatureStore = new PythonNatureStore();
    
    
    /**
     * constant that stores the name of the python version we are using for the project with this nature
     */
    private static QualifiedName pythonProjectVersion = null;
    static QualifiedName getPythonProjectVersionQualifiedName() {
        if(pythonProjectVersion == null){
            //we need to do this because the plugin ID may not be known on 'static' time
            pythonProjectVersion = new QualifiedName(PydevPlugin.getPluginID(), "PYTHON_PROJECT_VERSION");
        }
        return pythonProjectVersion;
    }
    
    /**
     * constant that stores the name of the python version we are using for the project with this nature
     */
    private static QualifiedName pythonProjectInterpreter = null;
    static QualifiedName getPythonProjectInterpreterQualifiedName() {
        if(pythonProjectInterpreter == null){
            //we need to do this because the plugin ID may not be known on 'static' time
            pythonProjectInterpreter = new QualifiedName(PydevPlugin.getPluginID(), "PYTHON_PROJECT_INTERPRETER");
        }
        return pythonProjectInterpreter;
    }

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
    
    private static Map<IProject, Job> jobs = new HashMap<IProject, Job>();

    /**
     * Sets this nature's project - called from the eclipse platform.
     * 
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    public void setProject(final IProject project) {
        getStore().setProject(project);
        this.project = project;
        this.pythonPathNature.setProject(project, this);
        
        if(project != null && !initializationStarted && !initializationFinished){
            synchronized (jobs) {
                Job job = jobs.get(project);
                if(job != null){
                    job.cancel();
                }
                job = new Job("PyDev: Restoring projects python nature") {
                    
                    protected IStatus run(IProgressMonitor monitor) {
                        try {
                            if(monitor.isCanceled()){
                                return Status.OK_STATUS;
                            }
                            init(null, null, monitor, null);
                            synchronized (jobs) {
                                if(jobs.get(project) == this){
                                    jobs.remove(project);
                                }
                            }
                        } catch (Throwable t) {
                            PydevPlugin.log(t);
                        }
                        return Status.OK_STATUS;
                    }
                    
                };
                jobs.put(project, job);
                job.schedule(250L); //wait to see if we've more than 1 request.
            }
        }

    }

    public static synchronized IPythonNature addNature(IEditorInput element) {
        if(element instanceof FileEditorInput){
            IFile file = (IFile)((FileEditorInput)element).getAdapter(IFile.class);
            if (file != null){
                try {
                    return PythonNature.addNature(file.getProject(), null, null, null, null);
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
        }
        return null;
    }
    
    /**
     * Utility routine to remove a PythonNature from a project.
     */
    public static synchronized void removeNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        
        PythonNature nature = PythonNature.getPythonNature(project);
        if (nature == null) {
            return;
        }
        
        try {
            //we have to set the nature store to stop listening changes to .pydevproject
            nature.pythonNatureStore.setProject(null);
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        
        try {
            //we have to remove the project from the pythonpath nature too...
            nature.pythonPathNature.setProject(null, null);
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        
        //notify listeners that the pythonpath nature is now empty for this project
        try {
            PythonNatureListenersManager.notifyPythonPathRebuilt(project, new ArrayList<String>()); 
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
        
        try {
            //actually remove the pydev configurations
            IResource member = project.findMember(".pydevproject");
            if(member != null){
                member.delete(true, null);
            }
        } catch (CoreException e) {
            PydevPlugin.log(e);
        }

        //and finally... remove the nature
        
        IProjectDescription description = project.getDescription();
        List<String> natures = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
        natures.remove(PYTHON_NATURE_ID);
        description.setNatureIds(natures.toArray(new String[natures.size()]));
        project.setDescription(description, monitor);
    }

    /**
     * Utility routine to add PythonNature to the project
     * 
     * @param projectPythonpath: @see {@link IPythonPathNature#setProjectSourcePath(String)}
     */
    public static synchronized IPythonNature addNature(IProject project, IProgressMonitor monitor, String version, 
            String projectPythonpath, String projectInterpreter) throws CoreException {
        if (project == null || !project.isOpen()) {
            return null;
        }
        if(monitor == null){
            monitor = new NullProgressMonitor();
        }
        if(projectInterpreter == null){
            projectInterpreter = IPythonNature.DEFAULT_INTERPRETER;
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

        IProjectNature n = getPythonNature(project);
        if (n instanceof PythonNature) {
            PythonNature nature = (PythonNature) n;
            //call initialize always - let it do the control.
            nature.init(version, projectPythonpath, monitor, projectInterpreter);
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
     * @param projectPythonpath this is the project python path to be used (may be null)  -- if not null, this nature is being created
     * @param version this is the version (project type) to be used (may be null) -- if not null, this nature is being created
     * @param monitor 
     * @param interpreter 
     */
    @SuppressWarnings("unchecked")
    private void init(String version, String projectPythonpath, IProgressMonitor monitor, String interpreter) {
        if(version != null || projectPythonpath != null){
            this.getStore().startInit();
            try {
                if(projectPythonpath != null){
                    this.getPythonPathNature().setProjectSourcePath(projectPythonpath);
                }
                if(version != null || interpreter != null){
                    this.setVersion(version, interpreter);
                }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }finally{
                this.getStore().endInit();
            }
        }else{
            //Change: 1.3.10: it could be reloaded more than once... (when it shouldn't) 
            if(astManager != null){
                return; //already initialized...
            }
        }
        
        if(initializationStarted || monitor.isCanceled()){
            return;
        }
        
        initializationStarted = true;
        //Change: 1.3.10: no longer in a Job... should already be called in a job if that's needed.

        try {
            astManager = ASTManager.loadFromFile(getAstOutputFile());
            if (astManager != null) {
                synchronized (astManager) {
                    astManager.setProject(getProject(), this, true); // this is the project related to it, restore the deltas (we may have some crash)

                    //just a little validation so that we restore the needed info if we did not get the modules
                    if (astManager.getModulesManager().getOnlyDirectModules().length < 5) {
                        astManager = null;
                    }

                    if (astManager != null) {
                        List<IInterpreterObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
                        for (IInterpreterObserver observer : participants) {
                            try {
                                observer.notifyNatureRecreated(this, monitor);
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
                rebuildPath(null, monitor);
            } catch (Exception e) {
                PydevPlugin.log(e);
            }
        }
        initializationFinished = true;
    }


    /**
     * Returns the directory that should store completions.
     * 
     * @param p
     * @return
     */
    public static File getCompletionsCacheDir(IProject p) {
        IPath path = p.getWorkingLocation(PydevPlugin.getPluginID());
    
        File file = new File(path.toOSString());
        return file;
    }
    
    
    public File getCompletionsCacheDir() {
        return getCompletionsCacheDir(getProject());
    }

    /**
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
            clearCaches();
            String paths = this.pythonPathNature.getOnlyProjectPythonPathStr();
            this.rebuildPath(paths);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    private RebuildPythonNatureModules rebuildJob = new RebuildPythonNatureModules();
    
    /**
     * This method is called whenever the pythonpath for the project with this nature is changed. 
     */
    private synchronized void rebuildPath(final String paths) {
        rebuildJob.cancel();
        rebuildJob.setParams(paths);
        rebuildJob.schedule(20L);
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
        if(project != null && project.isOpen()){
            try {
                IProjectNature n = project.getNature(PYTHON_NATURE_ID);
                if(n instanceof PythonNature){
                    return (PythonNature) n;
                }
            } catch (CoreException e) {
                PydevPlugin.logInfo(e);
            }
        }
        return null;
    }

    /**
     * Stores the version as a cache (the actual version is set in the xml file).
     * This is so that we don't have a runtime penalty for it.
     */
    private String versionPropertyCache = null;
    private String interpreterPropertyCache = null;
    
    /**
     * Returns the Python version of the Project. 
     * 
     * It's a String in the format "python 2.4", as defined by the constants PYTHON_VERSION_XX and 
     * JYTHON_VERSION_XX in IPythonNature.
     * 
     * @note it might have changed on disk (e.g. a repository update).
     * @return the python version for the project
     * @throws CoreException 
     */
    public String getVersion() throws CoreException {
        if(project != null){
            if (versionPropertyCache == null) {
                String storeVersion = getStore().getPropertyFromXml(getPythonProjectVersionQualifiedName());
                if(storeVersion == null){ //there is no such property set (let's set it to the default)
                    setVersion(getDefaultVersion(), null); //will set the versionPropertyCache too
                }else{
                    //now, before returning and setting in the cache, let's make sure it's a valid version.
                    if(!IPythonNature.Versions.ALL_VERSIONS_ANY_FLAVOR.contains(storeVersion)){
                        Log.log("The stored version is invalid ("+storeVersion+"). Setting default.");
                        setVersion(getDefaultVersion(), null); //will set the versionPropertyCache too
                    }else{
                        //Ok, it's correct.
                        versionPropertyCache = storeVersion;   
                    }
                }
            } 
        }else{
            Log.log("Trying to get version without project set. Returning default.");
            return getDefaultVersion();
        }
        
        if(versionPropertyCache == null){
            Log.log("The cached version is null. Returning default.");
            return getDefaultVersion();
            
        }else if(!IPythonNature.Versions.ALL_VERSIONS_ANY_FLAVOR.contains(versionPropertyCache)){
            Log.log("The cached version ("+versionPropertyCache+") is invalid. Returning default.");
            return getDefaultVersion();
        }
        return versionPropertyCache;
    }
    
    /**
     * @param version: the project version given the constants PYTHON_VERSION_XX and 
     * JYTHON_VERSION_XX in IPythonNature. If null, nothing is done for the version.
     * 
     * @param interpreter the interpreter to be set if null, nothing is done to the interpreter.
     * 
     * @throws CoreException 
     */
    public void setVersion(String version, String interpreter) throws CoreException{
        clearCaches();
        
        if(version != null){
            this.versionPropertyCache = version;
        }
        
        if(interpreter != null){
            this.interpreterPropertyCache = interpreter;
        }
        
        if(project != null){
            if(version != null){
                getStore().setPropertyToXml(getPythonProjectVersionQualifiedName(), version, true);
            }
            if(interpreter != null){
                getStore().setPropertyToXml(getPythonProjectInterpreterQualifiedName(), interpreter, true);
            }
        }
    }

    public String getDefaultVersion(){
        return PYTHON_VERSION_LATEST;
    }

    
    public boolean isJython() throws CoreException {
        if(isJython == null){
            isJython = IPythonNature.Versions.ALL_JYTHON_VERSIONS.contains(getVersion());
        }
        return isJython;
    }

    public boolean isPython() throws CoreException {
        return !isJython();
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
     * Resolve the module given the absolute path of the file in the filesystem.
     * 
     * @param fileAbsolutePath the absolute file path
     * @return the module name
     */
    public String resolveModule(String fileAbsolutePath) {
        String moduleName = null;
        
        if(astManager != null){
            moduleName = astManager.getModulesManager().resolveModule(fileAbsolutePath);
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

    
    // ------------------------------------------------------------------------------------------ LOCAL CACHES
    public void clearCaches() {
        this.isJython = null;
        this.versionPropertyCache = null;
        this.interpreterPropertyCache = null;
    }
    
    Boolean isJython = null; //cache
    
    public void setBuiltinCompletions(IToken[] comps) {
        this.getRelatedInterpreterManager().setBuiltinCompletions(comps);
    }

    public IToken[] getBuiltinCompletions() {
        return this.getRelatedInterpreterManager().getBuiltinCompletions();
    }

    public IModule getBuiltinMod() {
        return this.getRelatedInterpreterManager().getBuiltinMod();
    }

    public void setBuiltinMod(IModule mod) {
        this.getRelatedInterpreterManager().setBuiltinMod(mod);
    }

    public static List<IPythonNature> getPythonNaturesRelatedTo(int relatedTo) {
        ArrayList<IPythonNature> ret = new ArrayList<IPythonNature>();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (IProject project : projects) {
            PythonNature nature = getPythonNature(project);
            try {
                if(nature != null){
                    if(nature.getRelatedId() == relatedTo){
                        ret.add(nature);
                    }
                }
            } catch (CoreException e) {
                throw new RuntimeException(e);
            }
        }
        
        return ret;
    }

    /**
     * @return the version of the grammar as defined in IPythonNature.GRAMMAR_PYTHON...
     */
    public int getGrammarVersion() {
        try {
            String version = getVersion();
            if(version == null){
                Log.log("Found null version. Returning default.");
                return LATEST_GRAMMAR_VERSION;
            }
            
            String[] splitted = StringUtils.split(version, ' ');
            if(splitted == null || splitted.length != 2){
                String storeVersion;
                try {
                    storeVersion = getStore().getPropertyFromXml(getPythonProjectVersionQualifiedName());
                } catch (Exception e) {
                    storeVersion = "Unable to get storeVersion. Reason: "+e.getMessage();
                }
                
                Log.log("Found invalid version: "+version+"\n" +
                		"Returning default\n" +
                		"Project: "+this.project+"\n" +
        				"versionPropertyCache: "+ versionPropertyCache+"\n" +
						"storeVersion:" + storeVersion);
                
                return LATEST_GRAMMAR_VERSION;
            }
            
            String grammarVersion = splitted[1];
            return getGrammarVersionFromStr(grammarVersion);

        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param grammarVersion a string in the format 2.x or 3.x
     * @return the grammar version as given in IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION
     */
    public static int getGrammarVersionFromStr(String grammarVersion){
        //Note that we don't have the grammar for all versions, so, we use the one closer to it (which is
        //fine as they're backward compatible).
        if("2.1".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_4;
            
        }else if("2.2".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_4;
            
        }else if("2.3".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_4;
            
        }else if("2.4".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_4;
            
        }else if("2.5".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_5;
            
        }else if("2.6".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_2_6;
            
        }else if("3.0".equals(grammarVersion)){
            return GRAMMAR_PYTHON_VERSION_3_0;
        }
        
        PydevPlugin.log("Unable to recognize version: "+grammarVersion+" returning default.");
        return LATEST_GRAMMAR_VERSION;
    }
    
    protected IPythonNatureStore getStore(){
        return pythonNatureStore;
    }

    public String getProjectInterpreter(){
        try {
            return getProjectInterpreter(true);
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }
    
    public String getProjectInterpreter(boolean ifDefaultChangeToActualInterpreter) throws CoreException{
        if(project != null){
            if (interpreterPropertyCache == null) {
                String storeInterpreter = getStore().getPropertyFromXml(getPythonProjectInterpreterQualifiedName());
                if(storeInterpreter == null){ //there is no such property set (let's set it to the default)
                    setVersion(null, IPythonNature.DEFAULT_INTERPRETER); //will set the interpreterPropertyCache too
                }else{
                    interpreterPropertyCache = storeInterpreter;   
                }
            } 
        }
        if(ifDefaultChangeToActualInterpreter){
            if(IPythonNature.DEFAULT_INTERPRETER.equals(interpreterPropertyCache)){
                //if it's the default, let's translate it to the outside world 
                return getRelatedInterpreterManager().getDefaultInterpreter();
            }
        }
        return interpreterPropertyCache;
    }


}


