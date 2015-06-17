/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.refactorer.search.copied;

import org.eclipse.jface.action.Action;
import org.eclipse.search.internal.ui.SearchPlugin;
import org.eclipse.search.internal.ui.SearchPreferencePage;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.python.pydev.shared_ui.search.SearchMessages;

/**
 * Opens the search preferences dialog
 */
public class OpenSearchPreferencesAction extends Action {
    public OpenSearchPreferencesAction() {
        super(SearchMessages.OpenSearchPreferencesAction_label);
        setToolTipText(SearchMessages.OpenSearchPreferencesAction_tooltip);
        //PlatformUI.getWorkbench().getHelpSystem().setHelp(this, IWorkbenchHelpContextIds.OPEN_PREFERENCES_ACTION);
    }

    /* (non-Javadoc)
     * Method declared on Action.
     */
    public void run() {
        Shell shell = SearchPlugin.getActiveWorkbenchShell();
        PreferencesUtil.createPreferenceDialogOn(shell, SearchPreferencePage.PAGE_ID, null, null).open();
    }

}
