/*
 * Author: atotic
 * Created on Mar 11, 2004
 * License: Common Public License v1.0
 */
 
package org.python.pydev.plugin;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * PythonNature is currently used as a marker class.
 * 
 * When python nature is present, project gets extra properties.
 * Project gets assigned python nature when:
 *   - a python file is edited
 *   - a python project wizard is created
 */
public class PythonNature implements IProjectNature {

	public static final String PYTHON_NATURE_ID = "org.python.pydev.pythonNature";
	public static final String PYTHON_NATURE_NAME = "pythonNature";

	IProject project;
	
	public void configure() throws CoreException {
	}

	public void deconfigure() throws CoreException {
	}

	public IProject getProject() {
		return project;
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	
	/**
	 * Utility routine to add PythonNature to the project
	 */
	public static void addNature(IProject project, IProgressMonitor monitor) throws CoreException {
		if (project == null || project.hasNature(PYTHON_NATURE_ID))
			return;
		IProjectDescription desc = project.getDescription();
		String[] natures = desc.getNatureIds();
		String[] newNatures = new String[natures.length + 1];
		System.arraycopy(natures, 0, newNatures, 0, natures.length);
		newNatures[natures.length] = PYTHON_NATURE_ID;
		desc.setNatureIds(newNatures);
		project.setDescription(desc, monitor);
	}
}
