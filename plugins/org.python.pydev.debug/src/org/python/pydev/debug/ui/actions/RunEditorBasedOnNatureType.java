package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.plugin.PydevPlugin;

public class RunEditorBasedOnNatureType extends PyAction{

    public void run(IAction action) {
        String launchConfigurationType;
        String defaultType = Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
        IInterpreterManager interpreterManager = PydevPlugin.getPythonInterpreterManager();
        
        PyEdit pyEdit = getPyEdit();
        try{
            IPythonNature nature = pyEdit.getPythonNature();
            if(nature == null){
                launchConfigurationType = defaultType;
            }else{
                int interpreterType = nature.getInterpreterType();
                interpreterManager = nature.getRelatedInterpreterManager();
                switch(interpreterType){
                    case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                        launchConfigurationType = Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
                        break;
                    case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                        launchConfigurationType = Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
                        break;
                    case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                        launchConfigurationType = Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE;
                        break;
                    default:
                        throw new RuntimeException("Cannot recognize type: "+interpreterType);
                }
            }
        }catch(Exception e){
            Log.log(IStatus.INFO, "Problem determining nature type. Using regular python launch.", e);
            launchConfigurationType = defaultType;
        }
        
        
        final String finalLaunchConfigurationType = launchConfigurationType; 
        final IInterpreterManager finalInterpreterManager = interpreterManager;
        AbstractLaunchShortcut shortcut = new AbstractLaunchShortcut(){

            @Override
            protected String getLaunchConfigurationType(){
                return finalLaunchConfigurationType;
            }
            
            @Override
            protected IInterpreterManager getInterpreterManager(){
                return finalInterpreterManager;
            }
            
        };
        shortcut.launch(pyEdit, "run");
    }

}
