/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import org.eclipse.core.runtime.IProgressMonitor;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.shared_core.log.Log;

public class DeferredWorkbenchAdapter implements ICommandResponseListener {

    private volatile PyVariable[] commandVariables;
    private AbstractDebugTarget target;
    private IVariableLocator locator;
    private final ContainerOfVariables parent;
    private IProgressMonitor monitor;
    private boolean addGlobalsVariable;

    public DeferredWorkbenchAdapter(ContainerOfVariables parent, boolean addGlobalsVariable) {
        this.parent = parent;
        this.addGlobalsVariable = addGlobalsVariable;
        this.target = this.parent.getTarget();
        this.locator = this.parent.getLocator();
    }

    public Object[] getChildren() {
        if (this.target == null) {
            return new Object[0];
        }
        GetVariableCommand variableCommand = this.parent.getVariableCommand(target);
        variableCommand.setCompletionListener(this);
        target.postCommand(variableCommand);
        return waitForCommand();
    }

    private PyVariable[] waitForCommand() {
        try {
            // VariablesView does not deal well with children changing asynchronously.
            // it causes unneeded scrolling, because view preserves selection instead
            // of visibility.
            // I try to minimize the occurrence here, by giving pydevd time to complete the
            // task before we are forced to do asynchronous notification.
            int i = 150; //up to 1.5 seconds
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

    @Override
    public void commandComplete(AbstractDebuggerCommand cmd) {
        PyVariable[] temp = PyVariableCollection.getCommandVariables(cmd, target, locator);

        if (addGlobalsVariable) {
            PyVariable[] temp1 = new PyVariable[temp.length + 1];
            System.arraycopy(temp, 0, temp1, 1, temp.length);
            temp1[0] = new PyVariableCollection(target, "Globals", "frame.f_globals", "Global variables",
                    this.parent.getGlobalLocator());
            commandVariables = temp1;
            parent.setVariables(commandVariables);
        }
    }

}
