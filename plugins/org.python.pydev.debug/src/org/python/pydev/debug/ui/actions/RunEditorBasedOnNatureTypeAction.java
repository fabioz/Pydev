package org.python.pydev.debug.ui.actions;

import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.editor.PyEdit;

public class RunEditorBasedOnNatureTypeAction extends AbstractRunEditorAction{

    public void run(IAction action) {
        
        PyEdit pyEdit = getPyEdit();
        final Tuple<String, IInterpreterManager> launchConfigurationTypeAndInterpreterManager = 
            this.getLaunchConfigurationTypeAndInterpreterManager(pyEdit, false);
        
        AbstractLaunchShortcut shortcut = new AbstractLaunchShortcut(){

            @Override
            protected String getLaunchConfigurationType(){
                return launchConfigurationTypeAndInterpreterManager.o1;
            }
            
            @Override
            protected IInterpreterManager getInterpreterManager(){
                return launchConfigurationTypeAndInterpreterManager.o2;
            }
            
        };
        shortcut.launch(pyEdit, "run");
    }

}
