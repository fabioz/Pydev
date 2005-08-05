/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public interface IInterpreterManager {
    
    /**
     * This is the constant from where we get the interpreter info
     */
    public String INTERPRETER_PATH = "INTERPRETER_PATH_NEW";
        
    /**
     * @return the default interpreter.
     */
    public String getDefaultInterpreter();
    
    /**
     * @return the list of configured interpreters.
     */
    public String [] getInterpreters();
    
    /**
     * @param executable this is the executable from where we want to get the info
     * @return information on the executable
     */
    public InterpreterInfo getInterpreterInfo(String executable, IProgressMonitor monitor);

    /**
     * @param monitor
     * @return the default interpreter info.
     */
    public InterpreterInfo getDefaultInterpreterInfo(IProgressMonitor monitor);
    
    /**
     * This function should be used to add an interpreter to the system. Note that it should not be
     * really added to the system here.
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
     * @return whether we have information on the default interpreter.
     */
    public boolean hasInfoOnDefaultInterpreter();
    
    
}
