/*
 * Created on Feb 21, 2006
 */
package org.python.pydev.core;

import java.util.List;

public interface IInterpreterInfo {
    
    /**
     * @return a String such as 2.5 or 2.4 representing the python version that created this interpreter. 
     */
    public String getVersion();
    
    /**
     * @return a constant as defined in IGrammarVersionProvider.
     */
    public int getGrammarVersion();
    
    /**
     * @return a list of strings representing the pythonpath for this interpreter info.
     */
    public List<String> getPythonPath();


    public IModulesManager getModulesManager();

    /**
     * @return a valid path for the interpreter (may not be human readable)
     */
    public String getExeAsFileSystemValidPath();
    
    /**
     * @return the environment variables that should be set when running this interpreter.
     * It can be null if the default environment should be used.
     */
    public String[] getEnvVariables();

    /**
     * This method receives the environment variables available for a run and updates them with the environment
     * variables that are contained in this interpreter.
     * 
     * Note that if a key already exists in the passed env and in the env contained for this interpreter, it's overridden.
     */
    public String[] updateEnv(String[] env);

}
