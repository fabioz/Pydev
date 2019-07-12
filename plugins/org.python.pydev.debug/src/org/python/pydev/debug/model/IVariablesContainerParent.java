package org.python.pydev.debug.model;

import org.python.pydev.debug.model.remote.GetVariableCommand;

public interface IVariablesContainerParent extends IVariableLocator {

    AbstractDebugTarget getTarget();

    GetVariableCommand getVariableCommand(AbstractDebugTarget target);

    IVariableLocator getGlobalLocator();

}
