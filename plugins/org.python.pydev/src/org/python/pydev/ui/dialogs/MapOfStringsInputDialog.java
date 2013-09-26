/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.dialogs;

import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

/**
 * @author fabioz
 */
public abstract class MapOfStringsInputDialog extends AbstractMapOfStringsInputDialog {

    public MapOfStringsInputDialog(Shell shell, String dialogTitle, String dialogMessage, Map<String, String> map) {
        super(shell, dialogTitle, dialogMessage, map);
    }

    @Override
    protected String handleBrowseButton() {
        FileDialog dialog = new FileDialog(getShell(), SWT.OPEN);

        String file = dialog.open();
        return file;
    }

}
