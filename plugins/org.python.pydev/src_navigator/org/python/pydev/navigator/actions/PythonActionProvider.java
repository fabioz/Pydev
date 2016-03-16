/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 9, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionConstants;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.ICommonMenuConstants;
import org.eclipse.ui.navigator.ICommonViewerSite;
import org.eclipse.ui.navigator.ICommonViewerWorkbenchSite;

public class PythonActionProvider extends CommonActionProvider {

    private PyOpenPythonFileAction openPythonAction;
    private PyOpenResourceAction openResourceAction;
    private PyOpenExternalAction openExternalAction;
    private PyDeleteResourceAction deleteResourceAction;
    private PyCopyResourceAction copyResourceAction;
    private Clipboard clipboard;
    private PyPasteAction pasteAction;
    private PyMoveResourceAction moveResourceAction;
    private ISelectionProvider selectionProvider;

    @Override
    public void init(ICommonActionExtensionSite aSite) {
        ICommonViewerSite viewSite = aSite.getViewSite();
        if (viewSite instanceof ICommonViewerWorkbenchSite) {
            ICommonViewerWorkbenchSite site = (ICommonViewerWorkbenchSite) viewSite;
            Shell shell = site.getShell();

            ISharedImages images = PlatformUI.getWorkbench().getSharedImages();
            clipboard = new Clipboard(shell.getDisplay());
            selectionProvider = site.getSelectionProvider();
            openPythonAction = new PyOpenPythonFileAction(site.getPage(), selectionProvider);
            openResourceAction = new PyOpenResourceAction(site.getPage(), selectionProvider);
            openExternalAction = new PyOpenExternalAction(site.getPage(), selectionProvider);

            deleteResourceAction = new PyDeleteResourceAction(shell, selectionProvider);
            copyResourceAction = new PyCopyResourceAction(shell, selectionProvider, clipboard);
            pasteAction = new PyPasteAction(shell, selectionProvider, clipboard);
            moveResourceAction = new PyMoveResourceAction(shell, selectionProvider);

            copyResourceAction.setDisabledImageDescriptor(images
                    .getImageDescriptor(ISharedImages.IMG_TOOL_COPY_DISABLED));
            copyResourceAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_COPY));

            pasteAction.setDisabledImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
            pasteAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));

            deleteResourceAction.setDisabledImageDescriptor(images
                    .getImageDescriptor(ISharedImages.IMG_TOOL_DELETE_DISABLED));
            deleteResourceAction.setImageDescriptor(images.getImageDescriptor(ISharedImages.IMG_TOOL_DELETE));

        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillActionBars(org.eclipse.ui.IActionBars)
     */
    @Override
    public void fillActionBars(IActionBars actionBars) {
        if (openResourceAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ICommonActionConstants.OPEN, openResourceAction);
        }
        if (copyResourceAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ActionFactory.COPY.getId(), copyResourceAction);
        }
        if (pasteAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ActionFactory.PASTE.getId(), pasteAction);
        }
        if (deleteResourceAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ActionFactory.DELETE.getId(), deleteResourceAction);
        }
        if (moveResourceAction.isEnabled()) {
            actionBars.setGlobalActionHandler(ActionFactory.MOVE.getId(), moveResourceAction);
        }
    }

    /* (non-Javadoc)
     * @see org.eclipse.ui.actions.ActionGroup#fillContextMenu(org.eclipse.jface.action.IMenuManager)
     */
    @Override
    public void fillContextMenu(IMenuManager menu) {
        if (openResourceAction.isEnabledForSelectionWithoutContainers()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openResourceAction);
        }
        if (openPythonAction.isEnabledForSelectionWithoutContainers()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openPythonAction);
        }
        if (openExternalAction.isEnabledForSelectionWithoutContainers()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_OPEN, openExternalAction);
        }
        if (copyResourceAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, copyResourceAction);
        }
        if (pasteAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, pasteAction);
        }
        if (deleteResourceAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, deleteResourceAction);
        }
        if (moveResourceAction.isEnabled()) {
            menu.appendToGroup(ICommonMenuConstants.GROUP_EDIT, moveResourceAction);
        }
    }

}
