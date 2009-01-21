/*
 * Author: atotic
 * Created: Aug 26, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;


public class JythonUnitTestLaunchShortcut extends AbstractLaunchShortcut{

    protected String getLaunchConfigurationType() {
        return Constants.ID_JYTHON_UNITTEST_LAUNCH_CONFIGURATION_TYPE;
    }
    

    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager();
    }

}
