/*
 * License: Common Public License v1.0
 * Created on Mar 11, 2004
 * 
 * @author Fabio Zadrozny
 * @author atotic
 */
package org.python.pydev.plugin.nature;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
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
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;
import org.python.pydev.utils.JobProgressComunicator;

import sun.misc.BASE64Decoder;

/**
 * PythonNature is currently used as a marker class.
 * 
 * When python nature is present, project gets extra properties. Project gets assigned python nature when: - a python file is edited - a
 * python project wizard is created
 * 
 *  
 */
public class PythonNature implements IProjectNature, IPythonNature {

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
    private boolean initialized;

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

    public static synchronized void addNature(IEditorInput element) {
        if(element instanceof FileEditorInput){
			IFile file = (IFile)((FileEditorInput)element).getAdapter(IFile.class);
			if (file != null){
				try {
	                PythonNature.addNature(file.getProject(), null);
	            } catch (CoreException e) {
	                PydevPlugin.log(e);
	            }
			}
		}
    }

    /**
     * Utility routine to add PythonNature to the project
     */
    public static synchronized void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
        if (project == null) {
            return;
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

                    astManager = (ICodeCompletionASTManager) IOUtils.readFromFile(getAstOutputFile());
                    //errors can happen when restoring it
                    if(astManager != null){
	                    restoreSystemManager();
	                    
                    }else{
                        try {
                            String pythonPathStr = pythonPathNature.getOnlyProjectPythonPathStr();
                            rebuildPath(pythonPathStr);
                        } catch (CoreException e) {
                            
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
     * Returns the directory that should store completions.
     * 
     * @param p
     * @return
     */
    private static File getCompletionsCacheDir(IProject p) {
        IPath location = p.getWorkingLocation(PydevPlugin.getPluginID());
        IPath path = location;
    
        File file = new File(path.toOSString());
        return file;
    }
    
    private File getCompletionsCacheDir() {
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
                saveAstManager(false);
                restoreSystemManager();

                return Status.OK_STATUS;
            }
        };
        myJob.schedule();
        
    }

    /**
     * This must be called so that the system manager is restored.
     */
    private void restoreSystemManager() {
        try {
            if (astManager != null) {
                IInterpreterManager iMan = PydevPlugin.getInterpreterManager();
                InterpreterInfo info = iMan.getDefaultInterpreterInfo(new NullProgressMonitor());
                astManager.setSystemModuleManager(info.modulesManager, getProject());
            }
        } catch (Exception e) {
            PydevPlugin.log(e);
        }
    }
    
    /**
     * @return Returns the completionsCache.
     */
    public ICodeCompletionASTManager getAstManager() {
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
        return pythonNature.pythonPathNature;
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
     * @return the python version for the project
     * @throws CoreException 
     */
    public String getVersion() throws CoreException {
        if(project != null){
            String persistentProperty = project.getPersistentProperty(PYTHON_PROJECT_VERSION);
            if(persistentProperty == null){ //there is no such property set (let's set it to the default
                String defaultVersion = getDefaultVersion();
                setVersion(defaultVersion);
                persistentProperty = defaultVersion;
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
    
    public void saveAstManager(boolean saveNow) {
        //TODO: put into a save list...
        REF.writeToFile(astManager, getAstOutputFile());
    }

}


class IOUtils {
    /**
     * @param persisted
     * @return
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static Object getStrAsObj(String persisted) throws IOException, ClassNotFoundException {
        BASE64Decoder decoder = new BASE64Decoder();
        InputStream input = new ByteArrayInputStream(decoder.decodeBuffer(persisted));
        ObjectInputStream in = new ObjectInputStream(input);
        Object list = in.readObject();
        in.close();
        input.close();
        return list;
    }

    /**
     * @param astOutputFile
     * @return
     */
    public static Object readFromFile(File astOutputFile) {
        try {
            InputStream input = new FileInputStream(astOutputFile);
            ObjectInputStream in = new ObjectInputStream(input);
            Object o = in.readObject();
            in.close();
            input.close();
            return o;
        } catch (Exception e) {
            Log.log(e);
            return null;
        }
    }

}