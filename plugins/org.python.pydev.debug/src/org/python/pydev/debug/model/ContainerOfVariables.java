package org.python.pydev.debug.model;

import java.lang.ref.WeakReference;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IVariable;
import org.python.pydev.debug.model.remote.GetVariableCommand;

public class ContainerOfVariables {

    private volatile IVariable[] variables;
    private WeakReference<IVariablesContainerParent> parent;
    private volatile boolean onAskGetNewVars = true;

    private final static Object lock = new Object();
    private volatile boolean gettingInitialVariables = false;
    private final VariablesLoader variablesLoader;

    public IVariable[] getInternalVariables() {
        return this.variables;
    }

    public ContainerOfVariables(IVariablesContainerParent parent, boolean addGlobalsVarible) {
        this.parent = new WeakReference<IVariablesContainerParent>(parent);
        this.variablesLoader = new VariablesLoader(this, addGlobalsVarible);
    }

    /* default */ PyVariable[] setVariables(PyVariable[] newVars) {
        IVariable[] oldVars = this.variables;
        if (newVars == oldVars) {
            return newVars;
        }
        IVariablesContainerParent p = this.parent.get();
        if (p == null) {
            return newVars;
        }
        AbstractDebugTarget target = p.getTarget();
        this.variables = newVars;

        if (!gettingInitialVariables) {
            if (target != null) {
                target.fireEvent(new DebugEvent(p, DebugEvent.CHANGE, DebugEvent.CONTENT));
            }
        }
        return newVars;
    }

    public IVariable[] getVariables() throws DebugException {
        // System.out.println("get variables: " + super.toString() + " initial: " + this.variables);
        if (onAskGetNewVars) {
            synchronized (lock) {
                //double check idiom for accessing onAskGetNewVars.
                if (onAskGetNewVars) {
                    gettingInitialVariables = true;
                    try {
                        PyVariable[] vars = variablesLoader.fetchVariables();

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

}
