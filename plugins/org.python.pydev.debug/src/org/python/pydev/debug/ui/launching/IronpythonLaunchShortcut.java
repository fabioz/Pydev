package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;


public class IronpythonLaunchShortcut extends AbstractLaunchShortcut {

    protected String getLaunchConfigurationType() {
        return Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager(IProject project) {
        return PydevPlugin.getIronpythonInterpreterManager();
    }

    @Override
    protected boolean getRequireFile(){
        return true;
    }
    
}
