/*
 * Author: atotic
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.plugin;

import java.io.File;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.IASTManager;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.PyProjectPythonDetails;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.JobProgressComunicator;
import org.python.pydev.utils.REF;

/**
 * PythonNature is currently used as a marker class.
 * 
 * When python nature is present, project gets extra properties. Project gets assigned python nature when: - a python file is edited - a
 * python project wizard is created
 * 
 *  
 */
public class PythonNature implements IProjectNature {

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
    private IASTManager astManager;

    /**
     * We have to know if it has already been initialized.
     */
    private boolean initialized;

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
    }

    /**
     * Utility routine to add PythonNature to the project
     */
    public static synchronized void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (project == null) {
            return;
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
        }

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
        if (initialized == false) {
            initialized = true;

            astManager = null;
            
            Job myJob = new Job("Pydev code completion") {

                protected IStatus run(IProgressMonitor monitor) {

                    astManager = (IASTManager) REF.readFromFile(getAstOutputFile());
                    restoreAditionalManagers();
                    return Status.OK_STATUS;
                }
            };
            myJob.schedule();

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
     * This method is called whenever the pythonpath for the project with this nature is changed. 
     */
    public void rebuildPath(final String paths) {
        Job myJob = new Job("Pydev code completion: rebuilding modules") {

            protected IStatus run(IProgressMonitor monitor) {
                if(astManager == null){
                    astManager = new ASTManager();
                }

                astManager.changePythonPath(paths, project, new JobProgressComunicator(monitor, "Rebuilding modules", 500, this));
                REF.writeToFile(astManager, getAstOutputFile());
                restoreAditionalManagers();

                return Status.OK_STATUS;
            }
        };
        myJob.schedule();
        
    }

    /**
     * This must be called so that 
     */
    private void restoreAditionalManagers() {
        if(astManager != null){
	        IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
	        InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
	        astManager.setSystemModuleManager(info.modulesManager, getProject());
        }
    }
    
    /**
     * @return Returns the completionsCache.
     */
    public IASTManager getAstManager() {
        return astManager;
    }
    
    public void setAstManager(IASTManager astManager){
        this.astManager = astManager;
    }

    /**
     * @param project
     * @return
     */
    public static PythonNature getPythonNature(IProject project) {
        if(project != null){
            try {
                if(project.hasNature(PYTHON_NATURE_ID)){
	                IProjectNature n = project.getNature(PYTHON_NATURE_ID);
	                if(n instanceof PythonNature){
	                    return (PythonNature) n;
	                }
                }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return null;
    }

    /**
     * @return
     */
    public String getVersion() {
        return PyProjectPythonDetails.getPythonVersion();
    }
}