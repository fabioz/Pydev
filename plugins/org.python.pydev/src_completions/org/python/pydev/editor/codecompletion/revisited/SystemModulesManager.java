/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 24, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.cache.LRUCache;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.modules.EmptyModule;
import org.python.pydev.editor.codecompletion.revisited.modules.PredefinedSourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * @author Fabio Zadrozny
 */
public class SystemModulesManager extends ModulesManager implements ISystemModulesManager{

    /**
     * Version changed from 1.3.6 to 1.3.7 to force it to be reconstructed (because it was not being correctly saved).
     * 
     * Also changed from 1.4.1 to 1.4.2 (because of multiple interpreters: secondary interpreters had no info -- because
     * they weren't actually needed)
     */
    private static final long serialVersionUID = 5L;
    
    /**
     * The system modules manager may have a nature if we create a SystemASTManager
     */
    private transient IPythonNature nature;
    
    /**
     * This is the place where we store the info related to this manager
     */
    private transient InterpreterInfo info;

    /**
     * This method sets the info that contains this modules manager.
     * 
     * @param interpreterInfo the interpreter info that contains this object.
     */
    public void setInfo(Object interpreterInfo) {
        this.info = (InterpreterInfo)interpreterInfo;
    }

    
    /** 
     * @see org.python.pydev.core.ISystemModulesManager#getBuiltins()
     */
    public String[] getBuiltins() {
        return this.info.getBuiltins();
    }


    public void setPythonNature(IPythonNature nature) {
        Assert.isTrue(nature instanceof SystemPythonNature);
        Assert.isTrue(((SystemPythonNature)nature).info == this.info);
        
        this.nature = nature;
    }

    public IPythonNature getNature() {
        if(nature == null){
            IInterpreterManager manager;
            int interpreterType = this.info.getInterpreterType();
            switch(interpreterType){
                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                    manager = PydevPlugin.getJythonInterpreterManager();
                    break;
                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                    manager = PydevPlugin.getPythonInterpreterManager();
                    break;
                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                    manager = PydevPlugin.getIronpythonInterpreterManager();
                    break;
                default:
                    throw new RuntimeException("Don't know how to handle: "+interpreterType);
            }
            nature = new SystemPythonNature(manager, this.info);
        }
        return nature;
    }

    public ISystemModulesManager getSystemModulesManager() {
        return this; //itself
    }

    public IModule getModule(String name, IPythonNature nature, boolean checkSystemManager, boolean dontSearchInit) {
        return getModule(name, nature, dontSearchInit);
    }


    public String resolveModule(String full, boolean checkSystemManager) {
        return super.resolveModule(full);
    }

    
    public List<String> getCompletePythonPath(IInterpreterInfo interpreter, IInterpreterManager manager) {
        if(interpreter == null){
            throw new RuntimeException("The interpreter must be specified (received null)");
        }else{
            return interpreter.getPythonPath();
        }
    }

    public IModule getRelativeModule(String name, IPythonNature nature) {
        return super.getModule(name, nature, true);
    }

    
    /**
     * Called after the pythonpath is changed.
     */
    @Override
    protected void onChangePythonpath(SortedMap<ModulesKey, ModulesKey> keys) {
        //create the builtin modules
        String[] builtins = getBuiltins();
        if(builtins != null){
            for (int i = 0; i < builtins.length; i++) {
                String name = builtins[i];
                final ModulesKey k = new ModulesKey(name, null);
                keys.put(k, k);
            }
        }
    }

    /**
     * This is a cache with the name of a builtin pointing to itself (so, it works basically as a set), it's used
     * so that when we find a builtin that does not have a __file__ token we do not try to recreate it again later.
     */
    private LRUCache<String, String> builtinsNotConsidered; 
    
    private LRUCache<String, String> getBuiltinsNotConsidered(){
        if(builtinsNotConsidered == null){
            builtinsNotConsidered = new LRUCache<String, String>(500);
        }
        return builtinsNotConsidered;
    }
    
    /**
     * @return true if there is a token that has rep as its representation.
     */
    private boolean contains(IToken[] tokens, String rep) {
        for (IToken token : tokens) {
            if(token.getRepresentation().equals(rep)){
                return true;
            }
        }
        return false;
    }
    
    /**
     * Files only get here if we were unable to parse them.
     */
    private transient Map<File, Long> predefinedFilesNotParsedToTimestamp; 

    public AbstractModule getBuiltinModule(String name, boolean dontSearchInit) {
        AbstractModule n = null;
        
        //check for supported builtins these don't have files associated.
        //they are the first to be passed because the user can force a module to be builtin, because there
        //is some information that is only useful when you have builtins, such as os and wxPython (those can
        //be source modules, but they have so much runtime info that it is almost impossible to get useful information
        //from statically analyzing them).
        String[] builtins = getBuiltins();
        if(builtins == null || this.info == null){
            //still on startup
            return null;
        }
        
        //for temporary access (so that we don't generate many instances of it)
        ModulesKey keyForCacheAccess = new ModulesKey(null, null);
        
        //A different choice for users that want more complete information on the libraries they're dealing
        //with is using predefined modules. Those will 
        File predefinedModule = this.info.getPredefinedModule(name);
        if(predefinedModule != null && predefinedModule.exists()){
        	keyForCacheAccess.name = name;
        	keyForCacheAccess.file = predefinedModule;
        	n = cache.getObj(keyForCacheAccess, this);
        	if((n instanceof PredefinedSourceModule)){
        		PredefinedSourceModule predefinedSourceModule = (PredefinedSourceModule) n;
        		if(predefinedSourceModule.isSynched()){
        			return n;
        		}
        		//otherwise (not PredefinedSourceModule or not synched), just keep going to create 
        		//it as a predefined source module
        	}
        	
        	boolean tryToParse = true;
        	Long lastModified = null;
			if(predefinedFilesNotParsedToTimestamp == null){
        		predefinedFilesNotParsedToTimestamp = new HashMap<File, Long>();
        	}else{
	        	Long lastTimeChanged = predefinedFilesNotParsedToTimestamp.get(predefinedModule);
	        	if(lastTimeChanged != null){
	        		lastModified = predefinedModule.lastModified();
	        		if(lastTimeChanged == lastModified){
	        			tryToParse = false;
	        		}else{
	        			predefinedFilesNotParsedToTimestamp.remove(predefinedModule);
	        		}
        		}
        	}
        	
        	
        	if(tryToParse){
	        	IDocument doc;
				try {
					doc = REF.getDocFromFile(predefinedModule);
					IGrammarVersionProvider provider = new IGrammarVersionProvider() {
						
						public int getGrammarVersion() throws MisconfigurationException {
							return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0; // Always Python 3.0 here
						}
					};
					Tuple<SimpleNode, Throwable> obj = PyParser.reparseDocument(
							new PyParser.ParserInfo(doc, true, provider, 0, name, predefinedModule));
					if(obj.o2 != null){
						if(lastModified == null){
							lastModified = predefinedModule.lastModified();
						}
						predefinedFilesNotParsedToTimestamp.put(predefinedModule, lastModified);
						PydevPlugin.log("Unable to parse: "+predefinedModule, obj.o2);
						
					}else if(obj.o1 != null){
						n = new PredefinedSourceModule(name, predefinedModule, obj.o1, obj.o2);
						doAddSingleModule(keyForCacheAccess, n);
						return n;
					}
					//keep on going
				} catch (Throwable e) {
					Log.log(e);
				}
        	}
        }
        
        boolean foundStartingWithBuiltin = false;
        FastStringBuffer buffer = null;
        
        for (int i = 0; i < builtins.length; i++) {
            String forcedBuiltin = builtins[i];
            if (name.startsWith(forcedBuiltin)) {
                if(name.length() > forcedBuiltin.length() && name.charAt(forcedBuiltin.length()) == '.'){
                    foundStartingWithBuiltin = true;
                    
                    keyForCacheAccess.name = name;
                    n = cache.getObj(keyForCacheAccess, this);
                    
                    if(n == null && dontSearchInit == false){
                        if(buffer == null){
                            buffer = new FastStringBuffer();
                        }else{
                            buffer.clear();
                        }
                        keyForCacheAccess.name = buffer.append(name).append(".__init__").toString();
                        n = cache.getObj(keyForCacheAccess, this);
                    }
                    
                    if(n instanceof EmptyModule || n instanceof SourceModule){ 
                        //it is actually found as a source module, so, we have to 'coerce' it to a compiled module
                        n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
                        doAddSingleModule(new ModulesKey(n.getName(), null), n);
                        return n;
                    }
                }
                
                if(name.equals(forcedBuiltin)){
                    
                    keyForCacheAccess.name = name;
                    n = cache.getObj(keyForCacheAccess, this);
                    
                    if(n == null || n instanceof EmptyModule || n instanceof SourceModule){ 
                        //still not created or not defined as compiled module (as it should be)
                        n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
                        doAddSingleModule(new ModulesKey(n.getName(), null), n);
                        return n;
                    }
                }
                if(n instanceof CompiledModule){
                    return n;
                }
            }
        }
        if(foundStartingWithBuiltin){
            LRUCache<String,String> notConsidered = getBuiltinsNotConsidered();
            if(notConsidered.getObj(name) != null){
                return null;
            }
            
            //ok, just add it if it is some module that actually exists
            n = new CompiledModule(name, IToken.TYPE_BUILTIN, this);
            IToken[] globalTokens = n.getGlobalTokens();
            //if it does not contain the __file__, this means that it's not actually a module
            //(but may be a token from a compiled module, so, clients wanting it must get the module
            //first and only then go on to this token).
            //done: a cache with those tokens should be kept, so that we don't actually have to create
            //the module to see its return values (because that's slow)
            if(globalTokens.length > 0 && contains(globalTokens, "__file__")){
                doAddSingleModule(new ModulesKey(name, null), n);
                return n;
            }else{
                notConsidered.add(name, name);
                return null;
            }
        }
        return null;
    }


    /**
     * In the system modules manager, we also have to check for the builtins
     */
    @Override
    public IModule getModule(String name, IPythonNature nature, boolean dontSearchInit) {
        AbstractModule n = getBuiltinModule(name, dontSearchInit);
        if(n != null){
            return n;
        }
        
        return super.getModule(name, nature, dontSearchInit);
    }

    public IModule getModuleWithoutBuiltins(String name, IPythonNature nature, boolean dontSearchInit) {
        return super.getModule(name, nature, dontSearchInit);
    }


    public Tuple<IModule, IModulesManager> getModuleAndRelatedModulesManager(String name, IPythonNature nature,
            boolean checkSystemManager, boolean dontSearchInit) {
        IModule module = this.getModule(name, nature, checkSystemManager, dontSearchInit);
        if(module != null){
            return new Tuple<IModule, IModulesManager>(module, this);
        }
        return null;
    }


}
