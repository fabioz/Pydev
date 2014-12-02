/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.container;

import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.editor.actions.PyOrganizeImports;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * Action used to organize imports to all the available python files.
 *  
 * @author Jeremy J. Carroll
 */
public class PyOrganizeImportsAction extends PyContainerFormatterAction {

    public PyOrganizeImportsAction() {
        super("organize imports", "organize imports in", "organized");
    }

    @Override
    PyOrganizeImports createFormatter() {
        return new PyOrganizeImports();
    }

    @Override
    protected boolean confirmRun() {
        return

        super.confirmRun()
                //Note: ask for the platform, but preferences will follow settings in each project.
                && ((!ImportsPreferencesPage.getDeleteUnusedImports(null))
                ||
                MessageDialog
                        .openConfirm(
                                null,
                                "Confirm Deletion of Unused Imports",
                                "Your preferences show to delete unused imports (PyDev > Editor > Code Style > Imports)\n"
                                        + "\n"
                                        + "This requires that you have run the PyDev Code Analysis recently for correct behavior."));
    }
}
