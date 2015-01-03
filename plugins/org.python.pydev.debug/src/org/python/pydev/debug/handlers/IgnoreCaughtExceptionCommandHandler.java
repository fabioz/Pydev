/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;
import org.python.pydev.debug.model.PyStackFrame;

public class IgnoreCaughtExceptionCommandHandler extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (selection instanceof StructuredSelection) {
            StructuredSelection structuredSelection = (StructuredSelection) selection;
            Object elem = structuredSelection.getFirstElement();
            if (elem instanceof IAdaptable) {
                IAdaptable iAdaptable = (IAdaptable) elem;
                elem = iAdaptable.getAdapter(IStackFrame.class);

            }
            if (elem instanceof PyStackFrame) {
                try {
                    PyStackFrame pyStackFrame = (PyStackFrame) elem;
                    IPath path = pyStackFrame.getPath();
                    int lineNumber = pyStackFrame.getLineNumber();
                    PyExceptionBreakPointManager.getInstance().ignoreCaughtExceptionsWhenThrownFrom
                            .addIgnoreThrownExceptionIn(path.toFile(), lineNumber);
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
        }

        return null;
    }

}
