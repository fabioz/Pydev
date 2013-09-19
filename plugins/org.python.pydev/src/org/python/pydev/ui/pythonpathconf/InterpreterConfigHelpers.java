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
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.runners.SimpleJythonRunner;
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

    /**
     * Attempts to set up a provided interpreter.
     * 
     * @param interpreterNameAndExecutable Information pertaining to the interpreter to prepare.
     * @param interpreterManager
     * @param quickAutoConfig If true, folders will be automatically added to the SYSTEM pythonpath.
     * Otherwise, they must be selected manually with a dialog.
     * @param logger
     * @param nameToInfo
     * @param shell
     * @return
     * @throws Exception
     */
    static ObtainInterpreterInfoOperation findInterpreter(Tuple<String, String> interpreterNameAndExecutable,
            IInterpreterManager interpreterManager, boolean quickAutoConfig, PrintWriter logger,
            Map<String, IInterpreterInfo> nameToInfo, Shell shell) throws Exception {
        logger.println("- Ok, file is non-null. Getting info on:" + interpreterNameAndExecutable.o2);
        ProgressMonitorDialog monitorDialog = new AsynchronousProgressMonitorDialog(shell);
        monitorDialog.setBlockOnOpen(false);
        ObtainInterpreterInfoOperation operation;
        while (true) {
            operation = new ObtainInterpreterInfoOperation(interpreterNameAndExecutable.o2, logger,
                    interpreterManager, quickAutoConfig);
            monitorDialog.run(true, false, operation);
            if (operation.e != null) {
                logger.println("- Some error happened while getting info on the interpreter:");
                operation.e.printStackTrace(logger);

                if (operation.e instanceof SimpleJythonRunner.JavaNotConfiguredException) {
                    SimpleJythonRunner.JavaNotConfiguredException javaNotConfiguredException = (SimpleJythonRunner.JavaNotConfiguredException) operation.e;

                    ErrorDialog.openError(shell, "Error getting info on interpreter",
                            javaNotConfiguredException.getMessage(), PydevPlugin.makeStatus(IStatus.ERROR,
                                    "Java vm not configured.\n", javaNotConfiguredException));

                } else if (operation.e instanceof JDTNotAvailableException) {
                    JDTNotAvailableException noJdtException = (JDTNotAvailableException) operation.e;
                    ErrorDialog.openError(shell, "Error getting info on interpreter",
                            noJdtException.getMessage(),
                            PydevPlugin.makeStatus(IStatus.ERROR, "JDT not available.\n", noJdtException));

                } else if (!quickAutoConfig) {
                    String errorMsg = "Error getting info on interpreter.\n\n"
                            + "Common reasons include:\n\n" + "- Using an unsupported version\n"
                            + "  (Python and Jython require at least version 2.1 and IronPython 2.6).\n"
                            + "\n" + "- Specifying an invalid interpreter\n"
                            + "  (usually a link to the actual interpreter on Mac or Linux)" + "";
                    //show the user a message (so that it does not fail silently)...
                    ErrorDialog.openError(shell, "Unable to get info on the interpreter.",
                            errorMsg, PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.",
                                    operation.e));
                }

                throw operation.e;

            } else {
                if (operation.result != null) {
                    boolean foundError = checkInterpreterNameAndExecutable(new Tuple<String, String>(
                            interpreterNameAndExecutable.o1, operation.result.executableOrJar), logger,
                            "Error adding interpreter", nameToInfo, shell);

                    if (foundError) {
                        return null;
                    }

                    try {
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
                                    ZipFile zipFile = new ZipFile(file);
                                    for (String extension : validSourceFiles) {
                                        if (zipFile.getEntry("threading." + extension) != null) {
                                            hashSet.remove("threading");
                                        }
                                        if (zipFile.getEntry("traceback." + extension) != null) {
                                            hashSet.remove("traceback");
                                        }
                                    }
                                    zipFile.close();
                                } catch (Exception e) {
                                    //ignore (not zip file)
                                }
                            }
                        }

                        if (hashSet.size() > 0) {
                            if (!quickAutoConfig) {
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
                                if (choice != 2) {
                                    return null;
                                }
                            } else {
                                return null;
                            }
                        }
                    } catch (Exception e) {
                        ErrorDialog.openError(shell,
                                "Problem checking if the interpreter paths are correct.", e.getMessage(),
                                PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.", e));
                        throw e;
                    }
                    operation.result.setName(interpreterNameAndExecutable.o1);
                    logger.println("- Success getting the info. Result:" + operation.result);
                    return operation;
                } else {
                    return null;
                }
            }
        }
    }

    static boolean checkInterpreterNameAndExecutable(Tuple<String, String> interpreterNameAndExecutable,
            PrintWriter logger, String errorMsg, Map<String, IInterpreterInfo> nameToInfo, Shell shell) {
        boolean foundError = false;
        //Check auto config or dialog return.
        if (interpreterNameAndExecutable == null) {
            logger.println("- When trimmed, the chosen file was null (returning null).");

            ErrorDialog.openError(shell, errorMsg,
                    "interpreterNameAndExecutable == null",
                    PydevPlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable == null",
                            new RuntimeException()));
            foundError = true;
        }
        if (!foundError) {
            if (interpreterNameAndExecutable.o2.trim().length() == 0) {
                logger.println("- When trimmed, the chosen file was empty (returning null).");

                ErrorDialog.openError(shell, errorMsg, "interpreterNameAndExecutable size == empty",
                        PydevPlugin.makeStatus(IStatus.ERROR, "interpreterNameAndExecutable size == empty",
                                new RuntimeException()));
                foundError = true;
            }
        }
        if (!foundError && nameToInfo != null) {
            String error = getDuplicatedMessageError(interpreterNameAndExecutable.o1, interpreterNameAndExecutable.o2,
                    nameToInfo);
            if (error != null) {
                logger.println("- Duplicated interpreter found.");
                ErrorDialog.openError(shell, errorMsg, error, PydevPlugin.makeStatus(IStatus.ERROR,
                        "Duplicated interpreter information", new RuntimeException()));
                foundError = true;
            }
        }
        return foundError;
    }

    /**
     * Uses the passed name and executable to see if it'll match against one of the existing 
     * 
     * The null parameters are ignored.
     */
    public static String getDuplicatedMessageError(String interpreterName, String executableOrJar,
            Map<String, IInterpreterInfo> nameToInfo) {
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

    public static HashSet<IPath> getRootPaths() {
        IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
        IPath rootLocation = root.getLocation().makeAbsolute();

        HashSet<IPath> rootPaths = new HashSet<IPath>();
        rootPaths.add(rootLocation);

        IProject[] projects = root.getProjects();
        for (IProject iProject : projects) {
            IPath location = iProject.getLocation();
            IPath abs = location.makeAbsolute();
            if (!rootLocation.isPrefixOf(abs)) {
                rootPaths.add(abs);
            }
        }
        return rootPaths;
    }

    public static boolean isChildOfRootPath(String data, HashSet<IPath> rootPaths) {
        IPath path = Path.fromOSString(data);
        for (IPath p : rootPaths) {
            if (p.isPrefixOf(path)) {
                return true;
            }
        }
        return false;
    }
}
