/*
 * Created on 28/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.python.pydev.core.Tuple;

/**
 * Adds dependency information to the interpreter information. This should be used only for
 * classes that are part of a project (this info will not be gotten for the system interpreter) 
 * 
 * @author Fabio
 */
public abstract class AbstractAdditionalDependencyInfo extends AbstractAdditionalInterpreterInfo{
    
    
    /**
     * used so that we can map module dependencies
     * 
     * the module (key) maps to its dependencies (values)
     */
    protected Map<String,Set<String>> moduleDependencies = new TreeMap<String, Set<String>>();

    @Override
    public void removeInfoFromModule(String moduleName) {
        super.removeInfoFromModule(moduleName);
        this.moduleDependencies.remove(moduleName);
    }

    @Override
    protected Object getInfoToSave() {
        return new Tuple(this.initialsToInfo, this.moduleDependencies);
    }
    
    protected void restoreSavedInfo(Object o){
        Tuple readFromFile = (Tuple) o;
        this.initialsToInfo = (TreeMap<String, List<IInfo>>) readFromFile.o1;
        this.moduleDependencies = (Map<String, Set<String>>) readFromFile.o2;

    }

    /**
     * Adds dependency information.
     * 
     * @param analyzedModule this is the module that depends on some other module
     * @param dependsOn this is the module it depends
     * 
     * Note: all paths should be full paths. Relative entries should not be added.
     * This is important, because later it will be looked for with that name.
     *  
     * If the user has some import and it is unable to find it, it can be dependent on both representations
     * (relative and absolute), so that later, if the module is created, it is re-analyzed for its correct 
     * representation.
     */
    public void addDependency(String analyzedModule, String dependsOn) {
        Set<String> dependencies = this.moduleDependencies.get(analyzedModule);
        if(dependencies == null){
            dependencies = new HashSet<String>();
            this.moduleDependencies.put(analyzedModule, dependencies);
        }
        dependencies.add(dependsOn);
    }

    /**
     * This scenario is not as simple as it looks...
     * 
     * when getting dependencies, we have to get 'deep' dependencies, such that if we have:
     * mod1 > imports mod2
     * mod2 > imports mod3
     * mod3 > no dependency
     * 
     * getting dependencies for mod1 should return both, mod2 and mod3
     * 
     * TODO: use some graph theory to do this better!
     */
    public Set<String> getDependencies(String analyzedModule) {
        Set<String> directDependencies = this.moduleDependencies.get(analyzedModule);
        if(directDependencies == null){
            return new HashSet<String>();
        }
        
        //ok, there is something to return
        HashSet<String> toReturn = new HashSet<String>(directDependencies);
        
        //used to know what will we have to look in the next round
        HashSet<String> searchOnNextRound = new HashSet<String>(directDependencies);
        
        HashSet<String> alreadySeached = new HashSet<String>();
        alreadySeached.add(analyzedModule);//the initial has already been analyzed
        
        //now, for each of these we have to get their dependencies. Also, let's take
        //some measures to avoid recursing in this case
        do{
            //these are the ones we will have to go deeper on...
            HashSet<String> stillSearchOnThese = new HashSet<String>(searchOnNextRound);

            //clear next searches
            searchOnNextRound.clear();
            for (String dependency : stillSearchOnThese) {
                alreadySeached.add(dependency);
                
                Set<String> dependencies = this.moduleDependencies.get(dependency);
                if(dependencies != null){
                    for (String dep : dependencies) {
                        if(alreadySeached.contains(dep) == false){
                            searchOnNextRound.add(dep);
                            toReturn.add(dep);
                        }
                    }
                }
            }
        }while(searchOnNextRound.size() != 0);
        
        return toReturn;
    }

    /**
     * This scenario is not as simple as it looks...
     * 
     * when getting dependencies, we have to get 'deep' dependencies, such that if we have:
     * mod1 > imports mod2
     * mod2 > imports mod3
     * mod3 > no dependency
     * 
     * getting dependent modules on mod3 should return mod1 and mod2
     */
    public Set<String> getModulesThatHaveDependenciesOn(String moduleToFindDependents) {
        HashSet<String> dependenciesOn = new HashSet<String>();

        //used to know what will we have to look in the next round
        HashSet<String> searchOnNextRound = new HashSet<String>();
        
        HashSet<String> alreadySeached = new HashSet<String>();
        alreadySeached.add(moduleToFindDependents);//the initial has already been analyzed

        searchOnNextRound.add(moduleToFindDependents);
        
        do{
            //these are the ones we will have to go deeper on...
            HashSet<String> stillSearchOnThese = new HashSet<String>(searchOnNextRound);

            searchOnNextRound.clear();
            
            for (String dependsOn : stillSearchOnThese) {
                alreadySeached.add(dependsOn);
                
                for (Map.Entry<String,Set<String>> dependencies : this.moduleDependencies.entrySet()) {
                    
                    if(dependencies.getValue().contains(dependsOn)){
                        String key = dependencies.getKey();
                        if(alreadySeached.contains(key) == false){
                            searchOnNextRound.add(key);
                            dependenciesOn.add(key);
                        }
                    }
                }
            }
        }while(searchOnNextRound.size() > 0);
        
        return dependenciesOn;
    }

}
