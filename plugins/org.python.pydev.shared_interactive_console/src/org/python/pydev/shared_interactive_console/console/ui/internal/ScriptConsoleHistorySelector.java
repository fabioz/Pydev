/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console.ui.internal;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.shared_interactive_console.console.ScriptConsoleHistory;
import org.python.pydev.shared_interactive_console.console.ui.internal.fromeclipse.HistoryElementListSelectionDialog;

/**
 * Helper class to select one element from the history
 *
 * @author Fabio
 */
public class ScriptConsoleHistorySelector {

    /**
     * Selects a list of strings from a list of strings
     * 
     * @param history the history that may be selected for execution
     * @return null if none was selected or a list of strings with the commands to be executed
     */
    public static List<String> select(final ScriptConsoleHistory history) {

        //created the HistoryElementListSelectionDialog instead of using the ElementListSelectionDialog directly because:
        //1. No sorting should be enabled for choosing the history
        //2. The last element should be the one selected by default
        //3. The list should be below the commands
        //4. The up arrow should be the one used to get focus in the elements
        HistoryElementListSelectionDialog dialog = new HistoryElementListSelectionDialog(Display.getDefault()
                .getActiveShell(), getLabelProvider()) {
            private static final int CLEAR_HISTORY_ID = IDialogConstants.CLIENT_ID + 1;

            @Override
            protected void createButtonsForButtonBar(Composite parent) {
                super.createButtonsForButtonBar(parent);
                createButton(parent, CLEAR_HISTORY_ID, "Clear History", false); //$NON-NLS-1$
            }

            @Override
            protected void buttonPressed(int buttonId) {
                if (buttonId == CLEAR_HISTORY_ID) {
                    history.clear();

                    // After deleting the history, close the dialog with a cancel return code
                    cancelPressed();
                }
                super.buttonPressed(buttonId);
            }
        };

        dialog.setTitle("Command history");
        final List<String> selectFrom = history.getAsList();
        dialog.setElements(selectFrom.toArray(new String[0]));
        dialog.setEmptySelectionMessage("No command selected");
        dialog.setAllowDuplicates(true);
        dialog.setBlockOnOpen(true);
        dialog.setSize(100, 25); //in number of chars
        dialog.setMessage("Select command(s) to be executed");
        dialog.setMultipleSelection(true);

        if (dialog.open() == SelectionDialog.OK) {
            Object[] result = dialog.getResult();
            if (result != null) {
                ArrayList<String> list = new ArrayList<String>();
                for (Object o : result) {
                    list.add(o.toString());
                }
                return list;
            }
        }
        return null;
    }

    /**
     * @return a label provider that'll show the history command as a string
     */
    private static ILabelProvider getLabelProvider() {
        return new ILabelProvider() {

            @Override
            public Image getImage(Object element) {
                return null;
            }

            @Override
            public String getText(Object element) {
                return element.toString();
            }

            @Override
            public void addListener(ILabelProviderListener listener) {
            }

            @Override
            public void dispose() {
            }

            @Override
            public boolean isLabelProperty(Object element, String property) {
                return true;
            }

            @Override
            public void removeListener(ILabelProviderListener listener) {
            }
        };
    }

}
