/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.python.pydev.navigator.ui.PydevPackageExplorer;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This action setups the custom filters that are available for the pydev package explorer.
 *
 * @author Fabio
 */
public class PySetupCustomFilters extends Action implements IViewActionDelegate {

    public static final String CUSTOM_FILTERS_PREFERENCE_NAME = "org.python.pydev.CUSTOM_PACKAGE_EXPLORER_FILTERS";
    private IViewPart view;

    @Override
    public void init(IViewPart view) {
        this.view = view;
    }

    @Override
    public void run(IAction action) {
        final Display display = Display.getDefault();
        display.syncExec(new Runnable() {

            @Override
            public void run() {

                //ask the filters to the user
                IInputValidator validator = null;

                IPreferenceStore prefs = PydevPlugin.getDefault().getPreferenceStore();
                InputDialog dialog = new InputDialog(display.getActiveShell(), "Custom Filters",

                "Enter the filters (separated by comma. E.g.: \"__init__.py, *.xyz\").\n" + "\n"
                        + "Note 1: Only * and ? may be used for custom matching.\n" + "\n"
                        + "Note 2: it'll only take effect if the 'Pydev: Hide custom specified filters'\n"
                        + "is active in the menu: customize view > filters.",

                prefs.getString(CUSTOM_FILTERS_PREFERENCE_NAME), validator);

                dialog.setBlockOnOpen(true);
                dialog.open();
                if (dialog.getReturnCode() == Window.OK) {
                    //update the preferences and refresh the viewer (when we update the preferences, the 
                    //filter that uses this will promptly update its values -- just before the refresh).
                    prefs.setValue(CUSTOM_FILTERS_PREFERENCE_NAME, dialog.getValue());
                    PydevPackageExplorer p = (PydevPackageExplorer) view;
                    p.getCommonViewer().refresh();
                }
            }
        });
    }

    @Override
    public void selectionChanged(IAction action, ISelection selection) {
        //nothing to do here
    }

}
