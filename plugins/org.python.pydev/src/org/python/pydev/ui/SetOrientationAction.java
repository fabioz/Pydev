/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
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
            updateCheck();
        }

        public void updateCheck() {
            boolean check = this.setsValue == viewPartWithOrientation.getOrientationPreferenceValue();
            this.setChecked(check);
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
        private List<ActionContributionItem> fActions;

        public SetOrientationMenuCreator() {

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
            return null; //not used!
        }

        @Override
        public Menu getMenu(Menu parent) {
            dispose(); // dispose if already there.

            fMenu = new Menu(parent);

            fMenu.addMenuListener(new MenuListener() {

                @Override
                public void menuShown(MenuEvent e) {
                    List<ActionContributionItem> lst = fActions;
                    int len = lst.size();
                    for (int i = 0; i < len; i++) {
                        ActionContributionItem actionContributionItem = lst.get(i);
                        SetOrientationActionImpl action = (SetOrientationActionImpl) actionContributionItem.getAction();
                        action.updateCheck();
                    }
                }

                @Override
                public void menuHidden(MenuEvent e) {
                }
            });
            if (view == null) {
                return fMenu;
            }
            ViewPartWithOrientation viewPartWithOrientation = view.get();
            if (viewPartWithOrientation == null) {
                return fMenu;
            }
            ArrayList<ActionContributionItem> lst = new ArrayList<ActionContributionItem>();
            ActionContributionItem item = new ActionContributionItem(new SetOrientationActionImpl(
                    viewPartWithOrientation, "Automatic",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_AUTOMATIC));
            lst.add(item);

            item = new ActionContributionItem(new SetOrientationActionImpl(viewPartWithOrientation, "Horizontal",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_HORIZONTAL));
            lst.add(item);

            item = new ActionContributionItem(new SetOrientationActionImpl(viewPartWithOrientation, "Vertical",
                    ViewPartWithOrientation.PREFERENCES_VIEW_ORIENTATION_VERTICAL));
            lst.add(item);

            fActions = lst;
            int len = lst.size();
            for (int i = 0; i < len; i++) {
                lst.get(i).fill(fMenu, i);
            }

            return fMenu;

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
