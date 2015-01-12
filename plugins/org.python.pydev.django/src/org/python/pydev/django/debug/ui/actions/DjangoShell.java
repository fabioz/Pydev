/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django.debug.ui.actions;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.core.PydevDebugPlugin;
import org.python.pydev.debug.newconsole.PydevConsoleConstants;
import org.python.pydev.debug.newconsole.PydevConsoleFactory;
import org.python.pydev.debug.newconsole.PydevConsoleInterpreter;
import org.python.pydev.debug.newconsole.env.PydevIProcessFactory;
import org.python.pydev.debug.newconsole.env.PydevIProcessFactory.PydevConsoleLaunchInfo;
import org.python.pydev.django.launching.DjangoConstants;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_ui.EditorUtils;

public class DjangoShell extends DjangoAction {

    @Override
    public void run(IAction action) {
        try {
            //   		 this.launchDjangoCommand("shell", false);

            PythonNature nature = PythonNature.getPythonNature(selectedProject);
            if (nature == null) {
                MessageDialog.openError(EditorUtils.getShell(), "PyDev nature not found",
                        "Unable to perform action because the Pydev nature is not properly set.");
                return;
            }
            IPythonPathNature pythonPathNature = nature.getPythonPathNature();
            String settingsModule = null;
            Map<String, String> variableSubstitution = null;
            try {
                variableSubstitution = pythonPathNature.getVariableSubstitution();
                settingsModule = variableSubstitution.get(DjangoConstants.DJANGO_SETTINGS_MODULE);
            } catch (Exception e1) {
                throw new RuntimeException(e1);
            }
            if (settingsModule == null) {
                InputDialog d = new InputDialog(EditorUtils.getShell(), "Settings module",
                        "Please enter the settings module to be used.\n" + "\n"
                                + "Note that it can be edited later in:\np"
                                + "roject properties > pydev pythonpath > string substitution variables.",
                        selectedProject.getName() + ".settings", new IInputValidator() {

                            public String isValid(String newText) {
                                if (newText.length() == 0) {
                                    return "Text must be entered.";
                                }
                                for (char c : newText.toCharArray()) {
                                    if (c == ' ') {
                                        return "Whitespaces not accepted";
                                    }
                                    if (c != '.' && !Character.isJavaIdentifierPart(c)) {
                                        return "Invalid char: " + c;
                                    }
                                }
                                return null;
                            }
                        });

                int retCode = d.open();

                if (retCode == InputDialog.OK) {
                    settingsModule = d.getValue();
                    variableSubstitution.put(DjangoConstants.DJANGO_SETTINGS_MODULE, settingsModule);
                    try {
                        pythonPathNature.setVariableSubstitution(variableSubstitution);
                    } catch (Exception e) {
                        Log.log(e);
                    }

                }

                if (settingsModule == null) {
                    return;
                }
            }

            List<IPythonNature> natures = Collections.singletonList((IPythonNature) nature);
            PydevConsoleFactory consoleFactory = new PydevConsoleFactory();
            PydevConsoleLaunchInfo launchInfo = new PydevIProcessFactory().createLaunch(
                    nature.getRelatedInterpreterManager(),
                    nature.getProjectInterpreter(),
                    nature.getPythonPathNature().getCompleteProjectPythonPath(nature.getProjectInterpreter(),
                            nature.getRelatedInterpreterManager()), nature, natures);

            PydevConsoleInterpreter interpreter = PydevConsoleFactory.createPydevInterpreter(launchInfo, natures,
                    launchInfo.encoding);

            String djangoAdditionalCommands = PydevDebugPlugin.getDefault().getPreferenceStore().
                    getString(PydevConsoleConstants.DJANGO_INTERPRETER_CMDS);

            djangoAdditionalCommands = djangoAdditionalCommands.replace("${"
                    + DjangoConstants.DJANGO_SETTINGS_MODULE + "}", settingsModule);

            //os.environ.setdefault("DJANGO_SETTINGS_MODULE", "fooproject.settings")
            consoleFactory.createConsole(interpreter, djangoAdditionalCommands);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
