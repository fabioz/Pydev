/*
 * Created on Nov 18, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.modules;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PythonShell;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class CompiledModule extends AbstractModule{

    private HashMap cache = new HashMap();
    
    /**
     * These are the tokens the compiled module has.
     */
    private CompiledToken[] tokens = new CompiledToken[0];

    /**
     * 
     * @param module - module from where to get completions.
     */
    public CompiledModule(String name){
        this(name, PyCodeCompletion.TYPE_BUILTIN);
    }

    /**
     * 
     * @param module - module from where to get completions.
     */
    public CompiledModule(String name, int tokenTypes){
        super(name);
        System.out.println("creating compiled module: "+name);
        try {
            PythonShell shell = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL);
            List completions = shell.getImportCompletions(name);
            
            ArrayList array = new ArrayList();
            
            for (Iterator iter = completions.iterator(); iter.hasNext();) {
                String[] element = (String[]) iter.next();
                IToken t = new CompiledToken(element[0], element[1], name, tokenTypes);
                array.add(t);
                
            }
            tokens = (CompiledToken[]) array.toArray(new CompiledToken[0]);
        } catch (Exception e) {
            e.printStackTrace();
            PydevPlugin.log(e);
        }

    }
    
    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getWildImportedModules()
     */
    public IToken[] getWildImportedModules() {
        return new IToken[0];
    }

    /**
     * Compiled modules do not have imports to be seen
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getTokenImportedModules()
     */
    public IToken[] getTokenImportedModules() {
        return new IToken[0];
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getGlobalTokens()
     */
    public IToken[] getGlobalTokens() {
        return tokens;
    }

    /**
     * @see org.python.pydev.editor.javacodecompletion.AbstractModule#getDocString()
     */
    public String getDocString() {
        return "compiled extension";
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#getGlobalTokens(java.lang.String)
     */
    public IToken[] getGlobalTokens(String token, ASTManager manager, int line, int col, PythonNature nature) {
        Object v = cache.get(token);
        if(v != null){
            return (IToken[]) v;
        }
        
        IToken[] toks = new IToken[0];

        try {
            PythonShell shell = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL);
            List completions = shell.getImportCompletions(name+"."+token);
            
            ArrayList array = new ArrayList();
            
            for (Iterator iter = completions.iterator(); iter.hasNext();) {
                String[] element = (String[]) iter.next();
                IToken t = new CompiledToken(element[0], element[1], name, PyCodeCompletion.TYPE_BUILTIN);
                array.add(t);
                
            }
            toks = (CompiledToken[]) array.toArray(new CompiledToken[0]);
            cache.put(token, toks);
        } catch (Exception e) {
            e.printStackTrace();
            PydevPlugin.log(e);
        }
        return toks;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    public AssignDefinition[] findDefinition(String token, int line, int col) throws Exception {
        return new AssignDefinition[0];
    }

}
