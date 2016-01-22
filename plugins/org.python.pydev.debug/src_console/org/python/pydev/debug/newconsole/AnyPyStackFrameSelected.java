/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.shared_ui.utils.UIUtils;

/**
 * @author Fabio
 */
public class AnyPyStackFrameSelected implements IPyStackFrameProvider, IDebugContextListener {

    /**
     * By default, debug console will be linked with the selected frame
     */
    protected boolean isLinkedWithDebug = true;

    private PyStackFrame last;

    public AnyPyStackFrameSelected() {
        IWorkbenchPart activePart = UIUtils.getActivePart();
        if (activePart != null) {
            IWorkbenchPartSite site = activePart.getSite();
            DebugUITools.addPartDebugContextListener(site, this);
        }
    }

    /**
     * @return the currently selected / suspended frame. If the console is passed, it will only return
     * a frame that matches the passed console. If no selected / suspended frame is found or the console
     * doesn't match, null is returned.
     */
    public PyStackFrame getLastSelectedFrame() {
        updateContext(DebugUITools.getDebugContext());

        if (last instanceof PyStackFrame) {
            PyStackFrame stackFrame = last;
            if (!stackFrame.isTerminated() && stackFrame.isSuspended()) {
                // I.e.: can only deal with suspended contexts!
                return last;
            }
        }
        return null;
    }

    private void updateContext(IAdaptable context) {
        if (!isLinkedWithDebug && last != null) {
            return;
        }
        if (context != last && context instanceof PyStackFrame) {
            PyStackFrame stackFrame = (PyStackFrame) context;
            if (!stackFrame.isTerminated() && stackFrame.isSuspended()) {
                if (acceptsSelection(stackFrame)) {
                    last = stackFrame;
                }
            }
        }
    }

    //Subclasses may override
    protected boolean acceptsSelection(PyStackFrame stackFrame) {
        return true;
    }

    @Override
    public void debugContextChanged(DebugContextEvent event) {
        if (event.getFlags() == DebugContextEvent.ACTIVATED) {
            updateContext(getDebugContextElementForSelection(event.getContext()));
        }
    }

    private static IAdaptable getDebugContextElementForSelection(ISelection activeContext) {
        if (activeContext instanceof IStructuredSelection) {
            IStructuredSelection selection = (IStructuredSelection) activeContext;
            if (!selection.isEmpty()) {
                Object firstElement = selection.getFirstElement();
                if (firstElement instanceof IAdaptable) {
                    return (IAdaptable) firstElement;
                }
            }
        }
        return null;
    }

    /**
     * Enable/Disable linking of the debug console with the suspended frame.
     *
     * @param isLinkedWithDebug
     */
    public void linkWithDebugSelection(boolean isLinkedWithDebug) {
        this.isLinkedWithDebug = isLinkedWithDebug;
    }

}
