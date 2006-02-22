/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui.launching;

import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.core.Constants;
import org.python.pydev.plugin.PydevPlugin;

public class JythonLaunchShortcut extends AbstractLaunchShortcut{

    @Override
    protected String getLaunchConfigurationType() {
        return Constants.ID_JYTHON_LAUNCH_CONFIGURATION_TYPE;
    }
    
    @Override
    protected IInterpreterManager getInterpreterManager() {
        return PydevPlugin.getJythonInterpreterManager();
    }


}
