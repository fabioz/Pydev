/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import java.lang.ref.WeakReference;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Action used to force the orientation (automatic, horizontal or vertical).
 * 
 * @author fabioz
 */
public class SetOrientationAction extends Action {

    public class SetOrientationActionImpl extends Action {

        private ViewPartWithOrientation viewPartWithOrientation;
        private int setsValue;

        public SetOrientationActionImpl(ViewPartWithOrientation viewPartWithOrientation, String text, int setsValue) {
            this.setText(text);
            this.viewPartWithOrientation = viewPartWithOrientation;
            this.setsValue = setsValue;
            this.setChecked(this.setsValue == viewPartWithOrientation.getOrientationPreferenceValue());
        }

        @Override
        public void run() {
            PydevPrefs.getPreferenceStore().setValue(viewPartWithOrientation.getOrientationPreferencesKey(),
                    this.setsValue);
        }

    }

    public static interface IActionsMenu {

        void add(IAction action);

    }

    /**
     * Yeap, all this just to show the items 'Automatic', 'Horizontal' and 'Vertical' under the Orientation menu.
     * 
     * @author fabioz
     */
    public class SetOrientationMenuCreator implements IMenuCreator {

        private Menu fMenu;

        public SetOrientationMenuCreator() {

        }

        public void dispose() {
            if (fMenu != null) {
                fMenu.dispose();
                fMenu = null;
            }
        }

        public Menu getMenu(Control parent) {
            return null; //not used!
        }

        public Menu getMenu(Menu parent) {
            if (fMenu != null) {
                fMenu.dispose();
            }

            final MenuManagerCopiedToAddCreateMenuWithMenuParent manager = new MenuManagerCopiedToAddCreateMenuWithMenuParent();
            manager.setRemoveAllWhenShown(true);
            manager.addMenuListener(new IMenuListener() {
                public void menuAboutToShow(final IMenuManager manager2) {
                    fillMenuManager(new IActionsMenu() {

                        public void add(IAction action) {
                            manager2.add(action);
                        }
                    });
                }
            });
            fMenu = manager.createContextMenu(parent);

            return fMenu;

        }

        public void fillMenuManager(IActionsMenu actionsMenu) {
            if (view == null) {
                return;
            }
            ViewPartWithOrientation viewPartWithOrientation = view.get();
            if (viewPartWithOrientation == null) {
                return;
            }
            actionsMenu.add(new SetOrientationActionImpl(viewPartWithOrientation, "Automatic",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_AUTOMATIC));
            actionsMenu.add(new SetOrientationActionImpl(viewPartWithOrientation, "Horizontal",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_HORIZONTAL));
            actionsMenu.add(new SetOrientationActionImpl(viewPartWithOrientation, "Vertical",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_VERTICAL));
        }
    }

    private WeakReference<ViewPartWithOrientation> view;

    /**
     * This is the root action (Orientation).
     */
    public SetOrientationAction(ViewPartWithOrientation view) {
        this.view = new WeakReference<ViewPartWithOrientation>(view);
        setMenuCreator(new SetOrientationMenuCreator());
        this.setText("Orientation");
        setToolTipText("Update orientation");
    }
}
