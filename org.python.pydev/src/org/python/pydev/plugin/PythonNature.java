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
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.ui.PyProjectProperties;
import org.python.pydev.utils.JobProgressComunicator;

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
    private ASTManager astManager;

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
        if (hasBuilder(commands) == false) {

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
                    File dir = getCompletionsCacheDir();

                    //this is ok, just set it directly... (if it is still null anyway, 
                    //some error happened).
                    astManager = ASTManager.restoreASTManager(dir, monitor, this);

                    //failed if still null.
                    if (astManager == null) {
                        ASTManager tempAstManager = new ASTManager();
                        try {
                            IProgressMonitor comunicator = new JobProgressComunicator(monitor, "Rebuilding modules", 500, this);
                            comunicator.worked(1);
                            comunicator.setTaskName("Rebuilding modules.");
                            String pathStr = PyProjectProperties.getProjectPythonPathStr(project);
                            tempAstManager.rebuildModules(pathStr, comunicator);
                            astManager = tempAstManager; //only make it available after it is all loaded (better than synchronizing it).
                            saveIt(); //after rebuilding it, save it.
                            comunicator.done();
                        } catch (CoreException e) {
                            e.printStackTrace();
                            PydevPlugin.log(e);
                        }
                    }
                    return Status.OK_STATUS;
                }
            };
            myJob.schedule();

        }
    }

    /**
     * @return the file that should be used to store the completions.
     */
    private File getCompletionsCacheDir() {
        IProject p = project; 
        return getCompletionsCacheDir(p);
    }

    /**
     * @param p
     * @return
     */
    public static File getCompletionsCacheDir(IProject p) {
        IPath location = p.getWorkingLocation(PydevPlugin.getPluginID());
        IPath path = location;//.addTrailingSeparator().append(name + ".pydevcompletions");

        File file = new File(path.toOSString());
        return file;
    }

    /**
     * This method puts the completions cache in a dump file, so that we can restore it later. We do this ourselves because we don't want
     * this to be stored as a xml, as it is not an optimized format (the object dump should be much faster).
     * 
     * This can be used from time to time to store what we have (you never know when a crash might occur).
     */
    private void saveIt() {
        Job myJob = new Job("Pydev code completion (saving tokens)") {

            protected IStatus run(IProgressMonitor monitor) {
                if (astManager != null) {
                    //synchronize it!!
                    synchronized (astManager) {
                        //write completions cache to outputstream.
                        astManager.saveASTManager(getCompletionsCacheDir(), new JobProgressComunicator(monitor, "Save ast manager", astManager.getSize()+10, this));
                    }
                }
                return Status.OK_STATUS;
            }
        };
        myJob.schedule();
    }

    /**
     * This method is called whenever the pythonpath for the project with this nature is changed. It should then get the Completion Code
     * cache singleton and update it based on the new pythonpath.
     *  
     */
    public void rebuildPath(final String paths) {
        Job myJob = new Job("Pydev code completion: rebuilding modules") {

            protected IStatus run(IProgressMonitor monitor) {
                astManager.rebuildModules(paths, new JobProgressComunicator(monitor, "Rebuilding modules", 500, this));
                if(monitor.isCanceled() == false){
                    saveIt(); //after rebuilding, save it.
                }
                return Status.OK_STATUS;
            }
        };
        myJob.schedule();
        
    }
    
    /**
     * Updates the ast for the resource (that has the given document).
     * @param resource
     * @param document
     */
    public void rebuildDelta(final IResource resource, final IDocument document){
		IPath location = resource.getLocation(); 
		   
		if (astManager != null){
		    astManager.rebuildModule(new File(location.toOSString()), document, resource.getProject());
		}
        
    }


    /**
     * @return Returns the completionsCache.
     */
    public ASTManager getAstManager() {
        return astManager;
    }

    /**
     * @param project
     * @return
     */
    public static PythonNature getPythonNature(IProject project) {
        if(project != null){
            try {
                IProjectNature n = project.getNature(PYTHON_NATURE_ID);
                if(n instanceof PythonNature){
                    return (PythonNature) n;
                }
            } catch (CoreException e) {
                PydevPlugin.log(e);
            }
        }
        return null;
    }
}