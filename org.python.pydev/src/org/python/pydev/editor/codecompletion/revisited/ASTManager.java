/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.CompletionRequest;
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

    private static final long serialVersionUID = 1L;
    
    /**
     * This is the guy that will handle project things for us
     */
    public ProjectModulesManager projectModulesManager = new ProjectModulesManager();
    public ProjectModulesManager getProjectModulesManager(){
        return projectModulesManager;
    }
    
    /**
     * Set the project this ast manager works with.
     */
    public void setProject(IProject project, boolean restoreDeltas){
        projectModulesManager.setProject(project, restoreDeltas);
    }

    /**
     * Set the nature this ast manager works with (if no project is available and a nature is).
     */
    public void setNature(IPythonNature nature){
        projectModulesManager.setPythonNature(nature);
    }
    
    public IPythonNature getNature() {
        return projectModulesManager.getNature();
    }
    
    //----------------------- AUXILIARIES


    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        projectModulesManager.changePythonPath(pythonpath, project, monitor);
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
     * @param original is the name of the import module eg. 'from toimport import ' would mean that the original is 'toimport'
     * or something like 'foo.bar' or an empty string (if only 'import').
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public IToken[] getCompletionsForImport(final String original, CompletionRequest request) {
        PythonNature nature = request.nature;
        
        String relative = null;
        if(request.editorFile != null){
            String moduleName = nature.getAstManager().getProjectModulesManager().resolveModule(REF.getFileAbsolutePath(request.editorFile));
            if(moduleName != null){
                String tail = FullRepIterable.headAndTail(moduleName)[0];
                if(original.length() > 0){
                    relative = tail+"."+original;
                }else{
                    relative = tail;
                }
            }
        }
        
        String absoluteModule = original;
        if (absoluteModule.endsWith(".")) {
            absoluteModule = absoluteModule.substring(0, absoluteModule.length() - 1);
        }
        absoluteModule = absoluteModule.toLowerCase().trim();

        //set to hold the completion (no duplicates allowed).
        Set<IToken> set = new HashSet<IToken>();

        //first we get the imports... that complete for the token.
        getAbsoluteImportTokens(absoluteModule, set, PyCodeCompletion.TYPE_IMPORT);

        //Now, if we have an initial module, we have to get the completions
        //for it.
        getTokensForModule(original, nature, absoluteModule, set);

        if(relative != null && relative.equals(absoluteModule) == false){
            getAbsoluteImportTokens(relative, set, PyCodeCompletion.TYPE_RELATIVE_IMPORT);
            getTokensForModule(relative, nature, relative, set);
        }
        return (IToken[]) set.toArray(new IToken[0]);
    }

    /**
     * @param moduleToGetTokensFrom the string that represents the token from where we are getting the imports
     * @param set the set where the tokens should be added
     */
    private void getAbsoluteImportTokens(String moduleToGetTokensFrom, Set<IToken> set, int type) {
        for (Iterator iter = Arrays.asList(projectModulesManager.getAllModules()).iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();

            String element = key.name;

            if (element.toLowerCase().startsWith(moduleToGetTokensFrom)) {
                element = element.substring(moduleToGetTokensFrom.length());

                boolean goForIt = false;
                //if initial is not empty only get those that start with a dot (submodules, not
                //modules that start with the same name).
                //e.g. we want xml.dom
                //and not xmlrpclib
                //if we have xml token (not using the qualifier here) 
                if (moduleToGetTokensFrom.length() != 0) {
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
                        set.add(new ConcreteToken(splitted[0], "", "", moduleToGetTokensFrom, type));
                    }
                }
            }
        }
    }

    /**
     * @param original this is the initial module where the completion should happen (may have class in it too)
     * @param moduleToGetTokensFrom
     * @param set set where the tokens should be added
     */
    private void getTokensForModule(String original, PythonNature nature, String moduleToGetTokensFrom, Set<IToken> set) {
        if (moduleToGetTokensFrom.length() > 0) {
            if (original.endsWith(".")) {
                original = original.substring(0, original.length() - 1);
            }

            Tuple<AbstractModule, String> modTok = findModuleFromPath(original, nature, false);
            AbstractModule m = modTok.o1;
            String tok = modTok.o2;
            
            if(m == null){
            	//we were unable to find it with the given path, so, there's nothing else to do here...
            	return;
            }
            
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

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name
     * @return the module represented by this name
     */
    public AbstractModule getModule(String name, PythonNature nature, boolean isLookingForRelative) {
        return projectModulesManager.getModule(name, nature, isLookingForRelative);
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager#getCompletionsForToken(java.io.File, org.eclipse.jface.text.IDocument, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForToken(File file, IDocument doc, CompletionState state) {
        AbstractModule module = createModule(file, doc, state, this);
        return getCompletionsForModule(module, state);
    }

    /**
     * @param file
     * @param doc
     * @param state
     * @return
     */
    public static AbstractModule createModule(File file, IDocument doc, CompletionState state, ICodeCompletionASTManager manager) {
        String moduleName = "";
        if(file != null){
            moduleName = manager.getProjectModulesManager().resolveModule(REF.getFileAbsolutePath(file));
        }
        AbstractModule module = AbstractModule.createModuleFromDoc(moduleName, file, doc, state.nature, state.line);
        return module;
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
            AbstractModule m = getModule("__builtin__", state.nature, false);
            return m.getGlobalTokens(state2, this);
        }
        return null;
    }

    /** 
     * @see org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManage#getCompletionsForModule(org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule, org.python.pydev.editor.codecompletion.revisited.CompletionState)
     */
    public IToken[] getCompletionsForModule(AbstractModule module, CompletionState state) {
        ArrayList<IToken> importedModules = new ArrayList<IToken>();
        if(state.localImportsGotten == false){
            //in the first analyzed module, we have to get the local imports too. 
            state.localImportsGotten = true;
            if(module != null){
                importedModules.addAll(module.getLocalImportedModules(state.line, state.col));
            }
        }

        IToken[] builtinsCompletions = getBuiltinsCompletions(state);
        if(builtinsCompletions != null){
            return builtinsCompletions;
        }
        	
        if (module != null) {

            //get the tokens (global, imported and wild imported)
            IToken[] globalTokens = module.getGlobalTokens();
            importedModules.addAll(Arrays.asList(module.getTokenImportedModules()));
            IToken[] wildImportedModules = module.getWildImportedModules();
            
            
            if (state.activationToken.length() == 0) {

		        List<IToken> completions = getGlobalCompletions(globalTokens, importedModules.toArray(new IToken[0]), wildImportedModules, state, module);
		        
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
                
                final IToken[] t = searchOnImportedMods(importedModules.toArray(new IToken[0]), state, module);
                if(t != null && t.length > 0){
                    return t;
                }

                //wild imports: recursively go and get those completions and see if any matches it.
                for (int i = 0; i < wildImportedModules.length; i++) {

                    IToken name = wildImportedModules[i];
                    AbstractModule mod = getModule(name.getAsRelativeImport(module.getName()), state.nature, true); //relative (for wild imports this is ok... only a module can be used in wild imports)
                    
                    if (mod == null) {
                        mod = getModule(name.getOriginalRep(), state.nature, false); //absolute
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
                AbstractModule builtinsMod = getModule("__builtin__", state.nature, false);
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
                throw new RuntimeException(e);
            } catch (Throwable t) {
                throw new RuntimeException("A throwable exception has been detected "+t.getClass());
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
        AbstractModule builtMod = getModule("__builtin__", state.nature, false);
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
        try {
        	//this one is an exception... even though we are getting the name as a relative import, we say it
        	//is not because we want to get the module considering __init__
        	AbstractModule mod = null;
        	
        	if(current != null){
        		//we cannot get the relative path if we don't have a current module
        		mod = getModule(name.getAsRelativeImport(current.getName()), state.nature, false);
        	}

            if (mod == null) {
                mod = getModule(name.getOriginalRep(), state.nature, false); //absolute import
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
        } catch (CompletionRecursionException e) {
            //probably found a recursion... let's return the tokens we have so far
        }
        return completions;
    }

    private IToken[] searchOnImportedMods( IToken[] importedModules, CompletionState state, AbstractModule current) {
        Tuple<AbstractModule, String> o = findOnImportedMods(importedModules, state.nature, state.activationToken, current.getName());
        
        if(o == null)
            return null;
        
        AbstractModule mod = o.o1;
        String tok = o.o2;

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
     */
    public Tuple<AbstractModule, String> findOnImportedMods( PythonNature nature, String activationToken, AbstractModule current) {
        IToken[] importedModules = current.getTokenImportedModules();
        return findOnImportedMods(importedModules, nature, activationToken, current.getName());
    }
    
    /**
     * This function tries to find some activation token defined in some imported module.  
     * @return tuple with: the module and the token that should be used from it.
     * 
     * @param this is the activation token we have. It may be a single token or some dotted name.
     * 
     * If it is a dotted name, such as testcase.TestCase, we need to match against some import
     * represented as testcase or testcase.TestCase.
     * 
     * If a testcase.TestCase matches against some import named testcase, the import is returned and
     * the TestCase is put as the module
     * 
     * 0: mod
     * 1: tok
     */
    public Tuple<AbstractModule, String> findOnImportedMods( IToken[] importedModules, PythonNature nature, String activationToken, String currentModuleName) {
        for (IToken importedModule : importedModules) {
        	
        	FullRepIterable iterable = new FullRepIterable(activationToken, true);
        	for(String tok : iterable){
        	
	            final String modRep = importedModule.getRepresentation(); //this is its 'real' representation (alias) on the file (if it is from xxx import a as yyy, it is yyy)
	            
	            if(modRep.equals(tok)){
	            	Tuple<AbstractModule, String> modTok = null;
	            	AbstractModule mod = null;
	                
	                //check as relative with complete rep
	                modTok = findModuleFromPath(importedModule.getAsRelativeImport(currentModuleName), nature, true);
	                mod = modTok.o1;
	                if(mod != null && mod.getName().equals(currentModuleName) == false){
	                	return fixTok(modTok, tok, activationToken);
	                }
	                
	                //check as absolute with original rep
	                modTok = findModuleFromPath(importedModule.getOriginalRep(), nature, false);
	                mod = modTok.o1;
	                if(mod != null && mod.getName().equals(currentModuleName) == false){
	                	return fixTok(modTok, tok, activationToken);
	                }
	            }
	        }
        }   
        return null;
    }
            
            
    /**
     * Fixes the token if we found a module that was just a substring from the initial activation token.
     * 
     * This means that if we had testcase.TestCase and found it as TestCase, the token is added with TestCase
     */
    private Tuple<AbstractModule, String> fixTok(Tuple<AbstractModule, String> modTok, String tok, String activationToken) {
    	if(activationToken.length() > tok.length() && activationToken.startsWith(tok)){
    		String toAdd = activationToken.substring(tok.length() + 1);
    		if(modTok.o2.length() == 0){
    			modTok.o2 = toAdd;
    		}else{
    			modTok.o2 += "."+toAdd;
    		}
    	}
		return modTok;
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
    private Tuple<AbstractModule, String> findModuleFromPath(String rep, PythonNature nature, boolean isLookingForRelative){
        String tok = "";
        AbstractModule mod = getModule(rep, nature, isLookingForRelative);
        String mRep = rep;
        int index;
        while(mod == null && (index = mRep.lastIndexOf('.')) != -1){
            tok = mRep.substring(index+1) + "."+tok;
            mRep = mRep.substring(0,index);
            mod = getModule(mRep, nature, isLookingForRelative);
        }
        if (tok.endsWith(".")){
            tok = tok.substring(0, tok.length()-1); //remove last point if found.
        }
        return new Tuple<AbstractModule, String>(mod, tok);
    }

    /**
     * @return
     */
    public int getSize() {
        return projectModulesManager.getSize();
    }

}