/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 14/08/2005
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public class JythonLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate {

    @Override
    protected String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch) {
        return PythonRunnerConfig.RUN_JYTHON;
    }

}
