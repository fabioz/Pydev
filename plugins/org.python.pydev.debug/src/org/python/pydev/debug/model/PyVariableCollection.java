/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on May 4, 2004
 */
package org.python.pydev.debug.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;

/**
 * PyVariableCollection represents container variables.
 *
 * It knows how to fetch its contents over the network.
 *
 */
public class PyVariableCollection extends PyVariable implements IVariablesContainerParent {

    private final ContainerOfVariables variableContainer = new ContainerOfVariables(this, false);

    public PyVariableCollection(AbstractDebugTarget target, String name, String type, String value,
            IVariableLocator locator, String scope) {
        super(target, name, type, value, locator, scope);
    }

    @Override
    public String getDetailText() throws DebugException {
        return super.getDetailText();
    }

    public static final String SCOPE_SPECIAL_VARS = "special variables";
    public static final String SCOPE_PROTECTED_VARS = "protected variables";
    public static final String SCOPE_CLASS_VARS = "class variables";
    public static final String SCOPE_FUNCTION_VARS = "function variables";

    private static final String[] SCOPES_SORTED_REVERSED = new String[] {
            SCOPE_FUNCTION_VARS,
            SCOPE_CLASS_VARS,
            SCOPE_PROTECTED_VARS,
            SCOPE_SPECIAL_VARS,
    };

    /**
     * @return a list of variables resolved for some command
     */
    public static PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd, AbstractDebugTarget target,
            IVariableLocator locator) {
        PyVariable[] tempVariables = new PyVariable[0];
        try {
            String payload = ((GetVariableCommand) cmd).getResponse();
            tempVariables = XMLUtils.XMLToVariables(target, locator, payload);

            // Ok, now that we have the variables, group them based on the scope.
            Map<String, List<PyVariable>> scopedVars = new HashMap<>();
            List<PyVariable> otherVars = new ArrayList<PyVariable>(tempVariables.length);
            for (PyVariable v : tempVariables) {
                if (v.scope != null && v.scope.length() > 0) {
                    List<PyVariable> list = scopedVars.get(v.scope);
                    if (list == null) {
                        list = new ArrayList<>();
                        scopedVars.put(v.scope, list);
                    }
                    list.add(v);
                } else {
                    otherVars.add(v);
                }
            }

            if (!scopedVars.isEmpty()) {
                for (String scope : SCOPES_SORTED_REVERSED) {
                    List<PyVariable> list = scopedVars.remove(scope);
                    if (list != null && list.size() > 0) {
                        PyVariableGroup element = new PyVariableGroup(target, scope, "", "", locator.getThreadId(), "",
                                locator.getUniqueId());
                        element.setVariables(list);
                        otherVars.add(0, element);
                    }
                }
                // Add any remainder group (shouldn't really happen).
                for (Entry<String, List<PyVariable>> entry : scopedVars.entrySet()) {
                    Log.log("Unexpected scope: " + entry.getKey());
                    List<PyVariable> list = entry.getValue();
                    PyVariableGroup element = new PyVariableGroup(target, entry.getKey(), "", "", locator.getThreadId(),
                            "", locator.getUniqueId());
                    element.setVariables(list);
                    otherVars.add(0, element);

                }
                tempVariables = otherVars.toArray(new PyVariable[0]);
            }

            // if no variable was added, don't change tempVariables.
        } catch (CoreException e) {
            tempVariables = new PyVariable[1];
            tempVariables[0] = new PyVariable(target, "Error", "pydev ERROR", "Could not resolve variable", locator,
                    "");

            String msg = e.getMessage(); //we don't want to show this error
            if (msg == null || (msg.indexOf("Error resolving frame:") == -1 && msg.indexOf("from thread:") == -1)) {
                PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
            }
        }
        return tempVariables;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return this.variableContainer.getVariables();
    }

    @Override
    public GetVariableCommand getVariableCommand(AbstractDebugTarget dbg) {
        return new GetVariableCommand(dbg, getPyDBLocation());
    }

    @Override
    public void forceGetNewVariables() {
        this.variableContainer.forceGetNewVariables();
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return true;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return type;
    }

    @Override
    public AbstractDebugTarget getTarget() {
        return target;
    }

    @Override
    public IVariableLocator getGlobalLocator() {
        return null;
    }

}
