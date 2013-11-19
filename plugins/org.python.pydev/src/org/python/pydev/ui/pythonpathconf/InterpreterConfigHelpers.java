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

import java.io.File;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipFile;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.swt.widgets.Shell;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;

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
    public final static String[] CONFIG_NAMES = new String[] { "Manual Config", "Quick Auto-Config",
            "Advanced Auto-Config" };
    public final static int NUM_CONFIG_TYPES = 3;

    public final static String ERMSG_NOLIBS = "The interpreter's standard libraries (typically in a Lib/ folder) are missing: ";

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
    static ObtainInterpreterInfoOperation tryInterpreter(Tuple<String, String> interpreterNameAndExecutable,
            IInterpreterManager interpreterManager, boolean autoSelectFolders, boolean displayErrors,
            PrintWriter logger, Shell shell) throws Exception {
        String executable = interpreterNameAndExecutable.o2;
        logger.println("- Ok, file is non-null. Getting info on:" + executable);
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(shell);
        monitorDialog.setBlockOnOpen(false);
        ObtainInterpreterInfoOperation operation;
        while (true) {
            operation = new ObtainInterpreterInfoOperation(interpreterNameAndExecutable.o2, logger,
                    interpreterManager, autoSelectFolders);
            monitorDialog.run(true, false, operation);
            if (operation.e != null) {
                logger.println("- Some error happened while getting info on the interpreter:");
                operation.e.printStackTrace(logger);
                String errorTitle = "Unable to get info on the interpreter: " + executable;

                if (operation.e instanceof SimpleJythonRunner.JavaNotConfiguredException) {
                    SimpleJythonRunner.JavaNotConfiguredException javaNotConfiguredException = (SimpleJythonRunner.JavaNotConfiguredException) operation.e;
                    if (displayErrors) {
                        ErrorDialog.openError(shell, errorTitle,
                                javaNotConfiguredException.getMessage(), PydevPlugin.makeStatus(IStatus.ERROR,
                                        "Java vm not configured.\n", javaNotConfiguredException));
                    }
                    throw new Exception(javaNotConfiguredException);

                } else if (operation.e instanceof JDTNotAvailableException) {
                    JDTNotAvailableException noJdtException = (JDTNotAvailableException) operation.e;
                    if (displayErrors) {
                        ErrorDialog.openError(shell, errorTitle,
                                noJdtException.getMessage(),
                                PydevPlugin.makeStatus(IStatus.ERROR, "JDT not available.\n", noJdtException));
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
                                errorMsg, PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.",
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

            String[] validSourceFiles = FileTypesPreferencesPage.getValidSourceFiles();
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
    static boolean checkInterpreterNameAndExecutable(Tuple<String, String> interpreterNameAndExecutable,
            PrintWriter logger, String errorMsg, Map<String, IInterpreterInfo> nameToInfo, Shell shell) {
        boolean foundError = false;
        //Check auto config or dialog return.
        if (interpreterNameAndExecutable == null) {
            logger.println("- When trimmed, the chosen file was null (returning null).");

            if (shell != null) {
                ErrorDialog.openError(shell, errorMsg,
                        "interpreterNameAndExecutable == null",
                        PydevPlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable == null",
                                new RuntimeException()));
            }
            foundError = true;
        }
        if (!foundError) {
            if (interpreterNameAndExecutable.o2.trim().length() == 0) {
                logger.println("- When trimmed, the chosen file was empty (returning null).");

                if (shell != null) {
                    ErrorDialog.openError(shell, errorMsg, "interpreterNameAndExecutable size == empty",
                            PydevPlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable size == empty",
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
                    ErrorDialog.openError(shell, errorMsg, error, PydevPlugin.makeStatus(IStatus.ERROR,
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

    /**
     * Creates a Set of the root paths of all projects (and the workspace root itself).
     * @return A HashSet of root paths.
     */
    public static HashSet<IPath> getRootPaths() {
        HashSet<IPath> rootPaths = new HashSet<IPath>();
        if (SharedCorePlugin.inTestMode()) {
            return rootPaths;
        }
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath rootLocation = root.getLocation().makeAbsolute();

        rootPaths.add(rootLocation);

        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            IPath location = iProject.getLocation();
            if (location != null) {
                IPath abs = location.makeAbsolute();
                if (!rootLocation.isPrefixOf(abs)) {
                    rootPaths.add(abs);
                }
            }
        }
        return rootPaths;
    }

    /**
     * States whether or not a given path is the child of at least one root path of a set of root paths.
     * @param data The path that will be checked for child status.
     * @param rootPaths A set of root paths.
     * @return True if the path of data is a child of any of the paths of rootPaths.
     */
    public static boolean isChildOfRootPath(String data, Set<IPath> rootPaths) {
        IPath path = Path.fromOSString(data);
        for (IPath p : rootPaths) {
            if (p.isPrefixOf(path)) {
                return true;
            }
        }
        return false;
    }
}
