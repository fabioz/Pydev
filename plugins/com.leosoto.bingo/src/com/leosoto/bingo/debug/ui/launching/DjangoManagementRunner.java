package com.leosoto.bingo.debug.ui.launching;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunch;

public class DjangoManagementRunner {
	public static ILaunch launch(IProject project, String command) throws CoreException {
		IFile manageDotPy = project.getFile(project.getName() + "/manage.py");
		return PythonFileRunner.launch(manageDotPy, command);		
	}
}
