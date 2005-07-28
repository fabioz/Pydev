/*
 * Author: atotic
 * Created on Apr 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.part.FileEditorInput;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Locates source files from stack elements
 * 
 */
public class PySourceLocator implements ISourceLocator, ISourcePresentation {

	public Object getSourceElement(IStackFrame stackFrame) {
		return stackFrame;
	}

	// Returns the file
	public IEditorInput getEditorInput(Object element) {
		IEditorInput edInput = null;
		if (element instanceof PyStackFrame) 
		{
			IPath path = ((PyStackFrame)element).getPath();
			if (path != null && !path.toString().startsWith("<")) 
			{
		        IWorkspace w = ResourcesPlugin.getWorkspace();		        
		        IFile file = w.getRoot().getFileForLocation(path);
		        if (file == null  || !file.exists())
		        	file = PydevPlugin.linkToExternalFile(path);
		        edInput = new FileEditorInput(file);
			}
		}
		return edInput;
	}

	public String getEditorId(IEditorInput input, Object element) {
		return PyEdit.EDITOR_ID;
	}

}
