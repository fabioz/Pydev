/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.python.pydev.editor.IOfflineActionWithParameters;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;

/**
 * This action will pass the execution to an actual django command depending on the parameters.
 * 
 * If no parameters, it will use the custom command (which asks for parameters). 
 * For shell, our custom shell is used.
 * For any others, they're passed directly to manage.py.
 */
public class PyDjangoOfflineAction extends Action implements IOfflineActionWithParameters {

    private List<String> parameters;
    private PyEdit edit;

    public PyDjangoOfflineAction(PyEdit edit) {
        this.edit = edit;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        DjangoAction djangoAction = null;
        if (parameters.size() == 0) {
            //if no parameters were passed, use the custom to ask the user the action.
            djangoAction = new DjangoCustomCommand();

        } else {
            if (parameters.size() == 1) {
                String parameter = parameters.get(0);
                if ("shell".equals(parameter)) {
                    djangoAction = new DjangoShell();
                }
            }

            if (djangoAction == null) {
                djangoAction = new DjangoAction() {

                    @Override
                    public void run(IAction action) {
                        launchDjangoCommand(StringUtils.join(" ", parameters), true);
                    }
                };
            }
        }

        if (djangoAction != null) {
            djangoAction.setSelectedProject(edit.getProject());
            djangoAction.run(this);
        }
    }

}
