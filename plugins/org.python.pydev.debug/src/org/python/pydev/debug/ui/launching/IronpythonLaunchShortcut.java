package org.python.pydev.debug.ui.launching;

import org.python.pydev.debug.core.Constants;


public class IronpythonLaunchShortcut extends AbstractLaunchShortcut {

    protected String getLaunchConfigurationType() {
        return Constants.ID_IRONPYTHON_LAUNCH_CONFIGURATION_TYPE;
    }
}
