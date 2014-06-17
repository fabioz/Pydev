/**
 * Copyright (c) 2014 by Brainwy Software. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IAction;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.debug.ui.launching.AbstractLaunchShortcut;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.structure.Tuple;

public class DebugEditorBasedOnNatureTypeAction extends AbstractRunEditorAction {

    public void run(IAction action) {

        PyEdit pyEdit = getPyEdit();
        final Tuple<String, IInterpreterManager> launchConfigurationTypeAndInterpreterManager = this
                .getLaunchConfigurationTypeAndInterpreterManager(pyEdit, false);

        AbstractLaunchShortcut shortcut = new AbstractLaunchShortcut() {

            @Override
            protected String getLaunchConfigurationType() {
                return launchConfigurationTypeAndInterpreterManager.o1;
            }

            @Override
            protected IInterpreterManager getInterpreterManager(IProject project) {
                return launchConfigurationTypeAndInterpreterManager.o2;
            }

        };
        shortcut.launch(pyEdit, "debug");
    }

}
