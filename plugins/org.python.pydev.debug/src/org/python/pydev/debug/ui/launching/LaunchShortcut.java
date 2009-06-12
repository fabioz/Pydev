/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;


public class LaunchShortcut extends AbstractLaunchShortcut {

    protected String getLaunchConfigurationType() {
        return Constants.ID_PYTHON_REGULAR_LAUNCH_CONFIGURATION_TYPE;
    }
    
    
    @Override
    protected boolean getRequireFile(){
        return true;
    }

    
    @Override
    protected IInterpreterManager getInterpreterManager(){
        return PydevPlugin.getPythonInterpreterManager();
    }
}
