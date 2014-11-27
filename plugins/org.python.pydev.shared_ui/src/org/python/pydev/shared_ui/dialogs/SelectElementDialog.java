/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.dialogs;

import java.util.List;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.python.pydev.shared_ui.SharedUiPlugin;

public class SelectElementDialog extends ElementListSelectionDialog {

    private static final String DIALOG_SETTINGS = "org.python.pydev.shared_ui.dialogs.SelectElementDialog"; //$NON-NLS-1$;

    private final IDialogSettings dialogSettings;

    /**
     * renderer: Usually a org.eclipse.jface.viewers.LabelProvider subclass.
     */
    public SelectElementDialog(Shell parent, ILabelProvider renderer) {
        super(parent, renderer);
        dialogSettings = SharedUiPlugin.getDefault().getDialogSettings();

    }

    //override things to return the last position of the dialog correctly

    @Override
    protected Control createContents(Composite parent) {
        Control ret = super.createContents(parent);
        SharedUiPlugin.setCssId(parent, "py-select-dialog", true);
        return ret;
    }

    @Override
    public boolean isHelpAvailable() {
        return false;
    }

    @Override
    protected void updateStatus(IStatus status) {
        super.updateStatus(status);
        Control area = this.getDialogArea();
        if (area != null) {
            SharedUiPlugin.fixSelectionStatusDialogStatusLineColor(this, area.getBackground());
        }
    }

    /**
     * @see org.eclipse.ui.dialogs.SelectionDialog#getDialogBoundsSettings()
     */
    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        IDialogSettings section = dialogSettings.getSection(DIALOG_SETTINGS);
        if (section == null) {
            section = dialogSettings.addNewSection(DIALOG_SETTINGS);
        }
        return section;
    }

    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#getInitialSize()
     */
    @Override
    protected Point getInitialSize() {
        IDialogSettings settings = getDialogBoundsSettings();
        if (settings != null) {
            try {
                int width = settings.getInt("DIALOG_WIDTH"); //$NON-NLS-1$
                int height = settings.getInt("DIALOG_HEIGHT"); //$NON-NLS-1$
                if (width > 0 & height > 0) {
                    return new Point(width, height);
                }
            } catch (NumberFormatException nfe) {
                //make the default return
            }
        }
        return new Point(300, 300);
    }

    public static String selectOne(List<String> items, LabelProvider labelProvider, String message) {
        Shell activeShell = Display.getCurrent().getActiveShell();

        SelectElementDialog dialog = new SelectElementDialog(activeShell, labelProvider);
        dialog.setTitle("Select One");
        dialog.setMessage(message);
        dialog.setElements(items.toArray());
        dialog.setMultipleSelection(false);

        int returnCode = dialog.open();
        if (returnCode == Window.OK) {
            return (String) dialog.getFirstResult();
        }
        return null;

    }
};
