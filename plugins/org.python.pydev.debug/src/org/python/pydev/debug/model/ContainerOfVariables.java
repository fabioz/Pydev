package org.python.pydev.debug.model;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.model.remote.GetVariableCommand;
import org.python.pydev.shared_core.log.Log;

public class ContainerOfVariables {

    private volatile IVariable[] variables;
    private WeakReference<IVariablesContainerParent> parent;
    private volatile boolean onAskGetNewVars = true;

    private final static Object lock = new Object();
    private volatile boolean gettingInitialVariables = false;

    public IVariable[] getInternalVariables() {
        return this.variables;
    }

    public ContainerOfVariables(IVariablesContainerParent parent) {
        this.parent = new WeakReference<IVariablesContainerParent>(parent);
    }

    public void setVariables(IVariable[] newVars) {
        IVariable[] oldVars = this.variables;
        if (newVars == oldVars) {
            return;
        }
        this.verifyVariablesModified(newVars, oldVars);
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return;
        }
        AbstractDebugTarget target = p.getTarget();
        this.variables = newVars;

        if (!gettingInitialVariables) {
            if (target != null) {
                target.fireEvent(new DebugEvent(p, DebugEvent.CHANGE, DebugEvent.CONTENT));
            }
        }
    }

    public IVariable[] getVariables(boolean addGlobalsVarible) throws DebugException {
        // System.out.println("get variables: " + super.toString() + " initial: " + this.variables);
        if (onAskGetNewVars) {
            synchronized (lock) {
                //double check idiom for accessing onAskGetNewVars.
                if (onAskGetNewVars) {
                    gettingInitialVariables = true;
                    try {
                        DeferredWorkbenchAdapter adapter = new DeferredWorkbenchAdapter(this, addGlobalsVarible);
                        IVariable[] vars = (IVariable[]) adapter.getChildren();

                        setVariables(vars);
                        // Important: only set to false after variables have been set.
                        onAskGetNewVars = false;
                    } finally {
                        gettingInitialVariables = false;
                    }
                }
            }
        }
        return this.variables;
    }

    public void forceGetNewVariables() {
        this.onAskGetNewVars = true;
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return;
        }

        AbstractDebugTarget target = p.getTarget();
        if (target != null) {
            // I.e.: if we do a new DebugEvent(this, DebugEvent.CHANGE, DebugEvent.CONTENT), the selection
            // of the editor is redone (thus, if the user uses F2 it'd get back to the current breakpoint
            // location because it'd be reselected).
            target.fireEvent(new DebugEvent(p, DebugEvent.CHANGE, DebugEvent.UNSPECIFIED));
        }
    }

    public AbstractDebugTarget getTarget() {
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return null;
        }
        return p.getTarget();

    }

    public IVariableLocator getLocator() {
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return null;
        }
        return p;
    }

    public GetVariableCommand getVariableCommand(AbstractDebugTarget target) {
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return null;
        }
        return p.getVariableCommand(target);
    }

    public IVariableLocator getGlobalLocator() {
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return null;
        }
        return p.getGlobalLocator();
    }

    /**
     * compares stack frames to check for modified variables (and mark them as modified in the new stack)
     * 
     * @param newFrame the new frame
     * @param oldFrame the old frame
     */
    private void verifyVariablesModified(IVariable[] newFrameVariables, IVariable[] oldVariables) {
        if (oldVariables == null || newFrameVariables == oldVariables) {
            return; //All variables are new, so, no point in notifying it.
        }
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
                    boolean equals = newVariable.getValueString().equals(oldVariable.getValueString());

                    //if it is not equal, it was modified
                    newVariable.setModified(!equals);

                } else { //it didn't exist before...
                    newVariable.setModified(true);
                }
            }

        } catch (DebugException e) {
            Log.log(e);
        }
    }
}
