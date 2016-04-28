/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import java.lang.ref.WeakReference;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.UIConstants;

public class HistoryAction extends Action {

    public static interface IActionsMenu {

        void add(IAction action);

    }

    public class HistoryMenuCreator implements IMenuCreator {

        private Menu fMenu;

        public HistoryMenuCreator() {

        }

        @Override
        public void dispose() {
            if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
            }
        }

        @Override
        public Menu getMenu(Control parent) {
            if (fMenu != null) {
                fMenu.dispose();
            }

            final MenuManager manager = new MenuManager();
            manager.setRemoveAllWhenShown(true);
            manager.addMenuListener(new IMenuListener() {
                @Override
                public void menuAboutToShow(final IMenuManager manager2) {
                    fillMenuManager(new IActionsMenu() {

                        @Override
                        public void add(IAction action) {
                            manager2.add(action);

                        }
                    });
                }
            });
            fMenu = manager.createContextMenu(parent);

            return fMenu;
        }

        @Override
        public Menu getMenu(Menu parent) {
            return null; //yes, return null here (no sub children)
        }

        public void fillMenuManager(IActionsMenu actionsMenu) {
            if (view == null) {
                return;
            }
            PyUnitView pyUnitView = view.get();
            if (pyUnitView == null) {
                return;
            }
            PyUnitTestRun currentTestRun = pyUnitView.getCurrentTestRun();
            List<PyUnitTestRun> allTestRuns = pyUnitView.getAllTestRuns();

            PyUnitTestRun lastPinned = PyUnitViewTestsHolder.getLastPinned();
            if (lastPinned != null) {
                SetCurrentRunAction runAction = new SetCurrentRunAction(view, lastPinned);
                runAction.setChecked(false);
                runAction.setText("Last Pinned: " + lastPinned.getShortDescription());
                actionsMenu.add(runAction);
            }

            for (PyUnitTestRun pyUnitTestRun : allTestRuns) {
                SetCurrentRunAction runAction = new SetCurrentRunAction(view, pyUnitTestRun);
                runAction.setChecked(pyUnitTestRun == currentTestRun);
                runAction.setText(pyUnitTestRun.getShortDescription());
                actionsMenu.add(runAction);
            }
            actionsMenu.add(new ClearTerminatedAction(view));
            actionsMenu.add(new ExportCurrentToClipboardAction(view));
            actionsMenu.add(new RestoreFromClipboardAction(view));
        }
    }

    private WeakReference<PyUnitView> view;

    public HistoryAction(PyUnitView view) {
        this.view = new WeakReference<PyUnitView>(view);
        setMenuCreator(new HistoryMenuCreator());
        setToolTipText("Test Run History");
        this.setImageDescriptor(PydevPlugin.getImageCache().getDescriptor(UIConstants.HISTORY));
    }
}
