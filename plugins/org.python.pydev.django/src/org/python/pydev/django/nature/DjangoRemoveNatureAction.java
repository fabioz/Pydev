/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.nature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.ui.actions.project.PyRemoveNature;

public class DjangoRemoveNatureAction extends PyRemoveNature {

    @Override
    public void run(IAction action) {
        if (selectedProject == null) {
            return;
        }

        if (!MessageDialog.openConfirm(
                null,
                "Confirm Remove Django Nature",
                StringUtils.format("Are you sure that you want to remove the Django nature from %s?",
                        selectedProject.getName()))) {
            return;
        }

        try {
            DjangoNature.removeNature(selectedProject, null);
        } catch (CoreException e) {
            Log.log(e);
        }
    }

}
