/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.nature;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.log.Log;
import org.python.pydev.ui.actions.project.PyAddNature;


public class DjangoAddNatureAction extends PyAddNature {

    @Override
    public void run(IAction action) {
        if (selectedProject == null) {
            return;
        }

        try {
            DjangoNature.addNature(selectedProject, null);
        } catch (CoreException e) {
            Log.log(e);
        }
        //TODO: Set the manage.py location if not set.
    }

}
