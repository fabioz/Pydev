/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.python.pydev.debug.core.PydevDebugPlugin;

public class WatchExpressionAction implements IEditorActionDelegate {
    private ITextSelection fSelection;

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    }

    @Override
    public void run(IAction action) {
        if (fSelection == null) {
            return;
        }
        String text = fSelection.getText();
        createExpression(text);
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        fSelection = null;
        if (selection instanceof ITextSelection) {
            fSelection = (ITextSelection) selection;
        }
    }

    private void showExpressionsView() {
        IWorkbenchPage page = PydevDebugPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getActivePage();
        IViewPart part = page.findView(IDebugUIConstants.ID_EXPRESSION_VIEW);
        if (part == null) {
            try {
                page.showView(IDebugUIConstants.ID_EXPRESSION_VIEW);
            } catch (PartInitException e) {
            }
        } else {
            page.bringToTop(part);
        }

    }

    private void createExpression(String variable) {
        IWatchExpression expression = DebugPlugin.getDefault().getExpressionManager().newWatchExpression(variable);

        DebugPlugin.getDefault().getExpressionManager().addExpression(expression);
        IAdaptable object = DebugUITools.getDebugContext();
        IDebugElement context = null;
        if (object instanceof IDebugElement) {
            context = (IDebugElement) object;
        } else if (object instanceof ILaunch) {
            context = ((ILaunch) object).getDebugTarget();
        }
        expression.setExpressionContext(context);
        showExpressionsView();
    }
}
