/*
 * Author: atotic
 * Created: Aug 16, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.ui.launching;

import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;

public class CoverageLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate{
    /**
     * @return
     */
    protected String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch) {
        return PythonRunnerConfig.RUN_COVERAGE;
    }

}
