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
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;
import org.python.pydev.shared_core.log.Log;

public class VariablesLoader implements ICommandResponseListener {

    private volatile PyVariable[] currentVariables;
    private volatile PyVariable[] oldVariables;
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

    public PyVariable[] fetchVariables() {
        oldVariables = currentVariables;
        currentVariables = null;
        AbstractDebugTarget target = this.getTarget();
        if (target == null) {
            return new PyVariable[0];
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
        PyVariable[] temp = PyVariableCollection.getCommandVariables(cmd, target, locator);

        if (addGlobalsVariable) {
            PyVariable[] temp1 = new PyVariable[temp.length + 1];
            System.arraycopy(temp, 0, temp1, 1, temp.length);
            temp1[0] = new PyVariableCollection(target, "Globals", "frame.f_globals", "Global variables",
                    this.parent.getGlobalLocator(), "");
            temp = temp1;
        }

        PyVariable[] newVars = this.verifyVariablesModified(temp, oldVariables);

        currentVariables = parent.setVariables(newVars);
    }

    /**
     * Compares stack frames to check for modified variables (and mark them as modified in the new stack).
     * Tries to reuse variables from the old list so that the tree state is kept on the variables view.
     *
     * @returns the list to be used for the frame (which uses old variables when possible if they're compatible,
     * even updating them in-place as needed).
     */
    private PyVariable[] verifyVariablesModified(PyVariable[] newFrameVariables, PyVariable[] oldVariables) {
        if (oldVariables == null || newFrameVariables == oldVariables) {
            return newFrameVariables; //All variables are new, so, no point in notifying it.
        }
        ArrayList<PyVariable> newVarsList = new ArrayList<>(newFrameVariables.length);

        PyVariable newVariable = null;

        try {
            Map<String, PyVariable> map = new HashMap<String, PyVariable>();
            for (PyVariable var : oldVariables) {
                String uniqueId = var.getUniqueId();
                if (uniqueId != null) {
                    map.put(uniqueId, var);
                }
            }
            Map<String, PyVariable> variablesAsMap = map;

            //we have to check for each new variable
            for (int i = 0; i < newFrameVariables.length; i++) {
                newVariable = newFrameVariables[i];

                String uniqueId = newVariable.getUniqueId();
                if (uniqueId != null) {
                    PyVariable oldVariable = variablesAsMap.get(uniqueId);

                    if (oldVariable != null) {
                        boolean equals;
                        if (newVariable.getClass() != oldVariable.getClass()) {
                            // Changed from collection to simple var (or vice-versa).
                            // If it's a new variable, we don't need to force it to get new variables
                            // (this will happen naturally when requested).
                            newVariable.setModified(true);
                            newVarsList.add(newVariable);
                        } else {
                            // Same class: always use old variable (and set the value string accordingly)
                            String newValueString = newVariable.getValueString();
                            equals = newValueString.equals(oldVariable.getValueString());
                            if (!equals) {
                                oldVariable.copyValueString(newVariable);
                            }
                            // If it is not equal, it was modified.
                            oldVariable.setModified(!equals);
                            // Always force an existing variable collection to get new variables.
                            oldVariable.forceGetNewVariables();
                            newVarsList.add(oldVariable);
                        }

                    } else { // It didn't exist before...
                        newVariable.setModified(true);
                        newVarsList.add(newVariable);
                    }
                } else {
                    // no pyDBLocation, just add as is
                    newVarsList.add(newVariable);
                }
            }

        } catch (DebugException e) {
            Log.log(e);
        }
        return newVarsList.toArray(new PyVariable[0]);
    }
}
