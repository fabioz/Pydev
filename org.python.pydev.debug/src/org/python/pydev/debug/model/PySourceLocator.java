/*
 * Author: atotic
 * Created on Apr 23, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
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

	public IEditorInput getEditorInput(Object element) {
		if (element instanceof PyStackFrame) {
			IPath path = ((PyStackFrame)element).getPath();
			if (path != null) {
				IEditorPart part = PydevPlugin.doOpenEditor(path, false);
				if (part != null) {
					return part.getEditorInput();
				}
			}
		}
		return null;
	}

	public String getEditorId(IEditorInput input, Object element) {
		return PyEdit.EDITOR_ID;
	}

}
