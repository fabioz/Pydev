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

}
