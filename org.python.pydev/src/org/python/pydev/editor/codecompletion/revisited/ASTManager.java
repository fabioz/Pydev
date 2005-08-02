/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.core.REF;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * This structure should be in memory, so that it acts very quickly.
 * 
 * Probably an hierarchical structure where modules are the roots and they 'link' to other modules or other definitions, would be what we
 * want.
 * 
 * The ast manager is a part of the python nature (as a field).
 * 
 * @author Fabio Zadrozny
 */
public class ASTManager implements ICodeCompletionASTManager, Serializable{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public ProjectModulesManager projectModulesManager = new ProjectModulesManager();
    public ProjectModulesManager getProjectModulesManager(){
        return projectModulesManager;
    }

    //----------------------- AUXILIARIES


    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        projectModulesManager.changePythonPath(pythonpath, project, monitor);
    }
    public void setSystemModuleManager(SystemModulesManager systemManager, IProject project){
        projectModulesManager.setSystemModuleManager(systemManager,project);
    }
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor, PythonNature nature) {
        projectModulesManager.rebuildModule(f, doc, project, monitor, nature);
    }
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        projectModulesManager.removeModule(file, project, monitor);
    }




    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public IToken[] getCompletionsForImport(final String original, PythonNature nature) {
        String initial = original;
        if (initial.endsWith(".")) {
            initial = initial.substring(0, initial.length() - 1);
        }
        initial = initial.toLowerCase().trim();

        //set to hold the completion (no duplicates allowed).
        Set<IToken> set = new HashSet<IToken>();

        //first we get the imports... that complete for the token.
        for (Iterator iter = projectModulesManager.keySet().iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();

            String element = key.name;

            if (element.toLowerCase().startsWith(initial)) {
                element = element.substring(initial.length());

                boolean goForIt = false;
                //if initial is not empty only get those that start with a dot (submodules, not
                //modules that start with the same name).
                //e.g. we want xml.dom
                //and not xmlrpclib
                //if we have xml token (not using the qualifier here) 
                if (initial.length() != 0) {
                    if (element.startsWith(".")) {
                        element = element.substring(1);
                        goForIt = true;
                    }
                } else {
                    goForIt = true;
                }

                if (element.length() > 0 && goForIt) {
                    String[] splitted = element.split("\\.");
                    if (splitted.length > 0) {
                        //this is the completion
                        set.add(new ConcreteToken(splitted[0], "", "", initial, PyCodeCompletion.TYPE_IMPORT));
                    }
                }

            }
        }

        //Now, if we have an initial module, we have to get the completions
        //for it.
        if (initial.length() > 0) {
            String nameInCache = original;
            if (nameInCache.endsWith(".")) {
                nameInCache = nameInCache.substring(0, nameInCache.length() - 1);
            }

            Object[] modTok = findModuleFromPath(nameInCache, nature);
            
            
            Object object = modTok[0];
            String tok = (String) modTok[1];
            
            if (object instanceof AbstractModule) {
                AbstractModule m = (AbstractModule) object;

                IToken[] globalTokens;
                if(tok != null && tok.length() > 0){
                    CompletionState state2 = new CompletionState(-1,-1,tok,nature);
                    state2.builtinsGotten = true; //we don't want to get builtins here
                    globalTokens = m.getGlobalTokens(state2, this);
                }else{
                    CompletionState state2 = new CompletionState(-1,-1,"",nature);
                    state2.builtinsGotten = true; //we don't want to get builtins here
                    globalTokens = getCompletionsForModule(m, state2);
                }
                
                for (int i = 0; i < globalTokens.length; i++) {
                    IToken element = globalTokens[i];
                    //this is the completion
                    set.add(element);
                }
            }
        }

        return (IToken[]) set.toArray(new IToken[0]);
    }

//    /**
//     * @return a Set of strings with all the modules.
//     */
//    public ModulesKey[] getAllModules() {
//        return modulesManager.getAllModules();
//    }
//
//    /**
//     * @return
//     */
//    public int getSize() {
//        return modulesManager.getSize();
//    }
//
//    /**
//     * This method returns the module that corresponds to the path passed as a parameter.
//     * 
//     * @param file
//     * @return the module represented by the file.
//     */
//    private AbstractModule getModule(File file, PythonNature nature) {
//        String name = projectModulesManager.resolveModule(REF.getFileAbsolutePath(file));
//        return getModule(name, nature);
//    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name
     * @return the module represented by this name
     */
    public AbstractModule getModule(String name, PythonNature nature) {
        return projectModulesManager.getModule(name, nature);
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager#getCompletionsForToken(java.io.File, org.eclipse.jface.text.IDocument, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForToken(File file, IDocument doc, CompletionState state) {
        String moduleName = "";
        if(file != null){
            moduleName = projectModulesManager.resolveModule(REF.getFileAbsolutePath(file));
        }
        AbstractModule module = AbstractModule.createModuleFromDoc(moduleName, file, doc, state.nature, state.line);
        return getCompletionsForModule(module, state);
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForToken(org.eclipse.jface.text.IDocument, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForToken(IDocument doc, CompletionState state) {
        IToken[] completionsForModule;
        try {
	        Object[] obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, true, state.nature, state.line));
	        SimpleNode n = (SimpleNode) obj[0];
	        AbstractModule module = AbstractModule.createModule(n);
        
            completionsForModule = getCompletionsForModule(module, state);

        } catch (CompletionRecursionException e) {
            completionsForModule = new IToken[]{ new ConcreteToken(e.getMessage(), e.getMessage(), "","", PyCodeCompletion.TYPE_UNKNOWN)};
        }
        
        return completionsForModule;
    }

    /**
     * Identifies the token passed and if it maps to a builtin not 'easily recognizable', as
     * a string or list, we return it.
     * 
     * @param state
     * @return
     */
    private IToken[] getBuiltinsCompletions(CompletionState state){
        CompletionState state2 = state.getCopy();

        //check for the builtin types.
        state2.activationToken = NodeUtils.getBuiltinType(state.activationToken);

        if(state2.activationToken != null){
            AbstractModule m = getModule("__builtin__", state.nature);
            return m.getGlobalTokens(state2, this);
        }
        return null;
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForModule(org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForModule(AbstractModule module, CompletionState state) {
        IToken[] builtinsCompletions = getBuiltinsCompletions(state);
        if(builtinsCompletions != null){
            return builtinsCompletions;
        }
        	
        if (module != null) {

            //get the tokens (global, imported and wild imported)
            IToken[] globalTokens = module.getGlobalTokens();
            IToken[] importedModules = module.getTokenImportedModules();
            IToken[] wildImportedModules = module.getWildImportedModules();

            if (state.activationToken.length() == 0) {

		        List<IToken> completions = getGlobalCompletions(globalTokens, importedModules, wildImportedModules, state, module);
		        
		        //now find the locals for the module
		        if (state.line >= 0){
		            IToken[] localTokens = module.getLocalTokens(state.line, state.col);
		            for (int i = 0; i < localTokens.length; i++) {
                        completions.add(localTokens[i]); 
                    }
		        }

                return completions.toArray(new IToken[0]);
                
            }else{ //ok, we have a token, find it and get its completions.
                
                //first check if the token is a module... if it is, get the completions for that module.
                //TODO: COMPLETION: when we get here, we might have the module or something imports
                //from a module, so, first we check if it is a module or module token.
                
                final IToken[] t = searchOnImportedMods(importedModules, state, module);
                if(t != null && t.length > 0){
                    return t;
                }

                //wild imports: recursively go and get those completions and see if any matches it.
                for (int i = 0; i < wildImportedModules.length; i++) {

                    IToken name = wildImportedModules[i];
                    AbstractModule mod = getModule(name.getCompletePath(), state.nature);
                    
                    if (mod == null) {
                        mod = getModule(name.getRepresentation(), state.nature);
                    }
                    
                    if (mod != null) {
                        IToken[] completionsForModule = getCompletionsForModule(mod, state);
                        if(completionsForModule.length > 0)
                            return completionsForModule;
                    } else {
                        //"Module not found:" + name.getRepresentation()
                    }
                }

                //it was not a module (would have returned already), so, try to get the completions for a global token defined.
                IToken[] tokens = null;
                tokens = module.getGlobalTokens(state, this);
                if (tokens.length > 0){
                    return tokens;
                }
                
                //If it was still not found, go to builtins.
                AbstractModule builtinsMod = getModule("__builtin__", state.nature);
                if(builtinsMod != null && builtinsMod != module){
	                tokens = getCompletionsForModule( builtinsMod, state);
	                if (tokens.length > 0){
	                    if (tokens[0].getRepresentation().equals("ERROR:") == false){
	                        return tokens;
	                    }
	                }
                }
                
                return getAssignCompletions( module, state);
            }

            
        }else{
            System.err.println("Module passed in is null!!");
        }
        
        return new IToken[0];
    }

    /**
     * If we got here, either there really is no definition from the token
     * or it is not looking for a definition. This means that probably
     * it is something like.
     * 
     * It also can happen in many scopes, so, first we have to check the current
     * scope and then pass to higher scopes
     * 
     * e.g. foo = Foo()
     *      foo. | Ctrl+Space
     * 
     * so, first thing is discovering in which scope we are (Storing previous scopes so 
     * that we can search in other scopes as well).
     * 
     * 
     * @param activationToken
     * @param qualifier
     * @param module
     * @param line
     * @param col
     * @return
     */
    public IToken[] getAssignCompletions( AbstractModule module, CompletionState state) {
        if (module instanceof SourceModule) {
            SourceModule s = (SourceModule) module;
            try {
                Definition[] defs = s.findDefinition(state.activationToken, state.line, state.col, state.nature);
                for (int i = 0; i < defs.length; i++) {
                    if(!(defs[0].ast instanceof FunctionDef)){
                        //we might want to extend that later to check the return of some function...
                                
	                    CompletionState copy = state.getCopy();
	                    copy.activationToken = defs[i].value;
	                    copy.line = defs[i].line;
	                    copy.col = defs[i].col;
	                    module = defs[i].module;

	                    state.checkDefinitionMemory(module, defs[i]);
	                            
	                    IToken[] tks = getCompletionsForModule(module, copy);
	                    if(tks.length > 0)
	                        return tks;
                    }
                }
                
                
            } catch (CompletionRecursionException e) {
                //thats ok
            } catch (Exception e) {
                e.printStackTrace();
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return new IToken[0];
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getGlobalCompletions
     */
    public List getGlobalCompletions(IToken[] globalTokens, IToken[] importedModules, IToken[] wildImportedModules, CompletionState state, AbstractModule current) {
        List<IToken> completions = new ArrayList<IToken>();

        //in completion with nothing, just go for what is imported and global tokens.
        for (int i = 0; i < globalTokens.length; i++) {
            completions.add(globalTokens[i]);
        }

        //now go for the token imports
        for (int i = 0; i < importedModules.length; i++) {
            completions.add(importedModules[i]);
        }

        //wild imports: recursively go and get those completions.
        for (int i = 0; i < wildImportedModules.length; i++) {

            IToken name = wildImportedModules[i];
            getCompletionsForWildImport(state, current, completions, name);
        }

        if(!state.builtinsGotten){
            state.builtinsGotten = true;
            //last thing: get completions from module __builtin__
            getBuiltinCompletions(state, completions);
        }
        return completions;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getBuiltinCompletions
     */
    public List getBuiltinCompletions(CompletionState state, List completions) {
        AbstractModule builtMod = getModule("__builtin__", state.nature);
        if(builtMod != null){
            IToken[] toks = builtMod.getGlobalTokens();
            for (int i = 0; i < toks.length; i++) {
                completions.add(toks[i]);
            }
        }
        return completions;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForWildImport
     */
    public List getCompletionsForWildImport(CompletionState state, AbstractModule current, List completions, IToken name) {
        AbstractModule mod = getModule(name.getCompletePath(), state.nature); //relative import
        
        if (mod == null) {
            mod = getModule(name.getRepresentation(), state.nature);
        }
        
        if (mod != null) {
            state.checkWildImportInMemory(current, mod);
            IToken[] completionsForModule = getCompletionsForModule(mod, state);
            for (int j = 0; j < completionsForModule.length; j++) {
                completions.add(completionsForModule[j]);
            }
        } else {
            //"Module not found:" + name.getRepresentation()
        }
        return completions;
    }

    private IToken[] searchOnImportedMods( IToken[] importedModules, CompletionState state, AbstractModule current) {
        Object [] o = findOnImportedMods(importedModules, state.nature, state.activationToken, current);
        
        if(o == null)
            return null;
        
        if(o.length > 2)
            return (IToken[]) o[2];
        
        AbstractModule mod = (AbstractModule) o[0];
        String tok = (String) o[1];

        if(tok.length() == 0){
            //the activation token corresponds to an imported module. We have to get its global tokens and return them.
            CompletionState copy = state.getCopy();
            copy.activationToken = "";
            copy.builtinsGotten = true; //we don't want builtins... 
            return getCompletionsForModule(mod, copy);
        }else if (mod != null){
            CompletionState copy = state.getCopy();
            copy.activationToken = tok;
            copy.col = -1;
            copy.line = -1;
            
            return getCompletionsForModule(mod, copy);
        }
        return null;
    }

    /**
     * @param activationToken
     * @param importedModules
     * @param module
     * @return tuple with:
     * 0: mod
     * 1: tok
     * 2: (optional) completions if they've already been gotten 
     */
    public Object[] findOnImportedMods( PythonNature nature, String activationToken, AbstractModule current) {
        IToken[] importedModules = current.getTokenImportedModules();
        return findOnImportedMods(importedModules, nature, activationToken, current);
    }
    
    /**
     * @param activationToken
     * @param importedModules
     * @param module
     * @return tuple with:
     * 0: mod
     * 1: tok
     * 2: (optional) completions if they've already been gotten 
     */
    private Object[] findOnImportedMods( IToken[] importedModules, PythonNature nature, String activationToken, AbstractModule current) {
        for (int i = 0; i < importedModules.length; i++) {
            final String modRep = importedModules[i].getRepresentation();
            String fullRep = importedModules[i].getCompletePath();
            if(modRep.equals(activationToken) || fullRep.equals(activationToken)){
                String rep = importedModules[i].getCompletePath();
                
                Object [] o = null;
                AbstractModule mod = null;
                String tok = null;
                
                try {
                    if (current instanceof SourceModule) {
                        File f = ((SourceModule) current).getFile();
                        if (f != null) {
                            String full = f.toString();
                            full = this.projectModulesManager.resolveModule(full);
                            full = full.substring(0, full.lastIndexOf('.'));
                            if (full != null) {
                                full += "." + rep;
                                o = findModuleFromPath(full, nature);
                                mod = (AbstractModule) o[0];
                                tok = (String) o[1];
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    //e.printStackTrace();//that's ok...
                }
                
                if(o == null || mod == null || tok == null || current == mod || tok.equals(rep)){  
	                o = findModuleFromPath(rep, nature);
	                mod = (AbstractModule) o[0];
	                tok = (String) o[1];
                }
                
                if(tok.length() == 0){
                    //the activation token corresponds to an imported module. We have to get its global tokens and return them.
                    return new Object[]{ mod, ""};
                    
                }else if (mod != null){
                    if(mod.isInGlobalTokens(tok)){
                        return new Object[]{ mod, tok};
                    }
                    
                    //ok, it was not a global token, still, it might be some import from that module.
                    IToken[] tokenImportedModules = mod.getTokenImportedModules();
                    for (int j = 0; j < tokenImportedModules.length; j++) {
                        if(tokenImportedModules[j].getRepresentation().equals(tok)){
                            String path = tokenImportedModules[j].getCompletePath();
                            Object [] o2 = findModuleFromPath(path , nature);
                            AbstractModule mod2 = (AbstractModule) o2[0];
                            String tok2 = (String) o2[1];
                            
                            if(mod2 == null){
                                path = mod.getName()+"."+tokenImportedModules[j].getCompletePath();
                                o2 = findModuleFromPath(path , nature);
                                mod2 = (AbstractModule) o2[0];
                                tok2 = (String) o2[1];
                            }
                            
                            return new Object[]{ mod2, tok2};
                        }
                    }
                    IToken[] wildImportedModules = mod.getWildImportedModules();
                    for (int j = 0; j < wildImportedModules.length; j++) {
                        AbstractModule mod2 = getModule(wildImportedModules[j].getCompletePath(), nature);
                        
                        if (mod2 == null) {
                            mod2 = getModule(wildImportedModules[j].getRepresentation(), nature);
                        }
                        
                        if (mod2 != null) {
                            //the token to find is already specified.
                            if(tok != null){
	                            return new Object[]{ mod2, tok};
                            }else{
	                            return new Object[]{ mod2, activationToken};
                            }
                        }
                            
                        
                    }
                    
                }

                
            }else if (activationToken.startsWith(modRep)){
                //this is something like
                //import qt
                //
                //qt.QWidget.| Ctrl+Space
                //
                //so, we have to find the qt module and then go for the token.
                String subst = activationToken.substring(modRep.length());
                Object [] o = findModuleFromPath(importedModules[i].getCompletePath() + subst, nature);
                AbstractModule mod = (AbstractModule) o[0];
                String tok = (String) o[1];
                
                if(mod == current){
                    Object[] o1 = findModuleFromPath(importedModules[i].getRepresentation() + subst, nature);
                    AbstractModule mod1 = (AbstractModule) o1[0];
                    String tok1 = (String) o1[1];
                    if(mod1 != null){
                        mod = mod1;
                        tok = tok1;
                    }
                    
                    else{
                        return null; //we don't want to recurse...
                    }
                }
                return new Object[]{mod, tok};
            }
        }
        return null;
    }

    /**
     * This function receives a path (rep) and extracts a module from that path.
     * First it tries with the full path, and them removes a part of the final of
     * that path until it finds the module or the path is empty.
     * 
     * @param rep
     * @return tuple with found module and the String removed from the path in
     * order to find the module.
     */
    private Object [] findModuleFromPath(String rep, PythonNature nature){
        String tok = "";
        AbstractModule mod = getModule(rep, nature);
        String mRep = rep;
        int index;
        while(mod == null && (index = mRep.lastIndexOf('.')) != -1){
            tok = mRep.substring(index+1) + "."+tok;
            mRep = mRep.substring(0,index);
            mod = getModule(mRep, nature);
        }
        if (tok.endsWith(".")){
            tok = tok.substring(0, tok.length()-1); //remove last point if found.
        }
        return new Object[]{mod, tok};
    }

    /**
     * @return
     */
    public int getSize() {
        return projectModulesManager.getSize();
    }

}