/*
 * Author: atotic
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */

package org.python.pydev.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.editor.javacodecompletion.ASTManager;
import org.python.pydev.ui.PyProjectProperties;

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
    public void init() {
        if (initialized == false) {
            initialized = true;
            File file = getCompletionsCacheFile();
            
            if (file.exists()) {
                try {
                    FileInputStream stream = new FileInputStream(file);
                    try {
                        astManager = ASTManager.restoreASTManager(stream);
                    } finally {
                        stream.close();
                    }
                } catch (Exception e) {
                    PydevPlugin.log(e);
                }
            }
            
            if(astManager == null){
                astManager = new ASTManager();
                try {
                    String pathStr = PyProjectProperties.getProjectPythonPathStr(project);
                    astManager.rebuildModules(pathStr);
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
        }
    }
    
    /**
     * @return the file that should be used to store the completions.
     */
    private File getCompletionsCacheFile() {
        IPath location = project.getWorkingLocation(PydevPlugin.getPluginID());
        IPath path = location.addTrailingSeparator().append(project.getName()+".pydevcompletions");

        File file = new File(path.toOSString());
        return file;
    }

    /**
     * This method puts the completions cache in a dump file, so that we can restore it later.
     * We do this ourselves because we don't want this to be stored as a xml, as it is not an
     * optimized format (the object dump should be much faster).
     * 
     * This can be used from time to time to store what we have (you never know when a crash might occur).
     */
    public void saveIt(){
        synchronized(astManager){
	        File file = getCompletionsCacheFile();
	        //create file if needed
	        if(file.exists() == false){
	            try {
	                file.createNewFile();
	            } catch (IOException e1) {
	                PydevPlugin.log(e1);
	                e1.printStackTrace();
	            }
	        }
	        
	        //write completions cache to outputstream.
	        try {
	            astManager.saveASTManager(new FileOutputStream(file));
	        } catch (FileNotFoundException e) {
	            PydevPlugin.log(e);
	            e.printStackTrace();
	        }
        }
    }

    /**
     * This method is called whenever the pythonpath for the project with this nature is changed. It should then get the Completion Code
     * cache singleton and update it based on the new pythonpath.
     *  
     */
    public void rebuildPath(String paths) {
        astManager.rebuildModules(paths);
    }

    /**
     * @param completionsCache The completionsCache to set.
     */
    public void setAstManager(ASTManager completionsCache) {
        this.astManager = completionsCache;
    }

    /**
     * @return Returns the completionsCache.
     */
    public ASTManager getAstManager() {
        return astManager;
    }
}