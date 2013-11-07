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
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;

/**
 * Heler class to disable buttons on a dialog so that it doesn't get clicked accidentally (to be used on dialogs that
 * the user isn't expecting and that can appear while he's coding).
 *
 * @author Fabio
 */
public class DialogButtonEnablementHelper {

    private List<Button> buttons = new ArrayList<Button>(2);

    private boolean buttonsEnabled;

    public DialogButtonEnablementHelper(boolean buttonsEnabled) {
        this.buttonsEnabled = buttonsEnabled;
    }

    public void setButtonsEnabled(boolean b) {
        this.buttonsEnabled = b;
        for (Button bt : buttons) {
            bt.setEnabled(b);
        }
    }

    public void onCreateButton(Button button, int id) {
        if (id == IDialogConstants.OK_ID || id == IDialogConstants.CANCEL_ID) {
            buttons.add(button);
        }
    }

    public void onConstrainShellSize() {
        if (!buttonsEnabled) {
            setButtonsEnabled(false);
            Display.getCurrent().timerExec(2000, new Runnable() {

                @Override
                public void run() {
                    setButtonsEnabled(true);
                }
            });
        }
    }

    public boolean areButtonsEnabled() {
        return buttonsEnabled;
    }
}
