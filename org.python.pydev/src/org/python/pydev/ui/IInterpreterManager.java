/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui;

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
    public String INTERPRETER_PATH = "INTERPRETER_PATH_NEW";
        
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
     * @return whether we have information on the default interpreter.
     */
    public boolean hasInfoOnDefaultInterpreter(IPythonNature nature);

    /**
     * @return the default jython jar location.
     */
    public String getDefaultJythonJar();

    /**
     * @return the default jython home location.
     */
    public String getDefaultJythonHome();

    /**
     * @return the default jython pythonpath
     */
    public String getDefaultJythonPath();

    /**
     * @return the default java executable location
     */
    public String getDefaultJavaLocation();
    
    
}
