/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.python.pydev.core.log.Log;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Creates the selection dialog to be used to select a token.
 * 
 * @author Fabio
 */
public class GlobalsDialogFactory {

    /**
     * Creates the dialog according to the Eclipse version we have (on 3.2, the old API is used)
     * @param pythonNatures 
     */
    public static SelectionDialog create(Shell shell, List<AbstractAdditionalTokensInfo> additionalInfo,
            String selectedText) {
        boolean expectedError = true;
        try {
            GlobalsTwoPanelElementSelector2 newDialog = new GlobalsTwoPanelElementSelector2(shell, true, selectedText);
            //If we were able to instance it, the error is no longer expected!
            expectedError = false;

            newDialog.setElements(additionalInfo);
            return newDialog;
        } catch (Throwable e) {
            //That's OK: it's only available for Eclipse 3.3 onwards.
            if (expectedError) {
                Log.log(e);
            }
        }

        //If it got here, we were unable to create the new dialog (show the old -- compatible with 3.2)
        GlobalsTwoPaneElementSelector dialog;
        dialog = new GlobalsTwoPaneElementSelector(shell);
        dialog.setMessage("Filter");
        if (selectedText != null && selectedText.length() > 0) {
            dialog.setFilter(selectedText);
        }

        List<IInfo> lst = new ArrayList<IInfo>();

        for (AbstractAdditionalTokensInfo info : additionalInfo) {
            lst.addAll(info.getAllTokens());
        }

        dialog.setElements(lst.toArray());
        return dialog;
    }

}
