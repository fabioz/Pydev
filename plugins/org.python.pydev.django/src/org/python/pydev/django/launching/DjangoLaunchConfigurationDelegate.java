package org.python.pydev.django.launching;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.ui.launching.AbstractLaunchConfigurationDelegate;
import org.python.pydev.debug.ui.launching.PythonRunnerConfig;
import org.python.pydev.plugin.nature.PythonNature;

/**
 * It's the same as a regular run (we just config things a bit different)
 * 
 * @author Fabio
 */
public class DjangoLaunchConfigurationDelegate extends AbstractLaunchConfigurationDelegate{

	protected String getRunnerConfigRun(ILaunchConfiguration conf, String mode, ILaunch launch) {
		try {
			IProject project = PythonRunnerConfig.getProjectFromConfiguration(conf);
			PythonNature nature = PythonNature.getPythonNature(project);
			if(nature != null){
	            switch(nature.getInterpreterType()){
	                case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
	                    return PythonRunnerConfig.RUN_JYTHON;
	                case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
	                    return PythonRunnerConfig.RUN_REGULAR;
	                case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
	                	return PythonRunnerConfig.RUN_IRONPYTHON;
	            }
	            throw new RuntimeException("Unable to get the run configuration for interpreter type: "+nature.getInterpreterType());
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		throw new RuntimeException("Unable to get the run configuration");
	}

}
