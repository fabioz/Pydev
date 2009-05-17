package org.python.pydev.customizations.app_engine.actions;

import java.io.File;
import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.customizations.app_engine.launching.AppEngineConstants;
import org.python.pydev.customizations.app_engine.util.GoogleAppEngineUtil;
import org.python.pydev.editor.actions.PyAction;

/**
 * Abstract class for classes that are executed based on the app engine / target folder.
 */
public abstract class AbstractAppEngineHandler extends AbstractHandler{

    
    public Object execute(ExecutionEvent event) throws ExecutionException{
        ISelection sel = HandlerUtil.getCurrentSelectionChecked(event);
        if(sel instanceof IStructuredSelection){
            IStructuredSelection selection = (IStructuredSelection) sel;
            Object firstElement = selection.getFirstElement();
            
            IContainer container = GoogleAppEngineUtil.getContainerFromObject(firstElement);
            if(container == null){
                return null;
            }
            
            IPythonPathNature pythonPathNature = GoogleAppEngineUtil.getPythonPathNatureFromObject(firstElement);
            if(pythonPathNature == null){
                return null;
            }
            
            
            Map<String, String> variableSubstitution;
            try{
                variableSubstitution = pythonPathNature.getVariableSubstitution();
                //Only consider a google app engine a project that has a google app engine variable!
                if(variableSubstitution == null || !variableSubstitution.containsKey(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE)){
                    return null;
                }
                
                File appEngineLocation = new File(variableSubstitution.get(AppEngineConstants.GOOGLE_APP_ENGINE_VARIABLE));
                if(!appEngineLocation.isDirectory()){
                    MessageDialog.openError(PyAction.getShell(), "Error", "Expected: "+appEngineLocation+" to be a directory.");
                    return null;
                }
                
                File appcfg = new File(appEngineLocation, "appcfg.py");
                if(!appcfg.isFile()){
                    MessageDialog.openError(PyAction.getShell(), "Error", "Expected: "+appcfg+" to be a file.");
                    return null;
                }
                
                handleExecution(container, pythonPathNature, appcfg, appEngineLocation);
                
            }catch(CoreException e){
                Log.log(e);
            }

        }
        return null;
    }

    /**
     * Subclasses should override this method to properly handle the execution of the action (this is called when
     * all things are already validated).
     */
    protected abstract void handleExecution(IContainer container, IPythonPathNature pythonPathNature, File appcfg, File appEngineLocation);

}
