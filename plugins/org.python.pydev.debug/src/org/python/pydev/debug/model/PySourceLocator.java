/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 23, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.ISourcePresentation;
import org.eclipse.ui.IEditorInput;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editorinput.PySourceLocatorBase;

/**
 * Locates source files from stack elements
 * 
 */
public class PySourceLocator implements ISourceLocator, ISourcePresentation {

    private PySourceLocatorBase locatorBase = new PySourceLocatorBase();

    public Object getSourceElement(IStackFrame stackFrame) {
        return stackFrame;
    }

    private IProject lastProject = null;

    // Returns the file
    public IEditorInput getEditorInput(Object element) {
        IEditorInput edInput = null;
        if (element instanceof PyStackFrame) {
            PyStackFrame pyStackFrame = (PyStackFrame) element;
            IPath path = pyStackFrame.getPath();

            // get the project of the file that is being debugged
            Object target = pyStackFrame.getAdapter(IDebugTarget.class);
            if (target instanceof PyDebugTarget) {
                lastProject = ((PyDebugTarget) target).project;
            }

            if (path != null && !path.toString().startsWith("<")) {
                edInput = locatorBase.createEditorInput(path, true, pyStackFrame, lastProject);
            }

        }
        return edInput;
    }

    public String getEditorId(IEditorInput input, Object element) {
        return PyEdit.EDITOR_ID;
    }

}
