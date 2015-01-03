/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.curr_exception;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.python.pydev.debug.model.AbstractDebugTarget;
import org.python.pydev.debug.model.CaughtException;
import org.python.pydev.debug.views.BaseDebugView;
import org.python.pydev.debug.views.ILaunchAndDebugListener;
import org.python.pydev.shared_ui.utils.UIUtils;

/**
 * A view which shows information on the current exception.
 */
public class CurrentExceptionView extends BaseDebugView {

    private static final String CURRENT_EXCEPTION_VIEW_ID = "org.python.pydev.views.CurrentExceptionView";

    public CurrentExceptionView() {
    }

    /**
     * May only be called in the UI thread. If the view is not visible, shows it if the
     * preference to do that is set to true.
     * 
     * Note that it may return null if the preference to show it is false and the view is not currently shown.
     */
    public static CurrentExceptionView getView(boolean forceVisible) {
        return (CurrentExceptionView) UIUtils.getView(CURRENT_EXCEPTION_VIEW_ID, forceVisible);
    }

    @Override
    protected void configureToolBar(IViewSite viewSite) {
        IActionBars actionBars = viewSite.getActionBars();
        IToolBarManager toolBar = actionBars.getToolBarManager();
        //IMenuManager menuManager = actionBars.getMenuManager(); -- not adding anything to the menu for now.

        toolBar.add(new EditIgnoredCaughtExceptions(this));
    }

    @Override
    protected ILaunchAndDebugListener createListener() {
        return new ILaunchAndDebugListener() {

            @Override
            public void launchRemoved(ILaunch launch) {
                if (launch.getDebugTarget() instanceof AbstractDebugTarget) {
                    update();
                }
            }

            @Override
            public void launchChanged(ILaunch launch) {
                if (launch.getDebugTarget() instanceof AbstractDebugTarget) {
                    update();
                }
            }

            @Override
            public void launchAdded(ILaunch launch) {
            }

            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                for (DebugEvent debugEvent : events) {
                    if (debugEvent.getSource() instanceof AbstractDebugTarget) {
                        if (debugEvent.getKind() == DebugEvent.TERMINATE) {
                            update();
                        }
                    }
                }
            }
        };
    }

    /**
     * Makes the exception visible for each entry.
     */
    @Override
    protected void makeLastVisibleInTree(Object input) {
        if (input instanceof List) {
            List<AbstractDebugTarget> targets = (List) input;
            if (targets.size() > 0) {
                //i.e.: scroll to the last added element.
                AbstractDebugTarget element = targets.get(targets.size() - 1);
                List<CaughtException> currExceptions = element.getCurrExceptions();
                if (currExceptions.size() > 0) {
                    CaughtException caughtException = currExceptions.get(currExceptions.size() - 1);
                    if (caughtException != null) {
                        viewer.reveal(caughtException);
                    }
                }
            }
        }
    }

    /**
     * Updates the contents of the tree.
     */
    public void update() {
        super.updateTreeJob.schedule();
    }

    @Override
    protected ITreeContentProvider createContentProvider() {
        return new CurrentExceptionViewContentProvider();
    }

    @Override
    public void createPartControl(Composite parent) {
        super.createPartControl(parent);
        viewer.addDoubleClickListener(new IDoubleClickListener() {

            /**
             * When double-clicking show the location that has thrown the exception (or the stack frame clicked).
             */
            @Override
            public void doubleClick(DoubleClickEvent event) {
                ISelection selection = event.getSelection();
                if (selection instanceof IStructuredSelection) {
                    IStructuredSelection structuredSelection = (IStructuredSelection) selection;
                    Object context = structuredSelection.getFirstElement();

                    if (context instanceof IAdaptable) {
                        IAdaptable adaptable = (IAdaptable) context;
                        IStackFrame frame = (IStackFrame) adaptable.getAdapter(IStackFrame.class);
                        if (frame != null) {
                            ISourceDisplay adapter = (ISourceDisplay) frame.getAdapter(ISourceDisplay.class);
                            if (adapter != null) {
                                IWorkbenchPage activePage = UIUtils.getActivePage();
                                if (activePage != null) {
                                    adapter.displaySource(frame, activePage, false);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    @Override
    protected void onSetTreeInput() {
        IDebugTarget[] debugTargets = DebugPlugin.getDefault().getLaunchManager().getDebugTargets();
        List<AbstractDebugTarget> targets = new ArrayList<AbstractDebugTarget>();
        if (debugTargets.length > 0) {
            for (IDebugTarget iDebugTarget : debugTargets) {
                if (iDebugTarget instanceof AbstractDebugTarget) {
                    AbstractDebugTarget debugTarget = (AbstractDebugTarget) iDebugTarget;
                    if (!debugTarget.isTerminated() && !debugTarget.isDisconnected()) {
                        if (debugTarget.hasCurrExceptions()) {
                            targets.add(debugTarget);
                        }
                    }
                }
            }
        }
        viewer.setInput(targets);
    }

}
