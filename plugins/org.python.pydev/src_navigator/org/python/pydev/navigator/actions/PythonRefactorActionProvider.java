/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class PythonRefactorActionProvider extends CommonActionProvider {

    private PyRenameResourceAction renameResourceAction;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        ICommonViewerSite viewSite = aSite.getViewSite();
        if (viewSite instanceof ICommonViewerWorkbenchSite) {
            ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) viewSite;
            Shell shell = site.getShell();
            renameResourceAction = new PyRenameResourceAction(shell, site.getSelectionProvider());
        }
    }

    @Override
    public void fillActionBars(IActionBars actionBars) {
        if (renameResourceAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ActionFactory.RENAME.getId(), renameResourceAction);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    public void fillContextMenu(IMenuManager menu) {
        if (renameResourceAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, renameResourceAction);
        }
    }

}
