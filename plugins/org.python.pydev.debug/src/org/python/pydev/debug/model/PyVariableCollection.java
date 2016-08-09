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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;

/**
 * PyVariableCollection represents container variables.
 * 
 * It knows how to fetch its contents over the network.
 * 
 */
public class PyVariableCollection extends PyVariable
        implements IVariableLocator, IVariablesContainerParent {

    private final ContainerOfVariables variableContainer = new ContainerOfVariables(this, false);

    /**
     * Defines whether object is variable or watchExpression
     */
    boolean isWatchExpression = false;

    public PyVariableCollection(AbstractDebugTarget target, String name, String type, String value,
            IVariableLocator locator) {
        super(target, name, type, value, locator);
    }

    @Override
    public String getDetailText() throws DebugException {
        return super.getDetailText();
    }

    public PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd) {
        return getCommandVariables(cmd, target, this);
    }

    /**
     * @return a list of variables resolved for some command
     */
    public static PyVariable[] getCommandVariables(AbstractDebuggerCommand cmd, AbstractDebugTarget target,
            IVariableLocator locator) {
        PyVariable[] tempVariables = new PyVariable[0];
        try {
            String payload = ((GetVariableCommand) cmd).getResponse();
            tempVariables = XMLUtils.XMLToVariables(target, locator, payload);
        } catch (CoreException e) {
            tempVariables = new PyVariable[1];
            tempVariables[0] = new PyVariable(target, "Error", "pydev ERROR", "Could not resolve variable", locator);

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
