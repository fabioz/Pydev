/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.ui.DeferredDebugElementWorkbenchAdapter;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;


public class DeferredWorkbenchAdapter extends DeferredDebugElementWorkbenchAdapter implements
        IDeferredWorkbenchAdapter, ICommandResponseListener {

    private volatile PyVariable[] commandVariables;
    private AbstractDebugTarget target;
    private IVariableLocator locator;
    private final Object parent;
    private IProgressMonitor monitor;

    public DeferredWorkbenchAdapter(Object parent) {
        this.parent = parent;
    }

    @Override
    public boolean isContainer() {
        if (parent instanceof PyVariableCollection) {
            return true;
        } else if (parent instanceof PyStackFrame) {
            return true;
        }
        return false;
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter#fetchDeferredChildren(java.lang.Object, org.eclipse.ui.progress.IElementCollector, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void fetchDeferredChildren(Object object, IElementCollector collector, IProgressMonitor monitor) {
        this.monitor = monitor;
        if (monitor.isCanceled()) {
            return;
        }
        Object[] children = getChildren(object);
        if (monitor.isCanceled()) {
            return;
        }
        if (children != null && children.length > 0) {
            collector.add(children, monitor);
        }
        collector.done();
    }

    public Object[] getChildren(Object o) {
        if (parent != o) {
            throw new RuntimeException("This is valid only for a single getChildren!");
        }

        if (o instanceof PyVariableCollection) {
            PyVariableCollection variableCollection = (PyVariableCollection) o;

            target = variableCollection.getTarget();
            if (target != null) {
                locator = variableCollection;

                GetVariableCommand variableCommand = variableCollection.getVariableCommand(target);
                variableCommand.setCompletionListener(this);
                target.postCommand(variableCommand);
                return waitForCommand();
            }
            return new Object[0];

        } else if (o instanceof PyStackFrame) {
            PyStackFrame f = (PyStackFrame) o;

            target = f.getTarget();
            if (target != null) {
                locator = f;

                GetVariableCommand variableCommand = f.getFrameCommand(target);
                variableCommand.setCompletionListener(this);
                target.postCommand(variableCommand);
                return waitForCommand();
            }
            return new Object[0];

        } else if (o instanceof PyVariable) {
            return new Object[0];

        } else {
            throw new RuntimeException("Unexpected class: " + o.getClass());
        }
    }

    private PyVariable[] waitForCommand() {
        try {
            // VariablesView does not deal well with children changing asynchronously.
            // it causes unneeded scrolling, because view preserves selection instead
            // of visibility.
            // I try to minimize the occurrence here, by giving pydevd time to complete the
            // task before we are forced to do asynchronous notification.
            int i = 500; //up to 5 seconds
            while (--i > 0 && commandVariables == null) {
                if (this.monitor != null && this.monitor.isCanceled() == true) {
                    //canceled request... let's return
                    return new PyVariable[0];
                }
                Thread.sleep(10); //10 millis
            }
        } catch (InterruptedException e) {
            Log.log(e);
        }

        if (commandVariables != null) {
            return commandVariables;
        }
        return new PyVariable[0];
    }

    public Object getParent(Object o) {
        //do we really need that?
        return parent;
    }

    public void commandComplete(AbstractDebuggerCommand cmd) {
        PyVariable[] temp = PyVariableCollection.getCommandVariables(cmd, target, locator);
        if (parent instanceof PyVariableCollection) {
            commandVariables = temp;

        } else if (parent instanceof PyStackFrame) {
            PyStackFrame f = (PyStackFrame) parent;
            PyVariable[] temp1 = new PyVariable[temp.length + 1];
            System.arraycopy(temp, 0, temp1, 1, temp.length);
            temp1[0] = new PyVariableCollection(target, "Globals", "frame.f_globals", "Global variables",
                    f.getGlobalLocator());
            commandVariables = temp1;

        } else {
            throw new RuntimeException("Unknown parent:" + parent.getClass());
        }
    }

}
