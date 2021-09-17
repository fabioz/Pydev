/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.hover;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.python.pydev.core.IPythonPartitions;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.actions.EvalExpressionAction;
import org.python.pydev.editor.hover.AbstractPyEditorTextHover;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;

/**
 * Gathers hover info during a debug session.
 * 
 * @author Fabio
 */
public class PyDebugHover extends AbstractPyEditorTextHover {

    public static String ID = "org.python.pydev.debug.ui.hover.PyDebugHover";

    /**
     * Gets the value from the debugger for the currently hovered string.
     */
    @Override
    public String getHoverInfo(final ITextViewer textViewer, IRegion hoverRegion) {
        if (!PyHoverPreferencesPage.getShowValuesWhileDebuggingOnHover()) {
            return null;
        }

        IAdaptable object = DebugUITools.getDebugContext();

        IDebugElement context = null;
        if (object instanceof IDebugElement) {
            context = (IDebugElement) object;
        } else if (object instanceof ILaunch) {
            context = ((ILaunch) object).getDebugTarget();
        }

        if (context != null) {
            IDebugTarget debugTarget = context.getDebugTarget();
            if (debugTarget == null || debugTarget.isTerminated()) {
                return null;
            }
            String act = null;
            final ITextSelection[] textSelection = new ITextSelection[1];
            if (Thread.currentThread() == textViewer.getTextWidget().getDisplay().getThread()) {
                textSelection[0] = (ITextSelection) textViewer.getSelectionProvider().getSelection();
            } else {
                textViewer.getTextWidget().getDisplay().syncExec(new Runnable() {

                    @Override
                    public void run() {
                        textSelection[0] = (ITextSelection) textViewer.getSelectionProvider().getSelection();
                    }

                });
            }

            PySelection ps = new PySelection(textViewer.getDocument(),
                    hoverRegion.getOffset() + hoverRegion.getLength());
            int mouseOffset = ps.getAbsoluteCursorOffset();

            int offset = textSelection[0].getOffset();
            int len = textSelection[0].getLength();
            boolean reportSyntaxErrors = false;
            if (len > 0 && mouseOffset >= offset && offset + len >= mouseOffset) {
                try {
                    act = ps.getDoc().get(offset, len);
                    reportSyntaxErrors = true; //the user has text selected
                } catch (BadLocationException e) {
                    //that's Ok... we were not able to get the actual selection here
                    Log.log(e);
                }
            }
            if (act == null || act.trim().length() == 0) {
                String[] activationTokenAndQual = ps.getActivationTokenAndQualifier(true);
                act = activationTokenAndQual[0] + activationTokenAndQual[1];
            }

            //OK, we're in a debug context...
            IWatchExpression watchExpression = EvalExpressionAction.createWatchExpression(act);
            watchExpression.evaluate();
            EvalExpressionAction.waitForExpressionEvaluation(watchExpression);
            IValue value = watchExpression.getValue();
            if (value != null) {
                try {
                    String valueString = value.getValueString();
                    if (valueString != null) {
                        if (!reportSyntaxErrors) {
                            if (valueString.startsWith("SyntaxError") && valueString.indexOf("<string>") != -1) {
                                //Don't report syntax errors in the hover
                                return null;
                            }
                        }
                        return valueString + "\n";
                    }
                } catch (DebugException e) {
                    Log.log(e);
                }
            }
        }

        return null;
    }

    @Override
    public boolean isContentTypeSupported(String contentType) {
        boolean pythonCommentOrMultiline = IPythonPartitions.NON_DEFAULT_TYPES_AS_SET.contains(contentType);

        return !pythonCommentOrMultiline;
    }

}
