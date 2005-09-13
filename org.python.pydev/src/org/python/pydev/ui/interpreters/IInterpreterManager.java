/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

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
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     * 
     * @return the default interpreter.
     */
    public String getDefaultInterpreter();
    
    /**
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     * 
     * @return the list of configured interpreters.
     */
    public String [] getInterpreters();
    
    /**
     * This is not applicable for jython (the interpreter is given by the java plugin - jdt)
     * 
     * @param executable this is the executable from where we want to get the info
     * @return information on the executable
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor);

    /**
     * @param monitor monitor to report the progress.
     * @return the default interpreter info.
     */
    public InterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor);
    
    /**
     * This function should be used to add an interpreter to the system. Note that it should not be
     * persisted here.
     * 
     * @param executable interpreter to be added
     * @param monitor
     * @return the executable gotten (it could be different from the input, because we could receive a link and
     * return the actual executable in the system).
     */
    public String addInterpreter(String executable, IProgressMonitor monitor);
    
    /**
     * @param persisted string previously persisted
     * @return list of executables
     */
    public String [] getInterpretersFromPersistedString(String persisted);
    
    /**
     * @param executables executables that should be persisted
     * @return string to persist with the passed executables.
     */
    public String getStringToPersist(String[] executables);

    /**
     * @param nature is needed because we want to know which kind of project we are dealing with
     * @return whether we have information on the default interpreter.
     */
    public boolean hasInfoOnDefaultInterpreter(IPythonNature nature);

    /**
     * All the information cached should be cleared but the information related to the passed interpreters
     * @param allButTheseInterpreters name of the interpreters that should not have the information cleared
     */
    public void clearAllBut(List<String> allButTheseInterpreters);

    /**
     * @return whether this manager treats jython
     */
    public boolean isJython();

    /**
     * @return whether this manager treats python
     */
    public boolean isPython();

    /**
     * restores the pythonpath for the default selected interpreter (gets its information info 
     * and gets it to do the restore).
     * 
     * @param defaultSelectedInterpreter the name of the interpreter
     * @param monitor monitor used for the restore
     */
    public void restorePythopathFor(String defaultSelectedInterpreter, IProgressMonitor monitor);

    /**
     * @return the name that is related to this manager (e.g.: python, jython...)
     */
    String getManagerRelatedName();
    
}
