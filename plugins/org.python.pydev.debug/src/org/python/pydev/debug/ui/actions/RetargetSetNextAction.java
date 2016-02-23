/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.RetargetAction;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * @author Hussain Bohra
 */
public class RetargetSetNextAction extends RetargetAction {

    private DebugContextListener fContextListener = new DebugContextListener();
    private ISuspendResume fTargetElement = null;

    class DebugContextListener implements IDebugContextListener {

        protected void contextActivated(ISelection selection) {
            fTargetElement = null;
            if (selection instanceof IStructuredSelection) {
                IStructuredSelection ss = (IStructuredSelection) selection;
                if (ss.size() == 1) {
                    fTargetElement = (ISuspendResume) DebugPlugin
                            .getAdapter(ss.getFirstElement(), ISuspendResume.class);
                }
            }
            IAction action = getAction();
            if (action != null) {
                action.setEnabled(fTargetElement != null && hasTargetAdapter());
            }
        }

        @Override
        public void debugContextChanged(DebugContextEvent event) {
            contextActivated(event.getContext());
        }
    }

    @Override
    protected boolean canPerformAction(Object target, ISelection selection, IWorkbenchPart part) {
        return fTargetElement != null && ((ISetNextTarget) target).canSetNextToLine(part, selection, fTargetElement);
    }

    @Override
    protected Class<ISetNextTarget> getAdapterClass() {
        return ISetNextTarget.class;
    }

    @Override
    protected String getOperationUnavailableMessage() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void performAction(Object target, ISelection selection, IWorkbenchPart part) throws CoreException {

        boolean result = ((ISetNextTarget) target).setNextToLine(part, selection, fTargetElement);

        if (result == false) {
            IStatus status = new Status(IStatus.WARNING, DebugUIPlugin.getUniqueIdentifier(),
                    "Unable to set the next statement to this location. The next statement cannot be set to another function/loop.");
            DebugUIPlugin.errorDialog(DebugUIPlugin.getShell(), DebugUIPlugin.removeAccelerators("Set Next Statement"),
                    "Error", status);
        }
    }

    /*
     * (non-Javadoc)
     // * 
     * @see org.eclipse.ui.IWorkbenchWindowActionDelegate#dispose()
     */
    @Override
    public void dispose() {
        DebugUITools.getDebugContextManager().getContextService(fWindow).removeDebugContextListener(fContextListener);
        super.dispose();
    }

    /*
     * (non-Javadoc)
     * 
     * @seeorg.eclipse.ui.IWorkbenchWindowActionDelegate#init(org.eclipse.ui.
     * IWorkbenchWindow)
     */
    @Override
    public void init(IWorkbenchWindow window) {
        super.init(window);
        IDebugContextService service = DebugUITools.getDebugContextManager().getContextService(window);
        service.addDebugContextListener(fContextListener);
        ISelection activeContext = service.getActiveContext();
        fContextListener.contextActivated(activeContext);
    }
}
