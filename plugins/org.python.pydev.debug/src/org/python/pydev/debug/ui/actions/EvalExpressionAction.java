/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import java.util.Set;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.expressions.EvaluationContext;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.ui.DebugPopup;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorActionDelegate;
import org.eclipse.ui.IEditorPart;
import org.python.pydev.debug.core.PydevDebugPlugin;

/**
 * The original idea was not to use the <code>org.eclipse.jdt.internal.debug.ui</code> stuff; that's why this does not 
 * do things like extending {@link org.eclipse.jdt.internal.debug.ui.actions.PopupDisplayAction}, etc. 
 * 
 * But after all was said and done, I didn't feel like finding an alternate way of writing {@link DisplayPopup#persist()},
 * so this does result in depending on <code>org.eclipse.jdt.debug.ui</code> 
 * just for that - silly me. 
 * 
 * But then again, the original had dependencies on <code>org.eclipse.jdt</code>,
 * <code>org.eclipse.jdt.code</code> and <code>org.eclipse.jdt.launching</code>, so what's one extra one?
 * 
 * @see
 * 
 * @author "<a href=mailto:grisha@alum.mit.edu>Gregory Golberg</A>"
 */
public class EvalExpressionAction extends AbstractHandler implements IHandler, IEditorActionDelegate {

    /**
     * Action ID for displaying variable
     */
    public static final String ACTION_DEFINITION_ID = "org.python.pydev.debug.command.Display"; //$NON-NLS-1$

    private ITextSelection fSelection;

    @Override
    public void setActiveEditor(IAction action, IEditorPart targetEditor) {
    }

    /*
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        if (fSelection == null) {
            return;
        }
        String text = fSelection.getText();
        eval(text);
    }

    /*
     * @see org.eclipse.ui.IActionDelegate#selectionChanged(org.eclipse.jface.action.IAction, org.eclipse.jface.viewers.ISelection)
     */
    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        fSelection = null;
        if (selection instanceof ITextSelection) {
            fSelection = (ITextSelection) selection;
        }
    }

    /**
     * This hack just creates a Watch expression, gets result and removes the watch expression. 
     * This is simple, since the watch functionality is already there.
     * 
     * @see WatchExpressionAction#createExpression
     */
    private void eval(final String expr) {
        final IWatchExpression expression = createWatchExpression(expr);

        final Shell shell = PydevDebugPlugin.getActiveWorkbenchWindow().getShell();
        Display display = PydevDebugPlugin.getDefault().getWorkbench().getDisplay();
        final Point point = display.getCursorLocation();

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run() {
                expression.evaluate();
                waitForExpressionEvaluation(expression);
                try {
                    IValue value = expression.getValue();
                    String result = null;
                    if (value != null) {
                        result = expr + "\n" + value.getValueString();
                        DisplayPopup popup = new DisplayPopup(shell, point, result);
                        popup.open();
                    }
                } catch (DebugException e) {
                    DebugPlugin.log(e);
                    return;
                } catch (Throwable t) {
                    DebugPlugin.log(t);
                }
            }
        });
    }

    /**
     * Enters a busy wait until the expression finishes evaluating
     * 
     * @param expression the watch expression we shoud wait for
     */
    public static void waitForExpressionEvaluation(final IWatchExpression expression) {
        while (expression.isPending()) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                continue;
            }
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
    }

    /**
     * Creates a watch expression to be evaluated with the current debug context.
     * 
     * @param expr the expression that should be evaluated in the current context
     * @return the created expression.
     */
    public static IWatchExpression createWatchExpression(final String expr) {
        final IWatchExpression expression = DebugPlugin.getDefault().getExpressionManager().newWatchExpression(expr);
        IAdaptable object = DebugUITools.getDebugContext();
        IDebugElement context = null;
        if (object instanceof IDebugElement) {
            context = (IDebugElement) object;
        } else if (object instanceof ILaunch) {
            context = ((ILaunch) object).getDebugTarget();
        }

        expression.setExpressionContext(context);
        return expression;
    }

    /*
     * @see org.eclipse.core.commands.AbstractHandler#execute(org.eclipse.core.commands.ExecutionEvent)
     */
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        EvaluationContext evalCtx = (org.eclipse.core.expressions.EvaluationContext) event.getApplicationContext();
        Object obj = evalCtx.getDefaultVariable();
        if (obj instanceof Set) {
            Set set = (Set) obj;
            if (set.size() > 0) {
                Object sel = set.iterator().next();
                if (sel instanceof TextSelection) {
                    String expr = ((TextSelection) sel).getText();
                    if (expr != null && expr.trim().length() > 0) {
                        eval(expr);
                    }
                }
            }
        }
        return null;
    }

    /**
     * @see org.eclipse.jdt.internal.debug.ui.actions.PopupDisplayAction.DisplayPopup
     */
    private class DisplayPopup extends DebugPopup {
        public DisplayPopup(Shell shell, Point point, String text) {
            super(shell, point, ACTION_DEFINITION_ID + "2");

            this.text = text;
        }

        @Override
        protected String getActionText() {
            return "Move to Display view";
        }

        @Override
        protected void persist() {
            //            String displayId = IJavaDebugUIConstants.ID_DISPLAY_VIEW;
            //            IWorkbenchPage page = PydevDebugPlugin.getActiveWorkbenchWindow().getActivePage();
            //            IViewReference viewRef = page.findViewReference(displayId);
            //            IViewPart view = null;
            //            if (viewRef != null) {
            //                view = viewRef.getView(true);
            //            } else {
            //                try {
            //                    view = page.showView(displayId);
            //                } catch (PartInitException e) {
            //                    DebugPlugin.log(e);
            //                    return;
            //                }
            //            }
            //            page.activate(page.getActivePart());
            //            IDataDisplay adapter = (IDataDisplay) view.getAdapter(IDataDisplay.class);
            //            adapter.displayExpression(this.text);
            super.persist();
        }

        private String text;

        @Override
        protected Control createDialogArea(Composite parent) {
            GridData gd = new GridData(GridData.FILL_BOTH);
            StyledText text = new StyledText(parent, SWT.MULTI | SWT.READ_ONLY | SWT.WRAP | SWT.H_SCROLL | SWT.V_SCROLL);
            text.setLayoutData(gd);

            text.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
            text.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_INFO_BACKGROUND));

            text.setText(this.text);

            return text;
        }
    }

}
