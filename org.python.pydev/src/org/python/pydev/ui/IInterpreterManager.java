/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public interface IInterpreterManager {
    
    public String INTERPRETER_PATH = "INTERPRETER_PATH";
        
    public String getDefaultInterpreter();
    
    public String [] getInterpreters();
    
    /**
     * @param executable
     * @return
     */
    public InterpreterInfo getInterpreterInfo(String executable);

    /**
     * 
     * @param executable
     * @return the executable
     */
    public String addInterpreter(String executable);
    
    /**
     * 
     * @param persisted
     * @return list of executables
     */
    public String [] getInterpretersFromPersistedString(String persisted);
    
    /**
     * @param executables
     * @return string to persist
     */
    public String getStringToPersist(String[] executables);
    
    
}
