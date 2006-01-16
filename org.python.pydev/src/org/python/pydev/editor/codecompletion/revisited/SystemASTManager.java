package org.python.pydev.editor.codecompletion.revisited;

import java.io.File;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.ui.interpreters.IInterpreterManager;

public class SystemASTManager extends AbstractASTManager{
	
	private IInterpreterManager manager;

	public SystemASTManager(IInterpreterManager manager, IPythonNature nature) {
		this.manager = manager;
		this.modulesManager = this.manager.getDefaultInterpreterInfo(new NullProgressMonitor()).modulesManager;
		setNature(nature);
	}

	public void changePythonPath(String pythonpath, IProject project, IProgressMonitor monitor) {
		throw new RuntimeException("Not implemented");
	}

	public void setProject(IProject project, boolean restoreDeltas) {
		throw new RuntimeException("Not implemented");
	}

	public void rebuildModule(File file, IDocument doc, IProject project, IProgressMonitor monitor, IPythonNature nature) {
		throw new RuntimeException("Not implemented");
	}

	public void removeModule(File file, IProject project, IProgressMonitor monitor) {
		throw new RuntimeException("Not implemented");
	}

	public void validatePathInfo(String pythonpath, IProject project, IProgressMonitor monitor) {
		throw new RuntimeException("Not implemented");
	}

}
