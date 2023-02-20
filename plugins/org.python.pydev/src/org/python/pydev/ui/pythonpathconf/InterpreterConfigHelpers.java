/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on August 20, 2013
 *
 * @author Andrew Ferrazzutti
 */

package org.python.pydev.ui.pythonpathconf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.ast.runners.SimpleJythonRunner;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.plugin.nature.PipenvHelper;
import org.python.pydev.plugin.nature.SystemPythonNature;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.package_manager.PipenvPackageManager;

/**
 * Contains some static methods to be used for configuring PyDev interpreters.
 * (Private code from {@link AbstractInterpreterEditor} was moved here & made
 * static so as to be usable without needing an editor.)
 *
 * @author Andrew Ferrazzutti
 */
public class InterpreterConfigHelpers {

    public final static int CONFIG_MANUAL = 0;
    public final static int CONFIG_AUTO = 1;
    public final static int CONFIG_ADV_AUTO = 2;
    public final static int CONFIG_PIPENV = 3;
    public final static int CONFIG_CONDA = 4;

    public static final String CONFIG_MANUAL_CONFIG = "Manual &config";
    public static final String CONFIG_AUTO_NAME = "Config &first in PATH";
    public static final String CONFIG_ADV_AUTO_NAME = "Choose from &list";
    public static final String CONFIG_PIPENV_NAME = "&New with Pipenv";
    public static final String CONFIG_CONDA_NAME = "Choose from &Conda";

    public final static String[] CONFIG_NAMES_FOR_FIRST_INTERPRETER = new String[] { CONFIG_MANUAL_CONFIG,
            CONFIG_AUTO_NAME,
            CONFIG_ADV_AUTO_NAME }; // Note: pipenv requires a base interpreter configured and thus doesn't appear here.

    public final static String ERMSG_NOLIBS = "The interpreter's standard libraries (typically in a Lib/ folder) are missing: ";

    static ObtainInterpreterInfoOperation tryInterpreter(NameAndExecutable interpreterNameAndExecutable,
            IInterpreterManager interpreterManager, boolean autoSelectFolders, boolean displayErrors,
            PrintWriter logger, Shell shell) throws Exception {
        String executableOrJar = interpreterNameAndExecutable.getExecutableOrJar();
        File file = new File(executableOrJar);
        boolean isConda = false;

        try {
            isConda = new File(file.getParentFile(), "conda-meta").exists();
        } catch (Exception e) {
            Log.log(e);
        }
        return tryInterpreter(interpreterNameAndExecutable, interpreterManager, autoSelectFolders, displayErrors,
                logger, shell, isConda);
    }

    /**
     * Attempts to set up a provided interpreter.
     *
     * @param interpreterNameAndExecutable Information pertaining to the interpreter to prepare.
     * @param interpreterManager
     * @param autoSelectFolders If true, folders will be automatically added to the SYSTEM pythonpath.
     * Otherwise, they must be selected manually with a dialog.
     * @param displayErrors Set to true to display an error dialog on failure, or false to fail silently.
     * @param logger
     * @param shell A mandatory shell in which to display progress and errors.
     * @return The interpreter config operation, or <code>null</code> if the operation is cancelled.
     * @throws Exception Will be thrown if an operation fails.
     */
    static ObtainInterpreterInfoOperation tryInterpreter(NameAndExecutable interpreterNameAndExecutable,
            IInterpreterManager interpreterManager, boolean autoSelectFolders, boolean displayErrors,
            PrintWriter logger, Shell shell, boolean isConda) throws Exception {
        String executable = interpreterNameAndExecutable.o2;
        logger.println("- Ok, file is non-null. Getting info on:" + executable);
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(shell);
        monitorDialog.setBlockOnOpen(false);
        ObtainInterpreterInfoOperation operation;
        while (true) {
            operation = new ObtainInterpreterInfoOperation(interpreterNameAndExecutable.o2, logger,
                    interpreterManager, autoSelectFolders, isConda);
            monitorDialog.run(true, false, operation);
            if (operation.e != null) {
                logger.println("- Some error happened while getting info on the interpreter:");
                operation.e.printStackTrace(logger);
                String errorTitle = "Unable to get info on the interpreter: " + executable;

                if (operation.e instanceof SimpleJythonRunner.JavaNotConfiguredException) {
                    SimpleJythonRunner.JavaNotConfiguredException javaNotConfiguredException = (SimpleJythonRunner.JavaNotConfiguredException) operation.e;
                    if (displayErrors) {
                        ErrorDialog.openError(shell, errorTitle,
                                javaNotConfiguredException.getMessage(), SharedCorePlugin.makeStatus(IStatus.ERROR,
                                        "Java vm not configured.\n", javaNotConfiguredException));
                    }
                    throw new Exception(javaNotConfiguredException);

                } else if (operation.e instanceof JDTNotAvailableException) {
                    JDTNotAvailableException noJdtException = (JDTNotAvailableException) operation.e;
                    if (displayErrors) {
                        ErrorDialog.openError(shell, errorTitle,
                                noJdtException.getMessage(),
                                SharedCorePlugin.makeStatus(IStatus.ERROR, "JDT not available.\n", noJdtException));
                    }
                    throw new Exception(noJdtException);

                } else {
                    if (displayErrors) {
                        //show the user a message (so that it does not fail silently)...
                        String errorMsg = "Unable to get info on the interpreter: " + executable
                                + "\n\n"
                                + "Common reasons include:\n\n" + "- Using an unsupported version\n"
                                + "  (Python and Jython require at least version 2.1 and IronPython 2.6).\n"
                                + "\n" + "- Specifying an invalid interpreter\n"
                                + "  (usually a link to the actual interpreter on Mac or Linux)" + "";
                        ErrorDialog.openError(shell, errorTitle,
                                errorMsg, SharedCorePlugin.makeStatus(IStatus.ERROR, "See error log for details.",
                                        operation.e));
                    }
                    throw new Exception(operation.e);
                }

            } else if (operation.result == null) {
                //Folder selection was canceled, exit
                return null;
            }

            //Ok, we got the result, so, let's check if things are correct (i.e.: do we have threading.py, traceback.py?)
            HashSet<String> hashSet = new HashSet<String>();
            hashSet.add("threading");
            hashSet.add("traceback");

            String[] validSourceFiles = FileTypesPreferences.getValidSourceFiles();
            Set<String> extensions = new HashSet<String>(Arrays.asList(validSourceFiles));
            for (String s : operation.result.libs) {
                File file = new File(s);
                if (file.isDirectory()) {
                    String[] directoryFiles = file.list();
                    if (directoryFiles != null) {
                        for (String found : directoryFiles) {
                            List<String> split = StringUtils.split(found, '.');
                            if (split.size() == 2) {
                                if (extensions.contains(split.get(1))) {
                                    hashSet.remove(split.get(0));
                                }
                            }
                        }
                    } else {
                        logger.append("Warning: unable to get contents of directory: "
                                + file
                                + " (permission not available, it's not a dir or dir does not exist).");
                    }
                } else if (file.isFile()) {
                    //Zip file?
                    try {
                        try (ZipFile zipFile = new ZipFile(file)) {
                            for (String extension : validSourceFiles) {
                                if (zipFile.getEntry("threading." + extension) != null) {
                                    hashSet.remove("threading");
                                }
                                if (zipFile.getEntry("traceback." + extension) != null) {
                                    hashSet.remove("traceback");
                                }
                            }
                        }
                    } catch (Exception e) {
                        //ignore (not zip file)
                    }
                }
            }

            if (hashSet.size() > 0) {
                if (displayErrors) {
                    //The /Lib folder wasn't there (or at least threading.py and traceback.py weren't found)
                    int choice = PyDialogHelpers
                            .openCriticalWithChoices(
                                    "Error: Python stdlib source files not found.",
                                    "Error: Python stdlib not found or stdlib found without .py files.\n"
                                            + "\n"
                                            + "It seems that the Python /Lib folder (which contains the standard library) "
                                            + "was not found/selected during the install process or the stdlib does not contain "
                                            + "the required .py files (i.e.: only has .pyc files).\n"
                                            + "\n"
                                            + "This folder (which contains files such as threading.py and traceback.py) is "
                                            + "required for PyDev to function properly, and it must contain the actual source files, not "
                                            + "only .pyc files. if you don't have the .py files in your install, please use an install from "
                                            + "python.org or grab the standard library for your install from there.\n"
                                            + "\n"
                                            + "If this is a virtualenv install, the /Lib folder from the base install needs to be selected "
                                            + "(unlike the site-packages which is optional).\n"
                                            + "\n"
                                            + "What do you want to do?\n\n"
                                            + "Note: if you choose to proceed, the /Lib with the standard library .py source files must "
                                            + "be added later on, otherwise PyDev may not function properly.",
                                    new String[] { "Re-select folders", "Cancel", "Proceed anyways" });
                    if (choice == 0) {
                        //Keep on with outer while(true)
                        continue;
                    }
                    if (choice == 1) {
                        //Return nothing and exit quietly on a cancel
                        return null;
                    }
                } else {
                    //Don't allow auto-selection of an interpreter missing these folders
                    logger.println("- Could not find /Lib folder, exiting with error.");
                    throw new Exception(ERMSG_NOLIBS + executable);
                }
            }
            operation.result.setName(interpreterNameAndExecutable.o1);
            logger.println("- Success getting the info. Result:" + operation.result);
            return operation;
        }
    }

    /**
     * Performs various error checks on a given interpreter.
     * @param interpreterNameAndExecutable The name & executable of the interpreter to check for correctness.
     * @param logger
     * @param errorMsg An error message to display if an error does occur.
     * @param nameToInfo A map of names as keys to the IInterpreterInfos of existing interpreters. Set
     * to null if no other interpreters exist at the time of the configuration attempt.
     * @param shell An optional shell in which to display errors.
     * @return <code>true</code> if the interpreter is valid, or <code>false</code> if it caused an error.
     */
    static boolean checkInterpreterNameAndExecutable(NameAndExecutable interpreterNameAndExecutable,
            PrintWriter logger, String errorMsg, Map<String, IInterpreterInfo> nameToInfo, Shell shell) {
        boolean foundError = false;
        //Check auto config or dialog return.
        if (interpreterNameAndExecutable == null) {
            logger.println("- When trimmed, the chosen file was null (returning null).");

            if (shell != null) {
                ErrorDialog.openError(shell, errorMsg,
                        "interpreterNameAndExecutable == null",
                        SharedCorePlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable == null",
                                new RuntimeException()));
            }
            foundError = true;
        }
        if (!foundError) {
            if (interpreterNameAndExecutable.o2.trim().length() == 0) {
                logger.println("- When trimmed, the chosen file was empty (returning null).");

                if (shell != null) {
                    ErrorDialog.openError(shell, errorMsg, "interpreterNameAndExecutable size == empty",
                            SharedCorePlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable size == empty",
                                    new RuntimeException()));
                }
                foundError = true;
            }
        }
        if (!foundError && nameToInfo != null) {
            String error = getDuplicatedMessageError(interpreterNameAndExecutable.o1, interpreterNameAndExecutable.o2,
                    nameToInfo);
            if (error != null) {
                logger.println("- Duplicated interpreter found.");
                if (shell != null) {
                    ErrorDialog.openError(shell, errorMsg, error, SharedCorePlugin.makeStatus(IStatus.ERROR,
                            "Duplicated interpreter information", new RuntimeException()));
                }
                foundError = true;
            }
        }
        return foundError;
    }

    /**
     * Gets a unique name for the interpreter based on an initial expected name.
     */
    public static String getUniqueInterpreterName(final String expectedName, Map<String, IInterpreterInfo> nameToInfo) {
        if (nameToInfo == null) {
            return expectedName;
        }
        String additional = "";
        int i = 0;
        while (InterpreterConfigHelpers.getDuplicatedMessageError(
                expectedName + additional, null, nameToInfo) != null) {
            i++;
            additional = String.valueOf(i);
        }
        return expectedName + additional;
    }

    /**
     * Uses the passed name and executable to see if it'll match against one of the existing
     *
     * The null parameters are ignored.
     */
    public static String getDuplicatedMessageError(String interpreterName, String executableOrJar,
            Map<String, IInterpreterInfo> nameToInfo) {
        if (nameToInfo == null) {
            return null;
        }
        String error = null;
        if (interpreterName != null) {
            interpreterName = interpreterName.trim();
            if (nameToInfo.containsKey(interpreterName)) {
                error = "An interpreter is already configured with the name: " + interpreterName;
            }
        }
        if (executableOrJar != null) {
            executableOrJar = executableOrJar.trim();
            for (IInterpreterInfo info : nameToInfo.values()) {
                if (info.getExecutableOrJar().trim().equals(executableOrJar)) {
                    error = "An interpreter is already configured with the path: " + executableOrJar;
                }
            }
        }
        return error;
    }

    public static boolean canAddNameAndExecutable(PrintWriter logger, NameAndExecutable interpreterNameAndExecutable,
            Map<String, IInterpreterInfo> nameToInfo, Shell shell) {
        interpreterNameAndExecutable.o1 = getUniqueInterpreterName(
                interpreterNameAndExecutable.o1, nameToInfo);
        boolean foundError = checkInterpreterNameAndExecutable(
                interpreterNameAndExecutable, logger, "Error getting info on interpreter",
                nameToInfo, shell);
        return foundError;
    }

    public static ObtainInterpreterInfoOperation createPipenvInterpreter(IInterpreterInfo[] interpreterInfos,
            Shell shell, PrintWriter logger, Map<String, IInterpreterInfo> nameToInfo, String defaultProjectLocation,
            IInterpreterManager interpreterManager) throws Exception {
        if (logger == null) {
            logger = new PrintWriter(new ByteArrayOutputStream());
        }
        if (interpreterInfos == null || interpreterInfos.length == 0) {
            PyDialogHelpers.openCritical("Unable to configure with pipenv",
                    "Cannot configure with pipenv without a base interpreter configured. Please configure the base interpreter first.");
            return null;
        }
        PipenvDialog pipenvDialog = new PipenvDialog(shell, interpreterInfos, null,
                defaultProjectLocation, interpreterManager, "New Pipenv interpreter", true);
        if (pipenvDialog.open() == Dialog.OK) {
            final IInterpreterInfo baseInterpreter = pipenvDialog.getBaseInterpreter();
            final String executableOrJar = baseInterpreter.getExecutableOrJar();
            final String pipenvLocation = pipenvDialog.getPipenvLocation();
            final String projectLocation = pipenvDialog.getProjectLocation();
            final SystemPythonNature nature = new SystemPythonNature(interpreterManager, baseInterpreter);

            File pythonVenvFromLocation = PipenvHelper.getPythonExecutableFromProjectLocationWithPipenv(pipenvLocation,
                    new File(projectLocation));
            if (pythonVenvFromLocation == null) {
                PipenvPackageManager.create(executableOrJar, pipenvLocation, projectLocation, nature);
                // Get the one just created.
                pythonVenvFromLocation = PipenvHelper.getPythonExecutableFromProjectLocationWithPipenv(pipenvLocation,
                        new File(projectLocation));
            }

            if (pythonVenvFromLocation != null) {
                NameAndExecutable interpreterNameAndExecutable = new NameAndExecutable(
                        new File(projectLocation).getName() + " (pipenv)",
                        pythonVenvFromLocation.getAbsolutePath());
                boolean foundError = canAddNameAndExecutable(logger, interpreterNameAndExecutable, nameToInfo, shell);
                if (foundError) {
                    return null;
                }

                logger.println("- Chosen interpreter (name and file):'" + interpreterNameAndExecutable);

                if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                    //ok, now that we got the file, let's see if it is valid and get the library info.
                    ObtainInterpreterInfoOperation ret = tryInterpreter(
                            interpreterNameAndExecutable, interpreterManager,
                            false, true, logger, shell);
                    if (ret != null && ret.result != null) {
                        ret.result.setPipenvTargetDir(projectLocation);
                    }
                    return ret;
                }
            }
        }
        return null;
    }

    public static String getConfigPageIdFromInterpreterType(int interpreterType) {
        String configPageId;
        switch (interpreterType) {
            case IInterpreterManager.INTERPRETER_TYPE_PYTHON:
                configPageId = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPagePython";
                break;

            case IInterpreterManager.INTERPRETER_TYPE_JYTHON:
                configPageId = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageJython";
                break;

            case IInterpreterManager.INTERPRETER_TYPE_IRONPYTHON:
                configPageId = "org.python.pydev.ui.pythonpathconf.interpreterPreferencesPageIronpython";
                break;

            default:
                throw new RuntimeException("Cannot recognize type: " + interpreterType);

        }
        return configPageId;
    }

}
