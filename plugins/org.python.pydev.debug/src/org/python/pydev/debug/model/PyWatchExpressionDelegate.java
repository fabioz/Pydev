/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: tusk
 * Created: Dec 05, 2004
 */
package org.python.pydev.debug.model;

import java.util.Arrays;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.eclipse.debug.core.model.IWatchExpressionListener;
import org.eclipse.debug.core.model.IWatchExpressionResult;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.model.remote.AbstractDebuggerCommand;
import org.python.pydev.debug.model.remote.EvaluateExpressionCommand;
import org.python.pydev.debug.model.remote.ICommandResponseListener;

public class PyWatchExpressionDelegate implements IWatchExpressionDelegate, IWatchExpressionResult,
        ICommandResponseListener {

    private final Object sync = new Object();
    protected IValue value = null;
    protected IDebugElement context;
    protected String expression;
    protected IWatchExpressionListener listener;

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionDelegate#evaluateExpression(java.lang.String, org.eclipse.debug.core.model.IDebugElement, org.eclipse.debug.core.model.IWatchExpressionListener)
     */
    @Override
    public void evaluateExpression(String expression, IDebugElement context, IWatchExpressionListener listener) {
        this.expression = expression;
        this.context = context;
        this.listener = listener;
        if (context instanceof PyStackFrame) {

            AbstractDebugTarget target = (AbstractDebugTarget) context.getDebugTarget();
            if (target == null) {
                return; //disposed
            }

            // send the command, and then busy-wait
            EvaluateExpressionCommand cmd = new EvaluateExpressionCommand(target, expression, ((PyStackFrame) context)
                    .getLocalsLocator().getPyDBLocation(), false);
            cmd.setCompletionListener(this);
            target.postCommand(cmd);

        } else {
            addError("unknown expression context");
            listener.watchEvaluationFinished(this);
        }
    }

    protected String[] errors = new String[0];

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionResult#getValue()
     */
    @Override
    public IValue getValue() {
        synchronized (sync) {
            return value;
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionResult#hasErrors()
     */
    @Override
    public boolean hasErrors() {
        return errors.length > 0;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionResult#getErrorMessages()
     */
    @Override
    public String[] getErrorMessages() {
        return errors;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionResult#getExpressionText()
     */
    @Override
    public String getExpressionText() {
        return expression;
    }

    /* (non-Javadoc)
     * @see org.eclipse.debug.core.model.IWatchExpressionResult#getException()
     */
    @Override
    public DebugException getException() {
        return null;
    }

    public void addError(String message) {
        errors = Arrays.copyOf(errors, errors.length + 1);
        errors[errors.length - 1] = message;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.debug.model.remote.ICommandResponseListener#commandComplete(org.python.pydev.debug.model.remote.AbstractDebuggerCommand)
     */
    @Override
    public void commandComplete(AbstractDebuggerCommand cmd) {
        try {
            String payload = ((EvaluateExpressionCommand) cmd).getResponse();
            PyVariable[] variables = XMLUtils.XMLToVariables((AbstractDebugTarget) context.getDebugTarget(),
                    ((PyStackFrame) context).getExpressionLocator(), payload);
            IValue value = newValue(variables);
            synchronized (sync) {
                this.value = value;
            }

        } catch (CoreException e) {
            addError("Evaluation of " + this.getExpressionText() + " failed: " + e.getMessage());
            PydevDebugPlugin.log(IStatus.ERROR, "Error fetching a variable", e);
        }

        listener.watchEvaluationFinished(this);
    }

    private IValue newValue(PyVariable[] variables) {
        if (null == variables || variables.length == 0) {
            addError("Evaluation of " + this.getExpressionText() + " failed: no variables.");
            return null; // see #getValue(), implies evaluation failure.
        }
        if (variables.length == 1) {
            return variables[0];
        }
        return new WatchExpressionValue(this.context, variables);
    }

    public static class WatchExpressionValue extends DebugElement implements IValue {
        private final IVariable[] variables;

        public WatchExpressionValue(IDebugElement context, IVariable[] variables) {
            super(context.getDebugTarget());
            this.variables = variables;
        }

        @Override
        public String getModelIdentifier() {
            return this.getDebugTarget().getModelIdentifier();
        }

        @Override
        public String getReferenceTypeName() throws DebugException {
            return null;
        }

        @Override
        public String getValueString() throws DebugException {
            boolean first = true;
            StringBuilder sb = new StringBuilder().append("{");
            for (IVariable variable : variables) {
                if (first) {
                    first = false;
                } else {
                    sb.append(", ");
                }
                IValue val = variable.getValue();
                sb.append(variable.getName()).append(": ").append(val == null ? "<error>" : val.getValueString());
            }
            return sb.append("}").toString();
        }

        @Override
        public boolean isAllocated() throws DebugException {
            return false;
        }

        @Override
        public IVariable[] getVariables() throws DebugException {
            if (hasVariables()) {
                return Arrays.copyOf(variables, variables.length);
            }
            return PyVariable.EMPTY_IVARIABLE_ARRAY;
        }

        @Override
        public boolean hasVariables() throws DebugException {
            return variables.length > 0;
        }

    }
}
