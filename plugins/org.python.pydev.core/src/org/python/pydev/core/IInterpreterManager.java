/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * @author Fabio Zadrozny
 */
public interface IInterpreterManager {
    
    /**
     * This is the constant from where we get the interpreter info
     */
    public String PYTHON_INTERPRETER_PATH = "INTERPRETER_PATH_NEW";
        
    /**
     * This is the constant from where we get the jython jar
     */
    public String JYTHON_INTERPRETER_PATH = "JYTHON_INTERPRETER_PATH";
    
    /**
     * This is the constant from where we get the jython cache dir
     */
    public String JYTHON_CACHE_DIR = "JYTHON_CACHE_DIR";
    
    /**
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     * 
     * @return the default interpreter.
     */
    public String getDefaultInterpreter();
    
    
    /**
     * @return the interpreter infos kept internally in the interpreter manager.
     */
    public IInterpreterInfo[] getInterpreterInfos();
    
    /**
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     * 
     * @param executable this is the executable from where we want to get the info
     * @return information on the executable
     */
    public IInterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor);

    /**
     * @param monitor monitor to report the progress.
     * @return the default interpreter info.
     */
    public IInterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor);
    
    /**
     * This function should be used to create the interpreter info of some executable.
     * 
     * @param executable interpreter for which the info should be created.
     * @param monitor
     * @return the executable gotten (it could be different from the input, because we could receive a link and
     * return the actual executable in the system).
     * @throws JDTNotAvailableException 
     * @throws CoreException 
     */
    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor);
    
    
    /**
     * This function should be used to set an interpreter in the system. Note that it should not be
     * persisted here.
     * 
     * @param executable interpreter to be added
     * @param monitor
     * @throws JDTNotAvailableException 
     * @throws CoreException 
     */
    public void addInterpreterInfo(IInterpreterInfo info);
    
    /**
     * @param persisted string previously persisted
     * @return list of interpreter infos
     */
    public IInterpreterInfo[] getInterpretersFromPersistedString(String persisted);
    
    /**
     * @param executables executables that should be persisted
     * @return string to persist with the passed executables.
     */
    public String getStringToPersist(IInterpreterInfo[] executables);

    /**
     * @param nature is needed because we want to know which kind of project we are dealing with
     * @return whether we have information on the default interpreter.
     */
    public boolean hasInfoOnDefaultInterpreter(IPythonNature nature);

    /**
     * All the information cached should be cleared but the information related to the passed interpreters
     * @param allButTheseInterpreters name of the interpreters that should not have the information cleared
     */
    public void setInfos(List<IInterpreterInfo> allButTheseInterpreters);

    /**
     * @return whether this manager treats jython
     */
    public boolean isJython();

    /**
     * @return whether this manager treats python
     */
    public boolean isPython();

    /**
     * restores the pythonpath for all the interpreters available (gets its information info 
     * and gets it to do the restore).
     * 
     * @param monitor monitor used for the restore
     */
    public void restorePythopathForAllInterpreters(IProgressMonitor monitor);

    /**
     * @return the name that is related to this manager (e.g.: python, jython...)
     */
    String getManagerRelatedName();

    /**
     * @return the Persisted string with the information on this interpreter manager.
     */
    public String getPersistedString();

    /**
     * Set the string to be persisted with the information on this interpreter manager
     * @param s
     */
    public void setPersistedString(String s);

    /**
     * @return whether this manager is correctly configured (interpreter is correctly set)
     */
    public boolean isConfigured();

    /**
     * @return IPythonNature.PYTHON_RELATED or IPythonNature.JYTHON_RELATED 
     */
    public int getRelatedId();

    /**
     * @param interpreter the interpreter we care about. If null is passed, it should go for info
     * on the default interpreter
     * @return whether the interpreter has information on this manager.
     */
    public boolean hasInfoOnInterpreter(String interpreter);

    
    //caches for the builtin tokens and module
    public void setBuiltinCompletions(IToken[] comps);

    public IToken[] getBuiltinCompletions();

    public IModule getBuiltinMod();

    public void setBuiltinMod(IModule mod);

    public void clearCaches();

    /**
     * Saves the system modules managers info so that it can be restored later (it's restored when the plugin is started).
     */
    public void saveInterpretersInfoModulesManager();
    
}
