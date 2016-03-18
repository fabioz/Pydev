/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui.actions.project;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;


/**
 * Adds a nature to the given selected project.
 * 
 * @author Fabio
 */
public class PyAddNature extends PyRemoveNature {

    @Override
    public void run(IAction action) {
        if (selectedProject == null) {
            return;
        }

        try {
            PythonNature.addNature(selectedProject, null, null, null, null, null, null);
        } catch (CoreException e) {
            Log.log(e);
        }
    }

}
