/*
 * Created on 07/12/2005
 */
package com.python.pydev.analysis.additionalinfo.dependencies;

import java.util.HashSet;
import java.util.Set;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.DepInfo;

/**
 * This class calculates dependencies for a given change.
 * 
 * The dependencies must be calculated for changes and not for modules because as python
 * can have really dirty namespaces, when calculating 'deep' dependencies for a module,
 * too many modules may be dependent on it, whereas, for only a change (like adding or removing 
 * a class, import or module, this can have as a result much fewer modules).
 * 
 * As we are calculating dependencies for a change, we need as inputs:
 * - The changed module
 * - The tokens that were added and from which module those tokens are 
 * - The tokens that were removed and from which module those tokens are
 * 
 * Note: if a 'root' token is removed, its nested elements do not need to be added to the list, since we will have
 * the errors gathered from getting there in the first place.  
 * 
 * And as outputs, we need:
 * - The modules to be re-analyzed
 * 
 * @author Fabio
 */
public class DependencyCalculator {
    
    private AbstractAdditionalDependencyInfo info;

    public DependencyCalculator(AbstractAdditionalDependencyInfo info) {
        this.info = info;
    }

    /**
     * To calculate the dependencies, we need to go through the following steps:
     * 
     * 1. Get all the modules that might be dependent on this module (deep dependency), so that:
     *      1.1. All the modules that have a direct regular import from this module are gotten
     *      1.2. All the modules that have a direct wild import from this module are gotten
     *          1.2.1. For each module that makes a wild import from the found module, gets its dependencies too
     * 
     * 2. For each module, check if it uses some token that was added or removed. Those tokens may have been created either by:
     *      2.1. Some declaration (class, method, attr...)
     *      2.2. Some 'regular' import
     *      2.3. Some wild import
     * 
     * 3. If a module was added or removed, just add the name of the module to the names of the tokens
     * 
     * @param change This is the change upon where we should calculate the dependencies
     * @return a list of strings with the name of the modules that should be re-analyzed 
     */
    public HashSet<String> calculateDependencies(PyStructuralChange change){
        HashSet<String> mods = new HashSet<String>();
        
        Set<String> changedTokens = change.getChangedTokens();
        //for each changed token
        for (String changedToken : changedTokens) {
            
            Set<DepInfo> deppies = info.depInfo.get(changedToken);
            
            //analyze the modules that may use it
            for (DepInfo depInfo : deppies) {
                if(depInfo.isFromWildImport() && info.hasWildImportPath(depInfo.moduleName, change.getModule())){
                    mods.add(depInfo.moduleName);
                    
                } else if(depInfo.importsFrom.equals(change.getModule())){
                    mods.add(depInfo.moduleName);
                }
            }
        }
        return mods;
    }
}
