/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.shared_core.log.Log;

public class VariablesLoader implements ICommandResponseListener {

    private volatile IVariable[] currentVariables;
    private volatile IVariable[] oldVariables;
    private final ContainerOfVariables parent;
    private IProgressMonitor monitor;
    private boolean addGlobalsVariable;

    public VariablesLoader(ContainerOfVariables parent, boolean addGlobalsVariable) {
        this.parent = parent;
        this.addGlobalsVariable = addGlobalsVariable;
    }

    private AbstractDebugTarget getTarget() {
        return this.parent.getTarget();
    }

    private IVariableLocator getLocator() {
        return this.parent.getLocator();
    }

    public IVariable[] fetchVariables() {
        oldVariables = currentVariables;
        currentVariables = null;
        AbstractDebugTarget target = this.getTarget();
        if (target == null) {
            return new IVariable[0];
        }
        GetVariableCommand variableCommand = this.parent.getVariableCommand(target);
        variableCommand.setCompletionListener(this);
        target.postCommand(variableCommand);
        return waitForCommand();
    }

    private IVariable[] waitForCommand() {
        try {
            // VariablesView does not deal well with children changing asynchronously.
            // it causes unneeded scrolling, because view preserves selection instead
            // of visibility.
            // I try to minimize the occurrence here, by giving pydevd time to complete the
            // task before we are forced to do asynchronous notification.
            int i = 150; //up to 1.5 seconds
            while (--i > 0 && currentVariables == null) {
                if (this.monitor != null && this.monitor.isCanceled() == true) {
                    //canceled request... let's return
                    return new PyVariable[0];
                }
                Thread.sleep(10); //10 millis
            }
        } catch (InterruptedException e) {
            Log.log(e);
        }

        if (currentVariables != null) {
            return currentVariables;
        }
        return new PyVariable[0];
    }

    @Override
    public void commandComplete(AbstractDebuggerCommand cmd) {
        AbstractDebugTarget target = getTarget();
        IVariableLocator locator = getLocator();
        if (target == null || locator == null) {
            return;
        }
        IVariable[] temp = PyVariableCollection.getCommandVariables(cmd, target, locator);

        if (addGlobalsVariable) {
            PyVariable[] temp1 = new PyVariable[temp.length + 1];
            System.arraycopy(temp, 0, temp1, 1, temp.length);
            temp1[0] = new PyVariableCollection(target, "Globals", "frame.f_globals", "Global variables",
                    this.parent.getGlobalLocator());
            temp = temp1;
        }

        IVariable[] newVars = this.verifyVariablesModified(temp, oldVariables);

        currentVariables = parent.setVariables(newVars);
    }

    /**
     * compares stack frames to check for modified variables (and mark them as modified in the new stack)
     * 
     * @param newFrame the new frame
     * @param oldFrame the old frame
     * @return 
     */
    private IVariable[] verifyVariablesModified(IVariable[] newFrameVariables, IVariable[] oldVariables) {
        if (oldVariables == null || newFrameVariables == oldVariables) {
            return newFrameVariables; //All variables are new, so, no point in notifying it.
        }
        ArrayList<IVariable> newVarsList = new ArrayList<>(newFrameVariables.length);

        PyVariable newVariable = null;

        try {
            Map<String, IVariable> map = new HashMap<String, IVariable>();
            for (IVariable var : oldVariables) {
                map.put(var.getName(), var);
            }
            Map<String, IVariable> variablesAsMap = map;

            //we have to check for each new variable
            for (int i = 0; i < newFrameVariables.length; i++) {
                newVariable = (PyVariable) newFrameVariables[i];

                PyVariable oldVariable = (PyVariable) variablesAsMap.get(newVariable.getName());

                if (oldVariable != null) {
                    boolean equals;
                    if (newVariable.getClass() != oldVariable.getClass()) {
                        equals = false;
                    } else {
                        // Same class
                        equals = newVariable.getValueString().equals(oldVariable.getValueString());
                    }

                    //if it is not equal, it was modified
                    newVariable.setModified(!equals);

                    if (equals) {
                        //at this point, force the variable collection to get its own new contents
                        //if it already existed as those may be old.
                        oldVariable.forceGetNewVariables();
                        newVarsList.add(oldVariable);
                    } else {
                        //if it's a new variable, we don't need to force it to get new variables
                        //(this will happen naturally when requested).
                        newVarsList.add(newVariable);
                    }

                } else { //it didn't exist before...
                    newVariable.setModified(true);
                    newVarsList.add(newVariable);
                }
            }

        } catch (DebugException e) {
            Log.log(e);
        }
        return newVarsList.toArray(new IVariable[0]);
    }
}
