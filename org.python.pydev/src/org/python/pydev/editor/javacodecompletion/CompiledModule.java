/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author Fabio Zadrozny
 */
public class CompiledModule extends AbstractModule{

    private List tokens;

    /**
     * 
     * @param module - module from where to get completions.
     */
    public CompiledModule(String moduleName){
        System.out.println("creating compiled module: "+moduleName);
        try {
            PythonShell shell = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL);
            List completions = shell.getImportCompletions(moduleName);
            
            tokens = new ArrayList();
            
            for (Iterator iter = completions.iterator(); iter.hasNext();) {
                String[] element = (String[]) iter.next();
                IToken t = new CompiledToken(element[0], element[1]);
                tokens.add(t);
                
            }
        } catch (Exception e) {
            e.printStackTrace();
            PydevPlugin.log(e);
        }

    }
    
    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getWildImportedModules()
     */
    public List getWildImportedModules() {
        return new ArrayList();
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getTokenImportedModules()
     */
    public List getTokenImportedModules() {
        return new ArrayList();
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getGlobalTokens()
     */
    public List getGlobalTokens() {
        return tokens;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getDocString()
     */
    public String getDocString() {
        return "compiled extension";
    }

}
