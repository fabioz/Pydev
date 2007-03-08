package org.python.pydev.plugin.nature;

import java.io.File;

import org.eclipse.core.resources.IResource;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.REF;
import org.python.pydev.plugin.PydevPlugin;

public abstract class AbstractPythonNature implements IPythonNature{

    /**
     * @param resource the resource we want info on
     * @return whether the passed resource is in the pythonpath or not (it must be in a source folder for that).
     */
    public boolean isResourceInPythonpath(IResource resource) {
    	return resolveModule(resource) != null; 
    }
    
    public boolean isResourceInPythonpath(String absPath) {
    	return resolveModule(absPath) != null; 
    }

	/**
     * @param resource the resource we want to get the name from
     * @return the name of the module in the environment
     */
    public String resolveModule(IResource resource) {
    	return resolveModule(PydevPlugin.getIResourceOSString(resource));
    }
    
    public String resolveModule(File file) {
    	return resolveModule(REF.getFileAbsolutePath(file));
    }
    
	public boolean startRequests() {
		ICodeCompletionASTManager astManager = this.getAstManager();
		if(astManager == null){
			return false;
		}
		return astManager.getModulesManager().startCompletionCache();
	}
	
	public void endRequests() {
		this.getAstManager().getModulesManager().endCompletionCache();
	}

}
