/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.console;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.ListResourceBundle;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.internal.console.IOConsolePage;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IUpdate;
import org.python.pydev.shared_interactive_console.console.ui.internal.ScriptConsoleViewer;

/**
 * Provides a way to leave the global handlers updated when we change the focus to our
 * own StyledText.
 */
@SuppressWarnings("restriction")
public class PromptOverlayReplaceGlobalActionHandlers {

    private final HashMap<String, IAction> newActions = new HashMap<>();

    private final FocusListener focusListener;

    private final ScriptConsoleViewer viewer;

    public PromptOverlayReplaceGlobalActionHandlers(final IOConsolePage consolePage, final ScriptConsoleViewer viewer) {
        this.viewer = viewer;

        final Map<String, IAction> old = new HashMap<>();

        TextViewerAction action = new TextViewerAction(viewer, ITextOperationTarget.SELECT_ALL);
        action.configureAction("Select &All", "Select All", "Select All");
        action.setActionDefinitionId(ActionFactory.SELECT_ALL.getCommandId());
        newActions.put(ActionFactory.SELECT_ALL.getId(), action);

        action = new TextViewerAction(viewer, ITextOperationTarget.COPY);
        action.configureAction("&Copy", "Copy", "Copy");
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_COPY));
        action.setActionDefinitionId(ActionFactory.COPY.getCommandId());
        newActions.put(ActionFactory.COPY.getId(), action);

        action = new TextViewerAction(viewer, ITextOperationTarget.PASTE);
        action.configureAction("&Paste", "Paste", "Paste");
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
        action.setActionDefinitionId(ActionFactory.PASTE.getCommandId());
        newActions.put(ActionFactory.PASTE.getId(), action);

        action = new TextViewerAction(viewer, ITextOperationTarget.CUT);
        action.configureAction("C&ut", "Cut", "Cut");
        action.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages()
                .getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
        action.setActionDefinitionId(ActionFactory.CUT.getCommandId());
        newActions.put(ActionFactory.CUT.getId(), action);

        ResourceBundle bundle = new ListResourceBundle() {

            @Override
            protected Object[][] getContents() {
                return new Object[0][0];
            }
        };
        FindReplaceAction findAction = new FindReplaceAction(bundle, "Editor.FindReplace.", viewer.getControl()
                .getShell(), viewer.getFindReplaceTarget());
        findAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
        newActions.put(ActionFactory.FIND.getId(), findAction);

        selectionDependentActionIds.add(ActionFactory.CUT.getId());
        selectionDependentActionIds.add(ActionFactory.COPY.getId());
        selectionDependentActionIds.add(ActionFactory.PASTE.getId());
        selectionDependentActionIds.add(ActionFactory.FIND.getId());

        this.focusListener = new FocusListener() {

            @Override
            public void focusLost(FocusEvent e) {
                if (old.size() == 0) {
                    return;
                }
                IPageSite site = consolePage.getSite();
                final IActionBars actionBars = site.getActionBars();
                site.setSelectionProvider(consolePage.getViewer());
                viewer.getSelectionProvider().removeSelectionChangedListener(selectionChangedListener);

                //Restore old ones
                for (Entry<String, IAction> oldEntry : old.entrySet()) {
                    String actionId = oldEntry.getKey();
                    actionBars.setGlobalActionHandler(actionId, oldEntry.getValue());
                }
                old.clear();
                actionBars.updateActionBars();
            }

            @Override
            public void focusGained(FocusEvent e) {
                if (old.size() > 0) {
                    return;
                }

                IPageSite site = consolePage.getSite();
                //site.registerContextMenu(id, fMenuManager, fViewer);
                site.setSelectionProvider(viewer);
                viewer.getSelectionProvider().addSelectionChangedListener(selectionChangedListener);

                final IActionBars actionBars = site.getActionBars();
                //Store old ones and set new ones
                for (Entry<String, IAction> entry : newActions.entrySet()) {
                    String actionId = entry.getKey();
                    IAction globalActionHandler = actionBars.getGlobalActionHandler(actionId);
                    old.put(actionId, globalActionHandler);
                    actionBars.setGlobalActionHandler(actionId, entry.getValue());
                }
                actionBars.updateActionBars();
            }
        };
        viewer.getTextWidget().addFocusListener(focusListener);
    }

    // text selection listener, used to update selection dependent actions on selection changes
    private ISelectionChangedListener selectionChangedListener = new ISelectionChangedListener() {
        @Override
        public void selectionChanged(SelectionChangedEvent event) {
            updateSelectionDependentActions();
        }
    };

    protected ArrayList<String> selectionDependentActionIds = new ArrayList<String>();

    protected void updateSelectionDependentActions() {
        for (String string : selectionDependentActionIds) {
            IAction action = newActions.get(string);
            if (action instanceof IUpdate) {
                ((IUpdate) action).update();
            }
        }
    }

    public void dispose() {
        StyledText textWidget = viewer.getTextWidget();
        if (textWidget != null && !textWidget.isDisposed()) {
            textWidget.removeFocusListener(focusListener);
        }

    }
}
