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

import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * @author Fabio Zadrozny
 */
public class CompiledModule extends AbstractModule{
    
    public static boolean COMPILED_MODULES_ENABLED = true; 

    private HashMap<String, IToken[]> cache = new HashMap<String, IToken[]>();
    
    /**
     * These are the tokens the compiled module has.
     */
    private CompiledToken[] tokens = null;

    /**
     * 
     * @param module - module from where to get completions.
     */
    public CompiledModule(String name, ICodeCompletionASTManager manager){
        this(name, PyCodeCompletion.TYPE_BUILTIN, manager);
    }

    /**
     * 
     * @param module - module from where to get completions.
     */
    public CompiledModule(String name, int tokenTypes, ICodeCompletionASTManager manager){
        super(name);
        if(COMPILED_MODULES_ENABLED){
	        try {
	            AbstractShell shell = AbstractShell.getServerShell(manager.getNature(), AbstractShell.COMPLETION_SHELL);
	            List completions = shell.getImportCompletions(name, manager.getProjectModulesManager().getCompletePythonPath());
	            
	            ArrayList<IToken> array = new ArrayList<IToken>();
	            
	            for (Iterator iter = completions.iterator(); iter.hasNext();) {
	                String[] element = (String[]) iter.next();
	                IToken t = new CompiledToken(element[0], element[1], element[2], name, Integer.parseInt(element[3]));
	                array.add(t);
	                
	            }
                
                //as we will use it for code completion on sources that map to modules, the __file__ should also
                //be added...
                if(name.equals("__builtin__")){
                    array.add(new CompiledToken("__file__","","",name,PyCodeCompletion.TYPE_BUILTIN));
                }
                
	            tokens = array.toArray(new CompiledToken[0]);
	        } catch (Exception e) {
                tokens = new CompiledToken[0];
	            e.printStackTrace();
	            PydevPlugin.log(e);
	        }
        }else{
            //not used if not enabled.
            tokens = new CompiledToken[0];
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
    public IToken[] getGlobalTokens(CompletionState state, ICodeCompletionASTManager manager) {
        Object v = cache.get(state.activationToken);
        if(v != null){
            return (IToken[]) v;
        }
        
        IToken[] toks = new IToken[0];

        if(COMPILED_MODULES_ENABLED){
	        try {
	            AbstractShell shell = AbstractShell.getServerShell(manager.getNature(), AbstractShell.COMPLETION_SHELL);
	            List completions = shell.getImportCompletions(name+"."+state.activationToken, manager.getProjectModulesManager().getCompletePythonPath());
	            
	            ArrayList<IToken> array = new ArrayList<IToken>();
	            
	            for (Iterator iter = completions.iterator(); iter.hasNext();) {
	                String[] element = (String[]) iter.next(); 
	                if(element.length >= 4){//it might be a server error
	                    IToken t = new CompiledToken(element[0], element[1], element[2], name, Integer.parseInt(element[3]));
		                array.add(t);
	                }
	                
	            }
	            toks = (CompiledToken[]) array.toArray(new CompiledToken[0]);
	            cache.put(state.activationToken, toks);
	        } catch (Exception e) {
	            e.printStackTrace();
	            PydevPlugin.log(e);
	        }
        }
        return toks;
    }
    
    @Override
    public boolean isInGlobalTokens(String tok, PythonNature nature) {
        //we have to override because there is no way to check if it is in some import from some other place if it has dots on the tok...
        
        
        if(tok.indexOf('.') == -1){
            return super.isInGlobalTokens(tok, nature);
        }else{
            CompletionState state = CompletionState.getEmptyCompletionState(nature);
            String[] headAndTail = FullRepIterable.headAndTail(tok);
            state.activationToken = headAndTail[0];
            String head = headAndTail[1];
            IToken[] globalTokens = getGlobalTokens(state, nature.getAstManager());
            for (IToken token : globalTokens) {
                if(token.getRepresentation().equals(head)){
                    return true;
                }
            }
        }
        return false;

    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule#findDefinition(java.lang.String, int, int)
     */
    public Definition[] findDefinition(String token, int line, int col, PythonNature nature) throws Exception {
        return new Definition[0];
    }

}
