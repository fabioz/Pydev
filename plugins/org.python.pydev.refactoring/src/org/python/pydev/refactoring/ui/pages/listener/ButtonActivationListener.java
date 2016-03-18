/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ui.pages.listener;

import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;

public class ButtonActivationListener implements Listener {

    private Table table;

    private Button upButton;

    private Button downButton;

    private Button editButton;

    public ButtonActivationListener(Table table, Button upButton, Button downButton, Button editButton) {
        this.table = table;
        this.upButton = upButton;
        this.downButton = downButton;
        this.editButton = editButton;
    }

    @Override
    public void handleEvent(Event event) {
        updateButtonState();
    }

    private void updateButtonState() {
        editButton.setEnabled(false);
        upButton.setEnabled(true);
        downButton.setEnabled(true);
        if (table.getSelectionCount() == 1) {
            editButton.setEnabled(true);
            if (table.getSelectionIndex() == table.getItemCount() - 1) {
                downButton.setEnabled(false);
            }

            if (table.getSelectionIndex() == 0) {
                upButton.setEnabled(false);
            }
        }

    }

}
