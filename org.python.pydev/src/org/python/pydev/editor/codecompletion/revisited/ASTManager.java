/*
 * Created on Nov 9, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.parser.SimpleNode;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.ModulesKey;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;

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
public class ASTManager implements Serializable, IASTManager {

    /**
     * Modules that we have in memory. This is persisted when saved.
     * 
     * Keys are strings with the name of the module. Values are AbstractModule objects.
     */
    Map modules = new HashMap();

    /**
     * Helper for using the pythonpath. Also persisted.
     */
    PythonPathHelper pythonPathHelper = new PythonPathHelper();

    
    
    
    
    //----------------------- AUXILIARIES

    /**
     * Returns the directory that should store completions.
     * 
     * @param p
     * @return
     */
    static File getCompletionsCacheDir(IProject p) {
        IPath location = p.getWorkingLocation(PydevPlugin.getPluginID());
        IPath path = location;
    
        File file = new File(path.toOSString());
        return file;
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#changePythonPath(java.lang.String, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void changePythonPath(String pythonpath, final IProject project, IProgressMonitor monitor) {
        System.out.println("rebuildModules " + pythonpath);

        List pythonpathList = pythonPathHelper.setPythonPath(pythonpath);

        Map mods = new HashMap();

        List completions = new ArrayList();

        int total = 0;

        //first thing: get all files available from the python path and sum them up.
        for (Iterator iter = pythonpathList.iterator(); iter.hasNext() && monitor.isCanceled() == false;) {
            String element = (String) iter.next();

            //the slow part is getting the files... not much we can do (I think).
            List[] below = pythonPathHelper.getModulesBelow(new File(element), monitor);
            completions.addAll(below[0]);
            total += below[0].size();
        }

        int j = 0;
        
        //now, create in memory modules for all the loaded files (empty modules).
        for (Iterator iterator = completions.iterator(); iterator.hasNext() && monitor.isCanceled() == false; j++) {
            Object o = iterator.next();
            if (o instanceof File) {
                File f = (File) o;
                String m = pythonPathHelper.resolveModule(f.getAbsolutePath());

                monitor.setTaskName(new StringBuffer("Module resolved: ").append(j).append(" of ").append(total).append(" (").append(m)
                        .append(")").toString());
                monitor.worked(1);
                if (m != null) {
					//we don't load them at this time.
                    mods.put(new ModulesKey(m, f), AbstractModule.createEmptyModule(m, f));
                }
            }
        }
        
        //create the builtin modules
        String name = "sys";
        mods.put(new ModulesKey(name, null), AbstractModule.createEmptyModule(name, null));
        
        name = "__builtin__";
        mods.put(new ModulesKey(name, null), AbstractModule.createEmptyModule(name, null));

        //assign to instance variable 
        this.modules = mods;
        
        final ASTManager m = this;
        new Thread(){
            /**
             * @see java.lang.Thread#run()
             */
            public void run() {
                try{
                    ASTManagerIO.savePythonPath(getCompletionsCacheDir(project), new NullProgressMonitor(), m);
                }catch(Exception e){
                    //we're not in the eclipse enviroment
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#rebuildModule(java.io.File, org.eclipse.jface.text.IDocument, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void rebuildModule(File f, IDocument doc, final IProject project, IProgressMonitor monitor) {
        final String m = pythonPathHelper.resolveModule(f.getAbsolutePath());
        if (m != null) {
            final AbstractModule value = AbstractModule.createModuleFromDoc(m, f, doc);
            final ModulesKey key = new ModulesKey(m, f);
            modules.put(key, value);

        }
    }



    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModule(File file, IProject project, IProgressMonitor monitor) {
        String m = pythonPathHelper.resolveModule(file.getAbsolutePath(), false);
        m = PythonPathHelper.stripExtension(m);
        if (m != null) {
            modules.remove(new ModulesKey(m, file));
        }
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#removeModule(java.io.File, org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void removeModulesBelow(File file, IProject project, IProgressMonitor monitor) {
        String m = pythonPathHelper.resolveModule(file.getAbsolutePath(), false);
        List toRem = new ArrayList();
        if (m != null) {
            for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
                ModulesKey key = (ModulesKey) iter.next();
                if(key.name.startsWith(m)){
                    toRem.add(key);
                }
            }
        }
        
        //really remove them here.
        for (Iterator iter = toRem.iterator(); iter.hasNext();) {
            modules.remove(iter.next());
            
        }
    }

    /**
     * @see org.python.pydev.editor.codecompletion.revisited.IASTManager#syncModules(org.eclipse.core.resources.IProject, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void syncModules(IProject project, IProgressMonitor monitor) {
    }


    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    //----------------------------------- COMPLETIONS

    /**
     * Returns the imports that start with a given string. The comparisson is not case dependent. Passes all the modules in the cache.
     * 
     * @param initial: this is the initial module (e.g.: foo.bar) or an empty string.
     * @return a Set with the imports as tuples with the name, the docstring.
     */
    public IToken[] getCompletionsForImport(final String original) {
        String initial = original;
        if (initial.endsWith(".")) {
            initial = initial.substring(0, initial.length() - 1);
        }
        initial = initial.toLowerCase().trim();

        //set to hold the completion (no duplicates allowed).
        Set set = new HashSet();

        //first we get the imports...
        for (Iterator iter = modules.keySet().iterator(); iter.hasNext();) {
            ModulesKey key = (ModulesKey) iter.next();

            String element = key.name;
            
            if (element.toLowerCase().startsWith(initial)) {
                element = element.substring(initial.length());

                boolean goForIt = false;
                //if initial is not empty only get those that start with a dot (submodules, not
                //modules that start with the same name).
                //e.g. we want xml.dom
                //and not xmlrpclib
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
                        set.add(new ConcreteToken(splitted[0], "", initial, PyCodeCompletion.TYPE_IMPORT));
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

            Object object = getModule(nameInCache);
            if (object instanceof AbstractModule) {
                AbstractModule m = (AbstractModule) object;

                IToken[] globalTokens = m.getGlobalTokens();
                for (int i = 0; i < globalTokens.length; i++) {
                    IToken element = globalTokens[i];
                    //this is the completion
                    set.add(element);
                }
            }
        }

        return (IToken[]) set.toArray(new IToken[0]);
    }

    /**
     * @return a Set of strings with all the modules.
     */
    public ModulesKey[] getAllModules() {
        return (ModulesKey[]) modules.keySet().toArray(new ModulesKey[0]);
    }

    /**
     * @return
     */
    public int getSize() {
        return modules.size();
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param file
     * @return the module represented by the file.
     */
    private AbstractModule getModule(File file) {
        String name = pythonPathHelper.resolveModule(file.getAbsolutePath());
        return getModule(name);
    }

    /**
     * This method returns the module that corresponds to the path passed as a parameter.
     * 
     * @param name
     * @return the module represented by this name
     */
    private AbstractModule getModule(String name) {
        AbstractModule n = (AbstractModule) modules.get(new ModulesKey(name, null));
        if (n == null){
            n = (AbstractModule) modules.get(new ModulesKey(name+".__init__", null));
        }
        if(n == null){
            System.out.println("The module "+name+" could not be found!");
        }
        
        if (n instanceof EmptyModule){
            EmptyModule e = (EmptyModule)n;
            System.out.println("Loading module: "+name+ " file "+e.f);

            if(e.f != null){
                try{
                    n = AbstractModule.createModule(name, e.f);
                }catch(FileNotFoundException exc){
                    this.modules.remove(new ModulesKey(name, e.f));
                    n = null;
                }
            }else{
                //check for supported builtins
                if(name.equals("__builtin__")){
                    n = new CompiledModule(name, PyCodeCompletion.TYPE_BUILTIN);
                }else if(name.equals("sys")){
                    n = new CompiledModule(name);
                }else{
                    System.out.println("The module "+name+" could not be found nor created!");
                }
            }
            
            if(n != null){
	            System.out.println("Loaded");
	            this.modules.put(new ModulesKey(name, e.f), n);
            }
        }
        
        if( n instanceof EmptyModule){
            throw new RuntimeException("Should not be an empty module anymore!");
        }
        return n;

    }

    /**
     * The completion should work in the following way:
     * 
     * First we have to know in which scope we are.
     * 
     * If we have no token nor qualifier, get the locals for the file (only from module imports or from inner scope).
     * 
     * If we have a part of the qualifier and not activationToken, go for all that match (e.g. all classes, so that we can make the import
     * automatically)
     * 
     * If we have the activationToken, try to guess what it is and get its attrs and funcs.
     * 
     * @param file
     * @param line
     * @param col
     * @param activationToken
     * @param qualifier
     * @return
     */
    public IToken[] getCompletionsForToken(File file, IDocument doc, int line, int col, String activationToken, String qualifier) {
        AbstractModule module = AbstractModule.createModuleFromDoc("", file, doc);
        return getCompletionsForModule(activationToken, qualifier, module, line, col);
    }

    /**
     * 
     * @param doc
     * @param line
     * @param col
     * @param activationToken
     * @param qualifier
     * @return
     */
    public IToken[] getCompletionsForToken(IDocument doc, int line, int col, String activationToken, String qualifier) {
        Object[] obj = PyParser.reparseDocument(doc, true);
        SimpleNode n = (SimpleNode) obj[0];
        AbstractModule module = AbstractModule.createModule(n);
        return getCompletionsForModule( activationToken, qualifier, module, line, col);
    }

    public IToken[] getCompletionsForModule(String activationToken, String qualifier, AbstractModule module, int line, int col) {
        return getCompletionsForModule(activationToken, qualifier, module, line, col, false);
    }
    
    /**
     * @param file
     * @param activationToken
     * @param qualifier
     * @param module
     * @param col
     * @param line
     */
    private IToken[] getCompletionsForModule(String activationToken, String qualifier, AbstractModule module, int line, int col, boolean recursing) {

        if (module != null) {

            //get the tokens (global, imported and wild imported)
            IToken[] globalTokens = module.getGlobalTokens();
            IToken[] importedModules = module.getTokenImportedModules();
            IToken[] wildImportedModules = module.getWildImportedModules();

            if (activationToken.length() == 0) {

		        List completions = getGlobalCompletions(activationToken, qualifier, line, col, recursing, globalTokens, importedModules, wildImportedModules);
                return (IToken[]) completions.toArray(new IToken[0]);
                
            }else{ //ok, we have a token, find it and get its completions.
                
                //first check if the token is a module... if it is, get the completions for that module.
                //TODO: COMPLETION: when we get here, we might have the module or something imports
                //from a module, so, first we check if it is a module or module token.
                
                final IToken[] t = searchOnImportedMods(activationToken, importedModules, line, col);
                if(t != null && t.length > 0){
                    return t;
                }

                //wild imports: recursively go and get those completions and see if any matches it.
                for (int i = 0; i < wildImportedModules.length; i++) {

                    IToken name = wildImportedModules[i];
                    AbstractModule mod = getModule(name.getCompletePath());
                    
                    if (mod == null) {
                        mod = getModule(name.getRepresentation());
                    }
                    
                    if (mod != null) {
                        IToken[] completionsForModule = getCompletionsForModule(activationToken, qualifier, mod, line, col, true);
                        if(completionsForModule.length > 0)
                            return completionsForModule;
                    } else {
                        System.out.println("Module not found:" + name.getRepresentation());
                    }
                }

                //it was not a module (would have returned already), so, try to get the completions for a global token defined.
                IToken[] tokens = module.getGlobalTokens(activationToken, this, line, col);
                if (tokens.length > 0){
                    return tokens;
                }
                
                //If it was still not found, go to builtins.
                AbstractModule builtinsMod = getModule("__builtin__");
                if(builtinsMod != null){
	                tokens = getCompletionsForModule(activationToken, qualifier, builtinsMod, line, col, true);
	                if (tokens.length > 0){
	                    if (tokens[0].getRepresentation().equals("ERROR:") == false){
	                        return tokens;
	                    }
	                }
                }
                
                return getAssignCompletions(activationToken, qualifier, module, line, col);
            }

            
        }else{
            System.out.println("Module passed in is null!!");
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
    private IToken[] getAssignCompletions(String activationToken, String qualifier, AbstractModule module, int line, int col) {
        if (module instanceof SourceModule) {
            SourceModule s = (SourceModule) module;
            try {
                AssignDefinition[] defs = s.findDefinition(activationToken, line, col);
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return new IToken[0];
    }

    /**
     * @param activationToken
     * @param qualifier
     * @param recursing
     * @param globalTokens
     * @param importedModules
     * @param wildImportedModules
     */
    private List getGlobalCompletions(String activationToken, String qualifier, int line, int col, boolean recursing, IToken[] globalTokens, IToken[] importedModules, IToken[] wildImportedModules) {
        List completions = new ArrayList();

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
            AbstractModule mod = getModule(name.getCompletePath());
            
            if (mod == null) {
                mod = getModule(name.getRepresentation());
            }
            
            if (mod != null) {
                IToken[] completionsForModule = getCompletionsForModule(activationToken, qualifier, mod, line, col, true);
                for (int j = 0; j < completionsForModule.length; j++) {
                    completions.add(completionsForModule[j]);
                }
            } else {
                System.out.println("Module not found:" + name.getRepresentation());
            }
        }

        if(!recursing){
            //last thing: get completions from module __builtin__
            AbstractModule builtMod = getModule("__builtin__");
            if(builtMod != null){
                IToken[] toks = builtMod.getGlobalTokens();
                for (int i = 0; i < toks.length; i++) {
                    completions.add(toks[i]);
                }
            }
        }
        return completions;
    }

    /**
     * @param activationToken
     * @param importedModules
     * @return
     */
    private IToken[] searchOnImportedMods(String activationToken, IToken[] importedModules, int line, int col) {
        for (int i = 0; i < importedModules.length; i++) {
            final String modRep = importedModules[i].getRepresentation();
            if(modRep.equals(activationToken)){
                String rep = importedModules[i].getCompletePath();
                
                Object [] o = findModuleFromPath(rep);
                AbstractModule mod = (AbstractModule) o[0];
                String tok = (String) o[1];
                
                if(tok.length() == 0){
                    //the activation token corresponds to an imported module. We have to get its global tokens and return them.
                    return getCompletionsForModule("", "", mod, line, col, true);
                }else{
                    return mod.getGlobalTokens(tok, this, line, col);
                }

                
            }else if (activationToken.startsWith(modRep)){
                //this is something like
                //import qt
                //
                //qt.QWidget.| Ctrl+Space
                //
                //so, we have to find the qt module and then go for the token.
                Object [] o = findModuleFromPath(activationToken);
                AbstractModule mod = (AbstractModule) o[0];
                String tok = (String) o[1];
                System.out.println(tok);
                
                if(tok.length() == 0){
                    //the activation token corresponds to an imported module. We have to get its global tokens and return them.
                    return getCompletionsForModule("", "", mod, line, col, true);
                }else{
                    return mod.getGlobalTokens(tok, this, line, col);
                }
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
    private Object [] findModuleFromPath(String rep){
        String tok = "";
        AbstractModule mod = getModule(rep);
        String mRep = rep;
        int index;
        while(mod == null && (index = mRep.lastIndexOf('.')) != -1){
            tok = mRep.substring(index+1) + "."+tok;
            mRep = mRep.substring(0,index);
            mod = getModule(mRep);
        }
        if (tok.endsWith(".")){
            tok = tok.substring(0, tok.length()-1); //remove last point if found.
        }
        return new Object[]{mod, tok};
    }
}