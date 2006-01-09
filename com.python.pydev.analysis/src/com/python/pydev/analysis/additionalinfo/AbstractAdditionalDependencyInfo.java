/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

import com.python.pydev.analysis.additionalinfo.dependencies.DependencyCalculator;
import com.python.pydev.analysis.additionalinfo.dependencies.PyStructuralChange;

/**
 * Adds dependency information to the interpreter information. This should be used only for
 * classes that are part of a project (this info will not be gotten for the system interpreter) 
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalDependencyInfo extends AbstractAdditionalInterpreterInfo{
    
    /**
     * A token (say a class named Test) points to a list of dependencies which use that token from
     * imports.
     */
    public TreeMap<String, Set<DepInfo>> depInfo = new TreeMap<String, Set<DepInfo>>();
    
    /**
     * A module (key) points to the wild imports it uses (values)
     */
    public TreeMap<String, Set<String>> wildImportsInfo = new TreeMap<String, Set<String>>();
    
    public static boolean TESTING = false;
    
    /**
     * default constructor
     */
    public AbstractAdditionalDependencyInfo() {
	}
    
    @Override
    public void removeInfoFromModule(String moduleName, boolean generateDelta) {
    	if(moduleName == null){
    		throw new AssertionError("The module name may not be null.");
    	}
        super.removeInfoFromModule(moduleName, generateDelta);
    }

    @Override
    protected Object getInfoToSave() {
        return new Tuple(this.initialsToInfo, null);
    }
    
    protected void restoreSavedInfo(Object o){
        Tuple readFromFile = (Tuple) o;
        this.initialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o1;
    }

    /**
     * Add a dependency from a token gotten from a wild import
     * 
     * @param currentModuleName the module that is dependent on the token
     * @param tok the token that was found from a wild import
     */
    public void addDepFromWildImportTok(String currentModuleName, String tok) {
		addDepInfo(currentModuleName, null, tok);
    }
    
    /**
     * Add a dependency
     * @param currentModuleName this is the module that is being analyzed
     * @param mod this is the module that was found to be used
     * @param tok the token which it depends upon
     * @param isWildImport determines if the import we are analyzing is a wild import
     */
    public void addDep(String currentModuleName, AbstractModule mod, String tok, boolean isWildImport) {
    	if(currentModuleName == null){
    		if(!TESTING){
    			throw new RuntimeException("Current module name must NOT be null. Mod:"+mod.getName());
    		}
    		return;
    	}

    	// add dependency for token (regular import)
        addDepInfo(currentModuleName, mod, tok);
        
        // also, add wild import info (if it is a wild import)
        if (isWildImport){ 
            Set<String> wildImps = wildImportsInfo.get(currentModuleName);
            if(wildImps == null){
                wildImps = new HashSet<String>();
                wildImportsInfo.put(currentModuleName, wildImps);
            }
            
            wildImps.add(mod.getName());
        }
    }

    /**
     * @param currentModuleName the current module
     * @param mod the module that is used from the current module (may be null if it is from a wild import)
     * @param tok the token it is dependent upon
     */
    private void addDepInfo(String currentModuleName, AbstractModule mod, String tok) {
    	if(currentModuleName == null){
    		if(!TESTING){
    			throw new RuntimeException("Current module name must NOT be null. Mod:"+mod.getName());
    		}
    		return;
    	}
        if(tok != null && tok.length() > 0){
            Set<DepInfo> deppies = depInfo.get(tok);
            if(deppies == null){
                deppies = new HashSet<DepInfo>();
                depInfo.put(tok, deppies);
            }
            if(mod != null){
                deppies.add(new DepInfo(currentModuleName, mod.getName()));
            }else{
                deppies.add(new DepInfo(currentModuleName));
            }
        }
    }

    public Set<String> calculateDependencies(PyStructuralChange change) {
        return new DependencyCalculator(this).calculateDependencies(change);
    }

    /**
     * Check if we can get from the dependency passed to the module 
     * (e.g.: to check if we can get to a module that has just changed)
     * 
     * @param dep this is the starting place
     * @param module this is the place we may get
     * @return true if we can get a 'wild import path' to the passed module from the dep
     */
    public boolean hasWildImportPath(String from, String module) {
        HashSet<String> alreadyAnalyzed = new HashSet<String>();
        HashSet<String> nextRound = new HashSet<String>();
        nextRound.add(from);
        
        do{
            HashSet<String> thisRound = new HashSet<String>(nextRound);
            nextRound.clear();
            
            for (String mod : thisRound) {
                Set<String> importsInModule = wildImportsInfo.get(mod);
    
                if(importsInModule != null){
                    for (String wildImp : importsInModule) {
                        
                        //if it was not already analyzed
                        if(!alreadyAnalyzed.contains(wildImp)){
                            
                            if(wildImp.equals(module)){
                                return true;
                            }
                             
                            nextRound.add(wildImp);
                            alreadyAnalyzed.add(mod);
                        }
                    }
                }
            }
        }while(nextRound.size() > 0);
        
        return false;
    }


}
