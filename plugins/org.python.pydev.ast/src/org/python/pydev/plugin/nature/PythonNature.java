/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectInfo;
import org.eclipse.core.resources.ICommand;
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
import org.python.pydev.ast.codecompletion.revisited.ASTManager;
import org.python.pydev.ast.codecompletion.revisited.ModulesManager;
import org.python.pydev.ast.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.ast.interpreter_managers.InterpreterManagersAPI;
import org.python.pydev.ast.runners.SimpleRunner;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModuleRequestState;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ProjectMisconfiguredException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.interpreters.IInterpreterObserver;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.nature.AbstractPythonNature;
import org.python.pydev.parser.preferences.PyDevBuilderPreferences;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.global_feedback.GlobalFeedback;
import org.python.pydev.shared_core.global_feedback.GlobalFeedback.GlobalFeedbackReporter;
import org.python.pydev.shared_core.progress.JobProgressComunicator;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.PlatformUtils;

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
    private final static List<WeakReference<PythonNature>> createdNatures = new ArrayList<WeakReference<PythonNature>>();

    /**
     * @return the natures that were created.
     */
    public static List<PythonNature> getInitializedPythonNatures() {
        ArrayList<PythonNature> ret = new ArrayList<PythonNature>();
        synchronized (createdNatures) {
            for (Iterator<WeakReference<PythonNature>> it = createdNatures.iterator(); it.hasNext();) {
                PythonNature pythonNature = it.next().get();
                if (pythonNature == null) {
                    it.remove();
                } else if (pythonNature.getProject() != null) {
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
    public PythonNature() {
        synchronized (createdNatures) {
            createdNatures.add(new WeakReference<PythonNature>(this));
        }
    }

    private final Object initLock = new Object();

    /**
     * This is the job that is used to rebuild the python nature modules.
     *
     * @author Fabio
     */
    protected class RebuildPythonNatureModules extends Job {

        protected RebuildPythonNatureModules() {
            super("Python Nature: rebuilding modules");
        }

        @Override
        @SuppressWarnings("unchecked")
        protected IStatus run(IProgressMonitor monitor) {
            String paths;
            try {
                paths = pythonPathNature.getOnlyProjectPythonPathStr(true);
            } catch (CoreException e1) {
                Log.log(e1);
                return Status.OK_STATUS;
            }
            try (GlobalFeedbackReporter r = GlobalFeedback.start("Indexing modules...")) {
                try {
                    if (monitor.isCanceled()) {
                        return Status.OK_STATUS;
                    }
                    final JobProgressComunicator jobProgressComunicator = new JobProgressComunicator(monitor,
                            "Rebuilding modules", IProgressMonitor.UNKNOWN, this) {
                        private long lastReport = 0;

                        @Override
                        public void setTaskName(String name) {
                            super.setTaskName(name);
                            reportToListeners(name);
                        }

                        @Override
                        public void subTask(String name) {
                            super.subTask(name);
                            reportToListeners(name);
                        }

                        @Override
                        public void beginTask(String name, int totalWork) {
                            super.beginTask(name, totalWork);
                            reportToListeners(name);
                        }

                        private void reportToListeners(String name) {
                            r.progress(name);
                        }
                    };
                    final PythonNature nature = PythonNature.this;
                    try {
                        ICodeCompletionASTManager tempAstManager = astManager;
                        if (tempAstManager == null) {
                            tempAstManager = new ASTManager();
                        }
                        if (monitor.isCanceled()) {
                            return Status.OK_STATUS;
                        }
                        synchronized (tempAstManager.getLock()) {
                            astManager = tempAstManager;
                            tempAstManager.setProject(getProject(), nature, false); //it is a new manager, so, remove all deltas

                            //begins task automatically
                            tempAstManager.changePythonPath(paths, project, jobProgressComunicator);
                            if (monitor.isCanceled()) {
                                return Status.OK_STATUS;
                            }
                            saveAstManager();

                            List<IInterpreterObserver> participants = ExtensionHelper
                                    .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
                            for (IInterpreterObserver observer : participants) {
                                if (monitor.isCanceled()) {
                                    return Status.OK_STATUS;
                                }
                                try {
                                    observer.notifyProjectPythonpathRestored(nature, jobProgressComunicator);
                                } catch (Exception e) {
                                    //let's keep it safe
                                    Log.log(e);
                                }
                            }
                        }
                    } catch (Throwable e) {
                        Log.log(e);
                    }

                    if (monitor.isCanceled()) {
                        return Status.OK_STATUS;
                    }
                    PythonNatureListenersManager.notifyPythonPathRebuilt(project, nature);
                    //end task
                    jobProgressComunicator.done();
                } catch (Exception e) {
                    Log.log(e);
                }
            } catch (Exception e) {
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
     * Nature ID for projects that are django configured (it's here because the icon managing
     * needs this information).
     */
    public static final String DJANGO_NATURE_ID = "org.python.pydev.django.djangoNature";

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
    private boolean initialized;

    /**
     * Manages pythonpath things
     */
    private final IPythonPathNature pythonPathNature = new PythonPathNature();

    /**
     * Used to actually store settings for the pythonpath
     */
    private final IPythonNatureStore pythonNatureStore = new PythonNatureStore();

    /**
     * constant that stores the name of the python version we are using for the project with this nature
     */
    private static QualifiedName pythonProjectVersion = null;

    static QualifiedName getPythonProjectVersionQualifiedName() {
        if (pythonProjectVersion == null) {
            //we need to do this because the plugin ID may not be known on 'static' time
            pythonProjectVersion = new QualifiedName(SharedCorePlugin.PYDEV_PLUGIN_ID, "PYTHON_PROJECT_VERSION");
        }
        return pythonProjectVersion;
    }

    /**
     * constant that stores the name of the python version we are using for the project with this nature
     */
    private static QualifiedName pythonProjectInterpreter = null;

    static QualifiedName getPythonProjectInterpreterQualifiedName() {
        if (pythonProjectInterpreter == null) {
            //we need to do this because the plugin ID may not be known on 'static' time
            pythonProjectInterpreter = new QualifiedName(SharedCorePlugin.PYDEV_PLUGIN_ID,
                    "PYTHON_PROJECT_INTERPRETER");
        }
        return pythonProjectInterpreter;
    }

    /**
     * constant that stores the additional syntax validations we should do
     */
    private static QualifiedName pythonAdditionalGrammarValidation = null;

    static QualifiedName getPythonAdditionalGrammarValidationQualifiedName() {
        if (pythonAdditionalGrammarValidation == null) {
            //we need to do this because the plugin ID may not be known on 'static' time
            pythonAdditionalGrammarValidation = new QualifiedName(SharedCorePlugin.PYDEV_PLUGIN_ID,
                    "PYTHON_ADDITIONAL_GRAMMAR_VALIDATION");
        }
        return pythonAdditionalGrammarValidation;
    }

    @Override
    public boolean isResourceInPythonpathProjectSources(IResource resource, boolean addExternal)
            throws MisconfigurationException, CoreException {
        String resourceOSString = SharedCorePlugin.getIResourceOSString(resource);
        if (resourceOSString == null) {
            return false;
        }
        return isResourceInPythonpathProjectSources(resourceOSString, addExternal);

    }

    @Override
    public boolean isResourceInPythonpathProjectSources(String absPath, boolean addExternal)
            throws MisconfigurationException, CoreException {
        return resolveModuleOnlyInProjectSources(absPath, addExternal) != null;
    }

    @Override
    public String resolveModuleOnlyInProjectSources(IResource fileAbsolutePath, boolean addExternal)
            throws CoreException, MisconfigurationException {

        String resourceOSString = SharedCorePlugin.getIResourceOSString(fileAbsolutePath);
        if (resourceOSString == null) {
            return null;
        }
        return resolveModuleOnlyInProjectSources(resourceOSString, addExternal);
    }

    /**
     * This method is called only when the project has the nature added..
     *
     * @see org.eclipse.core.resources.IProjectNature#configure()
     */
    @Override
    public void configure() throws CoreException {
    }

    /**
     * @see org.eclipse.core.resources.IProjectNature#deconfigure()
     */
    @Override
    public void deconfigure() throws CoreException {
    }

    /**
     * Returns the project
     *
     * @see org.eclipse.core.resources.IProjectNature#getProject()
     */
    @Override
    public IProject getProject() {
        return project;
    }

    /**
     * Sets this nature's project - called from the eclipse platform.
     *
     * @see org.eclipse.core.resources.IProjectNature#setProject(org.eclipse.core.resources.IProject)
     */
    @Override
    public synchronized void setProject(final IProject project) {
        getStore().setProject(project);
        this.project = project;
        this.pythonPathNature.setProject(project, this);

        if (project != null) {
            //call initialize always - let it do the control.
            init(null, null, null, new NullProgressMonitor(), null, null);
        } else {
            this.clearCaches(false);
        }

    }

    /**
     * Utility routine to remove a PythonNature from a project.
     */
    public static synchronized void removeNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (monitor == null) {
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
            Log.log(e);
        }

        try {
            //we have to remove the project from the pythonpath nature too...
            nature.pythonPathNature.setProject(null, null);
        } catch (Exception e) {
            Log.log(e);
        }

        //notify listeners that the pythonpath nature is now empty for this project
        try {
            PythonNatureListenersManager.notifyPythonPathRebuilt(project, null);
        } catch (Exception e) {
            Log.log(e);
        }

        try {
            //actually remove the pydev configurations
            IResource member = project.findMember(".pydevproject");
            if (member != null) {
                member.delete(true, null);
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        //and finally... remove the nature

        IProjectDescription description = project.getDescription();
        List<String> natures = new ArrayList<String>(Arrays.asList(description.getNatureIds()));
        natures.remove(PYTHON_NATURE_ID);
        description.setNatureIds(natures.toArray(new String[natures.size()]));
        project.setDescription(description, monitor);
    }

    /**
     * Lock to access the map below.
     */
    private final static Object mapLock = new Object();

    /**
     * If some project has a value here, we're already in the process of adding a nature to it.
     */
    private final static Map<IProject, Object> mapLockAddNature = new HashMap<IProject, Object>();

    /**
     * Utility routine to add PythonNature to the project
     *
     * @param projectPythonpath: @see {@link IPythonPathNature#setProjectSourcePath(String)}
     */
    public static IPythonNature addNature(
            //Only synchronized internally!
            IProject project, IProgressMonitor monitor, String version, String projectPythonpath,
            String externalProjectPythonpath, String projectInterpreter, Map<String, String> variableSubstitution)
            throws CoreException {

        if (project == null || !project.isOpen()) {
            return null;
        }

        if (project.hasNature(PYTHON_NATURE_ID)) {
            //Return if it already has the nature configured.
            return getPythonNature(project);
        }
        boolean alreadyLocked = false;
        synchronized (mapLock) {
            if (mapLockAddNature.get(project) == null) {
                mapLockAddNature.put(project, new Object());
            } else {
                alreadyLocked = true;
            }
        }
        if (alreadyLocked) {
            //Ok, there's some execution path already adding the nature. Let's simply wait a bit here and return
            //the nature that's there (this way we avoid any possible deadlock) -- in the worse case, null
            //will be returned here, but this is a part of the protocol anyways.
            //Done because of: Deadlock acquiring PythonNature -- at setDescription()
            //https://sourceforge.net/tracker/?func=detail&aid=3478567&group_id=85796&atid=577329
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                //ignore
            }
            return getPythonNature(project);
        } else {
            IProjectDescription desc = project.getDescription();
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            if (projectInterpreter == null) {
                projectInterpreter = IPythonNature.DEFAULT_INTERPRETER;
            }
            try {
                //Lock only for the project and add the nature (at this point we know it hasn't been added).
                String[] natures = desc.getNatureIds();
                String[] newNatures = new String[natures.length + 1];
                System.arraycopy(natures, 0, newNatures, 0, natures.length);
                newNatures[natures.length] = PYTHON_NATURE_ID;
                desc.setNatureIds(newNatures);

                //add the builder. It is used for pylint, pychecker, code completion, etc.
                ICommand[] commands = desc.getBuildSpec();

                //now, add the builder if it still hasn't been added.
                if (hasBuilder(commands) == false && PyDevBuilderPreferences.usePydevBuilders()) {

                    ICommand command = desc.newCommand();
                    command.setBuilderName(BUILDER_ID);
                    ICommand[] newCommands = new ICommand[commands.length + 1];

                    System.arraycopy(commands, 0, newCommands, 1, commands.length);
                    newCommands[0] = command;
                    desc.setBuildSpec(newCommands);
                }
                project.setDescription(desc, monitor);

                IProjectNature n = getPythonNature(project);
                if (n instanceof PythonNature) {
                    PythonNature nature = (PythonNature) n;
                    //call initialize always - let it do the control.
                    nature.init(version, projectPythonpath, externalProjectPythonpath, monitor, projectInterpreter,
                            variableSubstitution);
                    return nature;
                }
            } finally {
                synchronized (mapLock) {
                    mapLockAddNature.remove(project);
                }
            }
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
    private void init(String version, String projectPythonpath, String externalProjectPythonpath,
            IProgressMonitor monitor, String interpreter, Map<String, String> variableSubstitution) {

        //if some information is passed, restore it (even if it was already initialized)
        boolean updatePaths = version != null || projectPythonpath != null || externalProjectPythonpath != null
                || variableSubstitution != null || interpreter != null;

        if (updatePaths) {
            this.getStore().startInit();
            try {
                if (variableSubstitution != null) {
                    this.getPythonPathNature().setVariableSubstitution(variableSubstitution);
                }
                if (projectPythonpath != null) {
                    this.getPythonPathNature().setProjectSourcePath(projectPythonpath);
                }
                if (externalProjectPythonpath != null) {
                    this.getPythonPathNature().setProjectExternalSourcePath(externalProjectPythonpath);
                }
                if (version != null || interpreter != null) {
                    this.setVersion(version, interpreter);
                }
            } catch (CoreException e) {
                Log.log(e);
            } finally {
                this.getStore().endInit();
            }
        } else {
            //Change: 1.3.10: it could be reloaded more than once... (when it shouldn't)
            if (astManager != null) {
                return; //already initialized...
            }
        }

        synchronized (initLock) {
            if (initialized && !updatePaths) {
                return;
            }
            initialized = true;
        }

        if (updatePaths) {
            //If updating the paths, rebuild and return (don't try to load an existing ast manager
            //and restore anything already there)
            rebuildPath();
            return;
        }

        if (monitor.isCanceled()) {
            checkPythonPathHelperPathsJob.schedule(500);
            return;
        }

        //Change: 1.3.10: no longer in a Job... should already be called in a job if that's needed.

        try {
            File astOutputFile = getAstOutputFile();
            if (astOutputFile == null) {
                Log.log(IStatus.INFO, "Not saving ast manager for: " + this.project + ". No write area available.",
                        null);
                return; //The project was deleted
            }
            astManager = ASTManager.loadFromFile(astOutputFile);
            if (astManager != null) {
                synchronized (astManager.getLock()) {
                    astManager.setProject(getProject(), this, true); // this is the project related to it, restore the deltas (we may have some crash)

                    //just a little validation so that we restore the needed info if we did not get the modules
                    if (astManager.getModulesManager().getOnlyDirectModules().length < 15) {
                        astManager = null;
                    }

                    if (astManager != null) {
                        List<IInterpreterObserver> participants = ExtensionHelper
                                .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
                        for (IInterpreterObserver observer : participants) {
                            try {
                                observer.notifyNatureRecreated(this, monitor);
                            } catch (Exception e) {
                                //let's not fail because of other plugins
                                Log.log(e);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            //Log.logInfo("Info: Rebuilding internal caches for: "+this.project, e);
            astManager = null;
        }

        //errors can happen when restoring it
        if (astManager == null) {
            try {
                rebuildPath();
            } catch (Exception e) {
                Log.log(e);
            }
        } else {
            checkPythonPathHelperPathsJob.schedule(500);
        }
    }

    private final Job checkPythonPathHelperPathsJob = new Job("Check restored pythonpath") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            try {
                if (astManager != null) {
                    String pythonpath = pythonPathNature.getOnlyProjectPythonPathStr(true);
                    PythonPathHelper pythonPathHelper = (PythonPathHelper) astManager.getModulesManager()
                            .getPythonPathHelper();
                    //If it doesn't match, rebuid the pythonpath!
                    if (!new HashSet<String>(PythonPathHelper.parsePythonPathFromStr(pythonpath, null))
                            .equals(new HashSet<String>(pythonPathHelper.getPythonpath()))) {
                        rebuildPath();
                    }
                }
            } catch (CoreException e) {
                Log.log(e);
            }
            return Status.OK_STATUS;
        }

    };

    /**
     * Returns the directory that should store completions.
     *
     * @param p
     * @return
     */
    private File getCompletionsCacheDir(IProject p) {
        IPath path = p.getWorkingLocation(SharedCorePlugin.PYDEV_PLUGIN_ID);

        if (path == null) {
            //this can happen if the project was removed.
            return null;
        }
        File file = new File(path.toOSString());
        return file;
    }

    @Override
    public File getCompletionsCacheDir() {
        return getCompletionsCacheDir(getProject());
    }

    /**
     * @return the file where the python path helper should be saved.
     */
    private File getAstOutputFile() {
        File completionsCacheDir = getCompletionsCacheDir();
        if (completionsCacheDir == null) {
            return null;
        }
        return new File(completionsCacheDir, "v1_astmanager");
    }

    /**
     * Can be called to refresh internal info (or after changing the path in the preferences).
     * @throws CoreException
     */
    @Override
    public void rebuildPath() {
        clearCaches(true);
        //Note: pythonPathNature.getOnlyProjectPythonPathStr(true); cannot be called at this moment
        //as it may trigger a refresh, which may trigger a build and could ask for PythonNature.getPythonNature (which
        //could be the method that ended up calling rebuildPath in the first place, so, it'd deadlock).
        this.rebuildJob.cancel();
        this.rebuildJob.schedule(20L);
    }

    private RebuildPythonNatureModules rebuildJob = new RebuildPythonNatureModules();

    /**
     * @return Returns the completionsCache. Note that it can be null.
     */
    @Override
    public ICodeCompletionASTManager getAstManager() {
        return astManager; //Change: don't wait if it's still not initialized.
    }

    @Override
    public boolean isOkToUse() {
        return this.astManager != null && this.pythonPathNature != null;
    }

    public void setAstManager(ICodeCompletionASTManager astManager) {
        this.astManager = astManager;
    }

    @Override
    public IPythonPathNature getPythonPathNature() {
        return pythonPathNature;
    }

    public static IPythonPathNature getPythonPathNature(IProject project) {
        PythonNature pythonNature = getPythonNature(project);
        if (pythonNature != null) {
            return pythonNature.pythonPathNature;
        }
        return null;
    }

    /**
     * @return all the python natures available in the workspace (for opened and existing projects)
     */
    public static List<IPythonNature> getAllPythonNatures() {
        List<IPythonNature> natures = new ArrayList<IPythonNature>();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (IProject project : projects) {
            PythonNature nature = getPythonNature(project);
            if (nature != null) {
                natures.add(nature);
            }
        }
        return natures;
    }

    public static PythonNature getPythonNature(IResource resource) {
        if (resource == null) {
            return null;
        }
        return getPythonNature(resource.getProject());
    }

    private static final Object lockGetNature = new Object();

    /**
     * @param project the project we want to know about (if it is null, null is returned)
     * @return the python nature for a project (or null if it does not exist for the project)
     *
     * @note: it's synchronized because more than 1 place could call getPythonNature at the same time and more
     * than one nature ended up being created from project.getNature().
     */
    public static PythonNature getPythonNature(IProject project) {
        if (project != null && project.isOpen()) {
            try {
                //Speedup: as this method is called a lot, we just check if the nature is available internally without
                //any locks, and just lock if it's not (which is needed to avoid a racing condition creating more
                //than 1 nature).
                try {
                    if (project instanceof Project) {
                        Project p = (Project) project;
                        ProjectInfo info = (ProjectInfo) p.getResourceInfo(false, false);
                        IProjectNature nature = info.getNature(PYTHON_NATURE_ID);
                        if (nature instanceof PythonNature) {
                            return (PythonNature) nature;
                        }
                    }
                } catch (Throwable e) {
                    //Shouldn't really happen, but as using internal methods of project, who knows if it may change
                    //from one version to another.
                    Log.log(e);
                }

                synchronized (lockGetNature) {
                    IProjectNature n = project.getNature(PYTHON_NATURE_ID);
                    if (n instanceof PythonNature) {
                        return (PythonNature) n;
                    }
                }
            } catch (CoreException e) {
                Log.logInfo(e);
            }
        }
        return null;
    }

    /**
     * Stores the version as a cache (the actual version is set in the xml file).
     * This is so that we don't have a runtime penalty for it.
     */
    private String versionPropertyCache = null;
    private AdditionalGrammarVersionsToCheck additionalGrammarValidationCache = null;
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
    @Override
    public String getVersion(boolean translateIfInterpreter) throws CoreException {
        return getVersionAndError(translateIfInterpreter).o1;
    }

    @Override
    public Tuple<String, String> getVersionAndError(boolean translateIfInterpreter) throws CoreException {
        if (project != null) {
            if (versionPropertyCache == null) {
                String storeVersion = getStore().getPropertyFromXml(getPythonProjectVersionQualifiedName());
                if (storeVersion == null) { //there is no such property set (let's set it to the default)
                    setVersion(getDefaultVersion(false), null); //will set the versionPropertyCache too
                } else {
                    //now, before returning and setting in the cache, let's make sure it's a valid version.
                    if (!IPythonNature.Versions.ALL_VERSIONS_ANY_FLAVOR.contains(storeVersion)) {
                        Log.log("The stored version is invalid (" + storeVersion + "). Setting default.");
                        setVersion(getDefaultVersion(false), null); //will set the versionPropertyCache too
                    } else {
                        //Ok, it's correct.
                        versionPropertyCache = storeVersion;
                    }
                }
            }
        } else {
            String msg = "Trying to get version without project set. Returning default.";
            Log.log(msg);
            return new Tuple<String, String>(getDefaultVersion(translateIfInterpreter), msg);
        }

        if (versionPropertyCache == null) {
            String msg = "The cached version is null. Returning default.";
            Log.log(msg);
            return new Tuple<String, String>(getDefaultVersion(translateIfInterpreter), msg);

        } else if (!IPythonNature.Versions.ALL_VERSIONS_ANY_FLAVOR.contains(versionPropertyCache)) {
            String msg = "The cached version (" + versionPropertyCache + ") is invalid. Returning default.";
            Log.log(msg);
            return new Tuple<String, String>(getDefaultVersion(translateIfInterpreter), msg);
        }
        if (translateIfInterpreter && versionPropertyCache.endsWith(IPythonNature.Versions.INTERPRETER_VERSION)) {
            Tuple<String, String> split = StringUtils.splitOnFirst(versionPropertyCache, ' ');
            String errorMessage;
            try {
                IInterpreterInfo info = this.getProjectInterpreter();
                String version = info.getVersion();
                if (version != null) {
                    return new Tuple<String, String>(IPythonNature.Versions
                            .convertToInternalVersion(new FastStringBuffer(split.o1, 6).append(' '), version),
                            null);
                } else {
                    errorMessage = "Unable to get version from interpreter info: " + info.getNameForUI() + " - "
                            + info.getExecutableOrJar();
                    Log.log(errorMessage);
                }
            } catch (MisconfigurationException | PythonNatureWithoutProjectException e) {
                Log.log(e);
                errorMessage = e.getMessage();
            }
            return new Tuple<String, String>(split.o1 + " " + "2.7",
                    errorMessage + " (in project: " + getProject() + ")");
        }
        return new Tuple<String, String>(versionPropertyCache, null);
    }

    /**
     * @param version: the project version given the constants PYTHON_VERSION_XX and
     * JYTHON_VERSION_XX in IPythonNature. If null, nothing is done for the version.
     *
     * @param interpreter the interpreter to be set if null, nothing is done to the interpreter.
     *
     * @throws CoreException
     */
    @Override
    public void setVersion(String version, String interpreter) throws CoreException {
        clearCaches(false);

        if (version != null) {
            this.versionPropertyCache = version;
        }

        if (interpreter != null) {
            this.interpreterPropertyCache = interpreter;
        }

        if (project != null) {
            boolean notify = false;
            if (version != null) {
                IPythonNatureStore store = getStore();
                QualifiedName pythonProjectVersionQualifiedName = getPythonProjectVersionQualifiedName();
                String current = store.getPropertyFromXml(pythonProjectVersionQualifiedName);

                if (current == null || !current.equals(version)) {
                    store.setPropertyToXml(pythonProjectVersionQualifiedName, version, true);
                    notify = true;
                }
            }
            if (interpreter != null) {
                IPythonNatureStore store = getStore();
                QualifiedName pythonProjectInterpreterQualifiedName = getPythonProjectInterpreterQualifiedName();
                String current = store.getPropertyFromXml(pythonProjectInterpreterQualifiedName);

                if (current == null || !current.equals(interpreter)) {
                    store.setPropertyToXml(pythonProjectInterpreterQualifiedName, interpreter, true);
                    notify = true;
                }
            }
            if (notify) {
                PythonNatureListenersManager.notifyPythonPathRebuilt(project, this);
            }
        }
    }

    private Tuple<String, AdditionalGrammarVersionsToCheck> validateAdditionalGrammars(
            String additionalGrammarValidation) {
        AdditionalGrammarVersionsToCheck additionalValidations = new AdditionalGrammarVersionsToCheck();
        if (additionalGrammarValidation != null) {
            FastStringBuffer buf = new FastStringBuffer(additionalGrammarValidation.length());
            for (String version : StringUtils.split(additionalGrammarValidation, ',')) {
                version = version.trim();
                if (!Versions.supportsVersion(version)) {
                    Log.log("Grammar version not handled: " + version + " project: " + this.project);
                    continue;
                }
                if (buf.length() > 0) {
                    buf.append(", ");
                }
                additionalValidations.add(Versions.getInternalVersion(version));
                buf.append(version);
            }

            additionalGrammarValidation = buf.toString();
            if (additionalGrammarValidation.length() == 0) {
                additionalGrammarValidation = null;
            }
        }
        return new Tuple<String, AdditionalGrammarVersionsToCheck>(additionalGrammarValidation, additionalValidations);
    }

    public void setAdditionalGrammarValidation(String additionalGrammarValidation) throws CoreException {
        Tuple<String, AdditionalGrammarVersionsToCheck> tup = validateAdditionalGrammars(additionalGrammarValidation);
        additionalGrammarValidation = tup.o1;
        this.additionalGrammarValidationCache = tup.o2;
        IPythonNatureStore store = getStore();
        QualifiedName additionalGrammarValidationQualifiedName = getPythonAdditionalGrammarValidationQualifiedName();
        String current = store.getPropertyFromXml(additionalGrammarValidationQualifiedName);
        if (current != additionalGrammarValidation) {
            if (current == null || !current.equals(additionalGrammarValidation)) {
                store.setPropertyToXml(additionalGrammarValidationQualifiedName, additionalGrammarValidation, true);
            }
        }
    }

    @Override
    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException {
        if (project != null) {
            if (additionalGrammarValidationCache == null) {
                String storeVersion = getStore()
                        .getPropertyFromXml(getPythonAdditionalGrammarValidationQualifiedName());
                Tuple<String, AdditionalGrammarVersionsToCheck> validateAdditionalGrammars = validateAdditionalGrammars(
                        storeVersion);
                additionalGrammarValidationCache = validateAdditionalGrammars.o2;
            }
            return additionalGrammarValidationCache;
        } else {
            Log.log("Trying to get additional grammar version without project set. Returning default.");
            return new AdditionalGrammarVersionsToCheck();
        }
    }

    private String getDefaultVersion(boolean translateIfInterpreter) {
        // Note: can't get type (it'd recurse).
        if (translateIfInterpreter) {
            return IPythonNature.Versions.PYTHON_VERSION_LATEST;

        } else {
            return IPythonNature.PYTHON_VERSION_INTERPRETER;

        }
    }

    @Override
    public void saveAstManager() {
        File astOutputFile = getAstOutputFile();
        if (astOutputFile == null) {
            //The project was removed. Nothing to save here.
            Log.log(IStatus.INFO, "Not saving ast manager for: " + this.project + ". No write area available.", null);
            return;
        }

        if (astManager == null) {
            return;

        } else {
            synchronized (astManager.getLock()) {
                astManager.saveToFile(astOutputFile);
            }
        }
    }

    @Override
    public int getInterpreterType() throws CoreException {
        if (interpreterType == null) {
            String version = getVersion(false);
            interpreterType = getInterpreterTypeFromVersion(version);
        }

        return interpreterType;

    }

    public static int getInterpreterTypeFromVersion(String version) throws CoreException {
        if (version.startsWith(IPythonNature.Versions.PYTHON_PREFIX)) {
            return INTERPRETER_TYPE_PYTHON;
        }

        if (version.startsWith(IPythonNature.Versions.JYTHON_PREFIX)) {
            return INTERPRETER_TYPE_JYTHON;
        }

        if (version.startsWith(IPythonNature.Versions.IRONYTHON_PREFIX)) {
            return INTERPRETER_TYPE_IRONPYTHON;
        }
        //if others fail, consider it python
        return INTERPRETER_TYPE_PYTHON;
    }

    /**
     * Resolve the module given the absolute path of the file in the filesystem.
     *
     * @param fileAbsolutePath the absolute file path
     * @return the module name
     */
    @Override
    public String resolveModule(String fileAbsolutePath) {
        String moduleName = null;

        if (astManager != null) {
            moduleName = astManager.getModulesManager().resolveModule(fileAbsolutePath);
        }
        return moduleName;
    }

    /**
     * Resolve the module given the absolute path of the file in the filesystem.
     *
     * @param fileAbsolutePath the absolute file path
     * @return the module name
     * @throws CoreException
     */
    @Override
    public String resolveModuleOnlyInProjectSources(String fileAbsolutePath, boolean addExternal) throws CoreException {
        String moduleName = null;

        if (astManager != null) {
            IModulesManager modulesManager = astManager.getModulesManager();
            if (modulesManager instanceof ProjectModulesManager) {
                moduleName = ((ProjectModulesManager) modulesManager).resolveModuleOnlyInProjectSources(
                        fileAbsolutePath, addExternal);
            }
        }
        return moduleName;
    }

    public static String[] getStrAsStrItems(String str) {
        return str.split("\\|");
    }

    @Override
    public IInterpreterManager getRelatedInterpreterManager() {
        try {
            int interpreterType = getInterpreterType();
            switch (interpreterType) {
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    return InterpreterManagersAPI.getPythonInterpreterManager();

                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    return InterpreterManagersAPI.getJythonInterpreterManager();

                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    return InterpreterManagersAPI.getIronpythonInterpreterManager();

                default:
                    throw new RuntimeException("Unable to find the related interpreter manager for type: "
                            + interpreterType);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    // ------------------------------------------------------------------------------------------ LOCAL CACHES
    public void clearCaches(boolean clearGlobalModulesCache) {
        this.interpreterType = null;
        this.versionPropertyCache = null;
        this.interpreterPropertyCache = null;
        this.pythonPathNature.clearCaches();
        if (clearGlobalModulesCache) {
            ModulesManager.clearCache();
        }
    }

    Integer interpreterType = null; //cache

    @Override
    public void clearBuiltinCompletions() {
        try {
            this.getRelatedInterpreterManager().clearBuiltinCompletions(this.getProjectInterpreterName());
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TokensList getBuiltinCompletions(IModuleRequestState moduleRequest) {
        try {
            return this.getRelatedInterpreterManager().getBuiltinCompletions(this.getProjectInterpreterName(),
                    moduleRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public IModule getBuiltinMod(IModuleRequestState moduleRequest) {
        try {
            return this.getRelatedInterpreterManager().getBuiltinMod(this.getProjectInterpreterName(), moduleRequest);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void clearBuiltinMod() {
        try {
            this.getRelatedInterpreterManager().clearBuiltinMod(this.getProjectInterpreterName());
        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<IPythonNature> getPythonNaturesRelatedTo(int relatedTo) {
        ArrayList<IPythonNature> ret = new ArrayList<IPythonNature>();
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IProject[] projects = root.getProjects();
        for (IProject project : projects) {
            PythonNature nature = getPythonNature(project);
            try {
                if (nature != null) {
                    if (nature.getInterpreterType() == relatedTo) {
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
    @Override
    public int getGrammarVersion() {
        try {
            String version = getVersion(true);
            if (version == null) {
                Log.log("Found null version. Returning default (latest python 3 grammar).");
                return LATEST_GRAMMAR_PY3_VERSION;
            }

            List<String> splitted = StringUtils.split(version, ' ');
            if (splitted.size() != 2) {
                String storeVersion;
                try {
                    storeVersion = getStore().getPropertyFromXml(getPythonProjectVersionQualifiedName());
                } catch (Exception e) {
                    storeVersion = "Unable to get storeVersion. Reason: " + e.getMessage();
                }

                Log.log("Found invalid version: " +
                        version +
                        "\n" +
                        "Returning default (latest python 3)\n" +
                        "Project: " +
                        this.project +
                        "\n" +
                        "versionPropertyCache: " +
                        versionPropertyCache +
                        "\n" +
                        "storeVersion:" +
                        storeVersion);

                return LATEST_GRAMMAR_PY3_VERSION;
            }

            String grammarVersion = splitted.get(1);
            return getGrammarVersionFromStr(grammarVersion);

        } catch (CoreException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param grammarVersion a string in the format 2.x or 3.x
     * @return the grammar version as given in IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION
     */
    public static int getGrammarVersionFromStr(String grammarVersion) {
        //Note that we don't have the grammar for all versions, so, we use the one closer to it (which is
        //fine as they're backward compatible).
        switch (grammarVersion) {
            case "2.0":
            case "2.1":
            case "2.2":
            case "2.3":
            case "2.4":
            case "2.5":
            case "2.6":
            case "2.7":
            case "3.0":
            case "3.1":
            case "3.2":
            case "3.3":
            case "3.4":
            case "3.5":
                return GRAMMAR_PYTHON_VERSION_3_5;

            case "3.6":
                return GRAMMAR_PYTHON_VERSION_3_6;
            case "3.7":
                return GRAMMAR_PYTHON_VERSION_3_7;
            case "3.8":
                return GRAMMAR_PYTHON_VERSION_3_8;
            case "3.9":
                return GRAMMAR_PYTHON_VERSION_3_9;
            case "3.10":
                return GRAMMAR_PYTHON_VERSION_3_10;
            case "3.11":
                return GRAMMAR_PYTHON_VERSION_3_11;

            default:
                break;
        }

        if (grammarVersion != null) {
            if (grammarVersion.startsWith("3")) {
                return LATEST_GRAMMAR_PY3_VERSION;

            } else if (grammarVersion.startsWith("2")) {
                //latest in the 2.x series
                return LATEST_GRAMMAR_PY3_VERSION;
            }
        }

        Log.log("Unable to recognize version: " + grammarVersion + " returning default.");
        return LATEST_GRAMMAR_PY3_VERSION; // Default to python 3 now.
    }

    protected IPythonNatureStore getStore() {
        return pythonNatureStore;
    }

    /**
     * This flag identifies that we're in tests (when that happens, some verifications are more relaxed).
     */
    public static boolean IN_TESTS = false;

    /**
     * @return info on the interpreter configured for this nature.
     * @throws MisconfigurationException
     *
     * @note that an exception will be raised if the
     */
    @Override
    public IInterpreterInfo getProjectInterpreter() throws MisconfigurationException,
            PythonNatureWithoutProjectException {
        if (this.project == null) {
            throw new PythonNatureWithoutProjectException("Project is not set.");
        }

        try {
            String projectInterpreterName = getProjectInterpreterName();
            IInterpreterInfo ret = null;
            IInterpreterManager relatedInterpreterManager = getRelatedInterpreterManager();
            if (relatedInterpreterManager == null) {
                if (IN_TESTS) {
                    return null;
                }
                throw new ProjectMisconfiguredException("Did not expect the interpreter manager to be null.");
            }

            if (IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)) {
                if (relatedInterpreterManager.getInterpreterType() == IPythonNature.INTERPRETER_TYPE_PYTHON) {
                    IPath location = this.project.getLocation();
                    if (location != null) {
                        File projectLocation = location.toFile();
                        ret = PipenvHelper.getPipenvInterpreterInfoForProjectLocation(
                                relatedInterpreterManager.getInterpreterInfos(),
                                projectLocation, relatedInterpreterManager);
                    }
                }
                if (ret == null) {
                    //if it's the default, let's translate it to the outside world
                    ret = relatedInterpreterManager.getDefaultInterpreterInfo(true);
                }
            } else {
                ret = relatedInterpreterManager.getInterpreterInfo(projectInterpreterName, null);
            }
            if (ret == null) {
                final IProject p = this.getProject();
                final String projectName;
                if (p != null) {
                    projectName = p.getName();
                } else {
                    projectName = "null";
                }

                String msg = "Invalid interpreter: " + projectInterpreterName + " configured for project: "
                        + projectName + ".";
                ProjectMisconfiguredException e = new ProjectMisconfiguredException(msg);
                Log.log(e);
                throw e;

            } else {
                return ret;
            }
        } catch (CoreException e) {
            throw new ProjectMisconfiguredException(e);
        }
    }

    /**
     * @return The name of the interpreter that should be used for the nature this project is associated to.
     *
     * Note that this is the name that's visible to the user (and not the actual path of the executable).
     *
     * It can be null if the project is still not set!
     */
    public String getProjectInterpreterName() throws CoreException {
        if (project != null) {
            if (interpreterPropertyCache == null) {
                String storeInterpreter = getStore().getPropertyFromXml(getPythonProjectInterpreterQualifiedName());
                if (storeInterpreter == null) { //there is no such property set (let's set it to the default)
                    setVersion(null, IPythonNature.DEFAULT_INTERPRETER); //will set the interpreterPropertyCache too
                } else {
                    interpreterPropertyCache = storeInterpreter;
                }
            }
        }
        return interpreterPropertyCache;
    }

    @Override
    public String toString() {
        return "PythonNature: " + this.project;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (IProject.class == adapter) {
            return (T) this.project;
        }
        return null;
    }

    public static Set<String> getPathsToSearch() {
        Set<String> pathsToSearch = new LinkedHashSet<String>();
        try {
            Map<String, String> env = SimpleRunner.getDefaultSystemEnv(null);
            if (env.containsKey("PYTHON_HOME")) {
                pathsToSearch.add(env.get("PYTHON_HOME"));
            }
            if (env.containsKey("PYTHONHOME")) {
                pathsToSearch.add(env.get("PYTHONHOME"));
            }
            if (env.containsKey("PATH")) {
                String path = env.get("PATH");
                String separator = SimpleRunner.getPythonPathSeparator();
                final List<String> split = StringUtils.split(path, separator);
                pathsToSearch.addAll(split);
            }
        } catch (CoreException e) {
            Log.log(e);
        }
        if (!PlatformUtils.isWindowsPlatform()) {
            // Paths to search on linux/mac
            pathsToSearch.add("/usr/bin");
            pathsToSearch.add("/usr/local/bin");
        }
        if (PlatformUtils.isMacOsPlatform()) {
            // Path to search on mac
            pathsToSearch.add("/Library/Frameworks/Python.framework/Versions/Current/bin");
        }
        return pathsToSearch;
    }

}
