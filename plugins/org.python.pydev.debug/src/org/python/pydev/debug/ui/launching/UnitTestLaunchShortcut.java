/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.core.resources.IProject;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;


public class UnitTestLaunchShortcut extends AbstractLaunchShortcut{

    protected String getLaunchConfigurationType() {
        return Constants.ID_PYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager(IProject project){
        return PydevPlugin.getPythonInterpreterManager();
    }
}
