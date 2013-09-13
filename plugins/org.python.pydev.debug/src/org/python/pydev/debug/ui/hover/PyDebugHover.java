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
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.ui.actions.EvalExpressionAction;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.editor.hover.IPyHoverParticipant;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;


/**
 * Gathers hover info during a debug session.
 * 
 * @author Fabio
 */
public class PyDebugHover implements IPyHoverParticipant {

    /**
     * Gets the value from the debugger for the currently hovered string.
     */
    public String getHoverText(IRegion hoverRegion, PySourceViewer s, PySelection ps, ITextSelection selection) {
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
            ITextSelection textSelection = (ITextSelection) selection;
            int mouseOffset = ps.getAbsoluteCursorOffset();

            int offset = textSelection.getOffset();
            int len = textSelection.getLength();
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
                String[] activationTokenAndQual = ps.getActivationTokenAndQual(true);
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

}
