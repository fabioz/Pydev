/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 28, 2004
 */
package org.python.pydev.debug.model;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.tasklist.ITaskListResourceAdapter;
import org.python.pydev.debug.model.remote.ChangeVariableCommand;
import org.python.pydev.shared_interactive_console.console.codegen.IScriptConsoleCodeGenerator;

/**
 * Represents a python variable.
 *
 * Eclipse gives you an option to separate implementation of variable
 * and its value. I've found it convenient to roll both of them into 1
 * class.
 *
 */
public class PyVariable extends PlatformObject implements IVariable, IValue, IVariableLocator {

    protected String name;
    protected String type;
    protected String qualifier;
    protected String value;
    protected String scope;
    protected AbstractDebugTarget target;
    protected boolean isModified;
    protected IVariableLocator locator;

    //Only create one instance of an empty array to be returned
    static final IVariable[] EMPTY_IVARIABLE_ARRAY = new IVariable[0];

    public PyVariable(AbstractDebugTarget target, String name, String type, String value, IVariableLocator locator,
            String scope) {
        this.value = value;
        this.name = name;
        this.type = type;
        this.target = target;
        this.locator = locator;
        this.scope = scope;
        this.isModified = false;
    }

    /**
     * This is usually not set. It's only set on special cases where the variable must be accessed by the global objects list.
     */
    protected String id;

    private boolean isReturnValue;
    private boolean isIPythonHidden;
    private boolean isErrorOnEval;

    /**
     * This method sets information about how this variable was found.
     */
    public void setRefererrerFoundInfo(String id, String foundAs) {
        if (foundAs != null && foundAs.length() > 0) {
            name += " Found as: " + foundAs;
        }
        if (id != null && id.length() > 0) {
            this.id = id;
        }
    }

    @Override
    public String getThreadId() {
        return locator.getThreadId();
    }

    @Override
    public String getPyDBLocation() {
        if (id == null) {
            return locator.getPyDBLocation() + "\t" + name;
        }
        //Ok, this only happens when we're dealing with references with no proper scope given and we need to get
        //things by id (which is usually not ideal). In this case we keep the proper thread id and set the frame id
        //as the id of the object to be searched later on based on the list of all alive objects.
        return locator.getThreadId() + "\t" + id + "\tBY_ID";
    }

    @Override
    public String getUniqueId() {
        // This was added because on a corner-case, it's possible to have
        // the same pydb location for a variable if the name of the variable
        // is equal to the name of a function whose return value was gotten.
        return getPyDBLocation() + this.isReturnValue;
    }

    public String getDetailText() throws DebugException {
        return getValueString();
    }

    @Override
    public IValue getValue() throws DebugException {
        return this;
    }

    @Override
    public String getValueString() throws DebugException {
        if (value == null) {
            return "";
        }
        if ("StringType".equals(type) || "UnicodeType".equals(type)) {
            return "\"" + value + "\"";
        }
        return value;
    }

    public void copyValueString(PyVariable newVariable) {
        this.type = newVariable.type;
        this.value = newVariable.value;
    }

    @Override
    public String getName() throws DebugException {
        return name;
    }

    @Override
    public String getModelIdentifier() {
        return target.getModelIdentifier();
    }

    @Override
    public IDebugTarget getDebugTarget() {
        return target;
    }

    @Override
    public ILaunch getLaunch() {
        return target.getLaunch();
    }

    /**
     * LATER valueChanging nterface has not been implemented yet.
     * When implemented, recently changed variables are shown in red.
     */
    @Override
    public boolean supportsValueModification() {
        return this.locator != null;
    }

    @Override
    public boolean hasValueChanged() throws DebugException {
        return isModified;
    }

    public void setModified(boolean mod) {
        isModified = mod;
    }

    /**
     * This method is called when some value has to be changed to some other expression.
     *
     * Note that it will (currently) only work for changing local values that are in the topmost frame.
     * -- python has no way of making it work right now (see: pydevd_vars.changeAttrExpression)
     */
    @Override
    public void setValue(String expression) throws DebugException {
        ChangeVariableCommand changeVariableCommand = getChangeVariableCommand(target, expression);
        if (changeVariableCommand != null) {
            target.postCommand(changeVariableCommand);
            this.value = expression;
            target.fireEvent(new DebugEvent(this, DebugEvent.CONTENT | DebugEvent.CHANGE));
        }
    }

    @Override
    public void setValue(IValue value) throws DebugException {
    }

    @Override
    public boolean verifyValue(String expression) throws DebugException {
        return true;
    }

    @Override
    public boolean verifyValue(IValue value) throws DebugException {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getAdapter(Class<T> adapter) {
        AdapterDebug.print(this, adapter);

        if (adapter.equals(ILaunch.class)) {
            return target.getAdapter(adapter);

        } else if (adapter.equals(org.eclipse.debug.ui.actions.IRunToLineTarget.class)) {
            return (T) this.target.getRunToLineTarget();

        } else if (adapter.equals(IScriptConsoleCodeGenerator.class)) {
            return (T) new PyConsoleCodeGeneratorVariable(this);

        } else if (adapter.equals(IPropertySource.class) || adapter.equals(ITaskListResourceAdapter.class)
                || adapter.equals(org.eclipse.ui.IContributorResourceAdapter.class)
                || adapter.equals(org.eclipse.ui.IActionFilter.class)
                || adapter.equals(org.eclipse.ui.model.IWorkbenchAdapter.class)
                || adapter.equals(org.eclipse.debug.ui.actions.IToggleBreakpointsTarget.class)
                || adapter.equals(IResource.class) || adapter.equals(org.eclipse.core.resources.IFile.class)) {
            return super.getAdapter(adapter);
        }

        //cannot check for the actual interface because it may not be available on eclipse 3.2 (it's only available
        //from 3.3 onwards... and this is only a hack for it to work with eclipse 3.4)
        if (adapter.toString().endsWith(
                "org.eclipse.debug.internal.ui.viewers.model.provisional.IElementContentProvider")) {
            return (T) new PyVariableContentProviderHack();
        }
        AdapterDebug.printDontKnow(this, adapter);
        return super.getAdapter(adapter);
    }

    @Override
    public boolean isAllocated() throws DebugException {
        return true;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return EMPTY_IVARIABLE_ARRAY;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return false;
    }

    @Override
    public String getReferenceTypeName() throws DebugException {
        return type;
    }

    public ChangeVariableCommand getChangeVariableCommand(AbstractDebugTarget dbg, String expression) {
        return new ChangeVariableCommand(dbg, getPyDBLocation(), expression);
    }

    public void forceGetNewVariables() {
        //no-op for variable (only really available for PyVariableCollection).
    }

    public void setIsReturnValue(boolean isReturnValue) {
        this.isReturnValue = isReturnValue;
    }

    public boolean isReturnValue() {
        return isReturnValue;
    }

    public void setIsIPythonHidden(boolean isIPythonHidden) {
        this.isIPythonHidden = isIPythonHidden;
    }

    public boolean isIPythonHidden() {
        return isIPythonHidden;
    }

    public void setIsErrorOnEval(boolean isErrorOnEval) {
        this.isErrorOnEval = isErrorOnEval;
    }

    public boolean isErrorOnEval() {
        return isErrorOnEval;
    }

    public String getQualifier() {
        return qualifier;
    }

    public void setQualifier(String qualifier) {
        this.qualifier = qualifier;
    }

}
