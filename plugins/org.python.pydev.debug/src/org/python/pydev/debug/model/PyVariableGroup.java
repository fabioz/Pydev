package org.python.pydev.debug.model;

import java.util.List;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.model.remote.ChangeVariableCommand;
import org.python.pydev.debug.model.remote.GetVariableCommand;

public class PyVariableGroup extends PyVariable
        implements IVariableLocator, IVariablesContainerParent {

    private static final PyVariable[] EMPTY_VARIABLE_ARRAY = new PyVariable[0];
    private volatile PyVariable[] variables;
    private String threadId;
    private String parentUniqueId;

    public PyVariableGroup(AbstractDebugTarget target, String name, String type, String value, String threadId,
            String scope, String parentUniqueId) {
        super(target, name, type, value, null, scope);
        this.variables = EMPTY_VARIABLE_ARRAY;
        this.threadId = threadId;
        this.parentUniqueId = parentUniqueId;
    }

    @Override
    public AbstractDebugTarget getTarget() {
        return target;
    }

    public PyVariable[] fetchVariables() {
        return this.variables;
    }

    @Override
    public IVariable[] getVariables() throws DebugException {
        return this.variables;
    }

    @Override
    public boolean hasVariables() throws DebugException {
        return this.variables.length > 0;
    }

    @Override
    public GetVariableCommand getVariableCommand(AbstractDebugTarget target) {
        //no-op
        return null;
    }

    @Override
    public IVariableLocator getGlobalLocator() {
        return null;
    }

    public void setVariables(List<PyVariable> list) {
        this.variables = list.toArray(new PyVariable[0]);
    }

    @Override
    public ChangeVariableCommand getChangeVariableCommand(AbstractDebugTarget dbg, String expression) {
        //no-op
        return null;
    }

    @Override
    public String getPyDBLocation() {
        return null;
    }

    @Override
    public String getThreadId() {
        return threadId;
    }

    @Override
    public String getUniqueId() {
        return "PyVariableGroup - " + parentUniqueId + " - " + name;
    }

}
