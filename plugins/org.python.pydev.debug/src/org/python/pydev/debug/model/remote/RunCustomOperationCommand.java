/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.debug.model.remote;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.handlers.PrettyPrintCommandHandler;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.IVariableLocator;
import org.python.pydev.debug.model.PyStackFrame;
import org.python.pydev.debug.model.PyVariable;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * Run a custom bit of Python in the context of the specified debug target.
 * <p>
 * This command takes a variable or expression (expressed as an {@link IVariableLocator#getPyDBLocation()} style
 * location) and passes it to the function provided in the constructor. The constructor also takes either a code
 * snippet that should define the function, or a file to execfile that should define the function.
 * <p>
 * Once created, the command should be posted to the target with {@link AbstractDebugTarget#postCommand(AbstractDebuggerCommand)}.
 * Optionally, the function run on the target can return a string for further processing. In this case the command's
 * {@link #setCompletionListener(ICommandResponseListener)} should be set and on completion, {@link #getResponsePayload()}
 * can be used to obtain the returned value.
 * <p>
 * For an example, see {@link PrettyPrintCommandHandler}
 */
public class RunCustomOperationCommand extends AbstractDebuggerCommand {

    private String encodedCodeOrFile;
    private String operationFnName;
    private IVariableLocator locator;
    private String style;
    private String responsePayload;

    /**
     * Extracts a debug target and locator from the provided selection.
     * <p>
     * The Eclipse definition org.python.pydev.debug.model.RunCustomOperationCommand.ExtractableContext can
     * be used to filter menu presentation on whether the selection may be extractable.
     *
     * @param selection
     * @return Debug target and locator suitable for passing the the constructor,
     * or <code>null</code> if no suitable selection is selected.
     */
    public static Tuple<AbstractDebugTarget, IVariableLocator> extractContextFromSelection(ISelection selection) {
        if (selection instanceof StructuredSelection) {
            StructuredSelection structuredSelection = (StructuredSelection) selection;
            Object elem = structuredSelection.getFirstElement();

            if (elem instanceof PyVariable) {
                PyVariable pyVar = (PyVariable) elem;
                AbstractDebugTarget target = (AbstractDebugTarget) pyVar.getDebugTarget();
                return new Tuple<AbstractDebugTarget, IVariableLocator>(target, pyVar);

            } else if (elem instanceof IWatchExpression) {
                IWatchExpression expression = (IWatchExpression) elem;
                final String expressionText = expression.getExpressionText();
                IDebugTarget debugTarget = expression.getDebugTarget();

                if (debugTarget instanceof AbstractDebugTarget) {
                    AbstractDebugTarget target = (AbstractDebugTarget) debugTarget;
                    IAdaptable context = DebugUITools.getDebugContext();
                    final PyStackFrame stackFrame = (PyStackFrame) context.getAdapter(PyStackFrame.class);

                    if (stackFrame != null) {
                        return new Tuple<AbstractDebugTarget, IVariableLocator>(target, new IVariableLocator() {

                            @Override
                            public String getThreadId() {
                                return stackFrame.getThreadId();
                            }

                            @Override
                            public String getPyDBLocation() {
                                String locator = stackFrame.getExpressionLocator().getPyDBLocation();
                                return locator + "\t" + expressionText;
                            }
                        });
                    }
                }
            }
        }
        return null;
    }

    private RunCustomOperationCommand(AbstractDebugTarget target, IVariableLocator locator,
            String style, String codeOrFile, String operationFnName) {
        super(target);

        this.locator = locator;
        this.style = style;
        this.encodedCodeOrFile = encode(codeOrFile);
        this.operationFnName = operationFnName;
    }

    /**
     * Create a new command to run with the function defined in a string.
     *
     * @param target Debug Target to run on
     * @param locator Location of variable or expression.
     * @param operationSource Definition of the function to be run (this code is "exec"ed by the target)
     * @param operationFnName Function to call, must be defined by operationSource
     */
    public RunCustomOperationCommand(AbstractDebugTarget target, IVariableLocator locator,
            String operationSource, String operationFnName) {
        this(target, locator, "EXEC", operationSource, operationFnName);
    }

    /**
     * Create a new command to run with the function defined in a file.
     *
     * @param target Debug Target to run on
     * @param locator Location of variable or expression.
     * @param operationPyFile Definition of the function to be run (this file is "execfile"d by the target)
     * @param operationFnName Function to call, must be defined by operationSource
     */
    public RunCustomOperationCommand(AbstractDebugTarget target, IVariableLocator locator,
            File operationPyFile, String operationFnName) {
        this(target, locator, "EXECFILE", operationPyFile.toString(), operationFnName);
    }

    @Override
    public String getOutgoing() {
        String payload = locator.getPyDBLocation() + "||" + style + "\t" + encodedCodeOrFile + "\t" + operationFnName;
        String cmd = makeCommand(CMD_RUN_CUSTOM_OPERATION, sequence, payload);
        return cmd;
    }

    @Override
    public boolean needResponse() {
        return true;
    }

    @Override
    public void processOKResponse(int cmdCode, String payload) {
        if (cmdCode == CMD_RUN_CUSTOM_OPERATION) {
            this.responsePayload = decode(payload);
        }
    }

    /**
     * Return the response received from the custom command
     * @return the response or <code>null</code> if an error or no response has been received.
     */
    public String getResponsePayload() {
        return responsePayload;
    }

    private static String encode(String in) {
        try {
            return URLEncoder.encode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.log("Unreachable? UTF-8 is always supported.", e);
            return "";
        }
    }

    private static String decode(String in) {
        try {
            return URLDecoder.decode(in, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.log("Unreachable? UTF-8 is always supported.", e);
            return "";
        }
    }

}
