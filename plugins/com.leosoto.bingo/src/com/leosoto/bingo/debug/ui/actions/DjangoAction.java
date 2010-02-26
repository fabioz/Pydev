package com.leosoto.bingo.debug.ui.actions;
import org.eclipse.core.resources.IFile;import org.eclipse.debug.core.ILaunch;
import org.python.pydev.ui.actions.project.PyRemoveNature;

import com.leosoto.bingo.debug.ui.launching.PythonFileRunner;

public class DjangoAction extends PyRemoveNature {
	public ILaunch launchDjangoCommand(String command) {
		IFile manageDotPy = selectedProject.getFile(selectedProject.getName() + "/manage.py");
		try {
			return PythonFileRunner.launch(manageDotPy, command);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
