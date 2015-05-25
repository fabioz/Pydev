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

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.LinkedListWarningOnSlowOperations;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorWrapper;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType;

/**
 * This class uses code based from {@link AbstractInterpreterEditor} and
 * {@link AbstractInterpreterPreferencesPage} to form a somewhat lighter utility for auto-
 * configuring a PyDev interpreter (without having to rely on the Preferences dialog). Also
 * contains an interpreter auto-searching method {@link AutoConfigMaker#autoConfig(InterpreterType)}
 * implemented statically, for use by other dialogs (particularly {@link AbstractInterpreterEditor}.
 *
 * @author Andrew Ferrazzutti
 */
public class AutoConfigMaker {
    private InterpreterType interpreterType;
    private IInterpreterManager interpreterManager;
    private boolean advanced;

    private PrintWriter logger;

    private CharArrayWriter charWriter;
    private Map<String, IInterpreterInfo> nameToInfo;

    /**
     * Create a new AutoConfigMaker, which will hold all passed settings for automatically
     * creating a new interpreter configuration. Must call {@link AutoConfigMaker#autoConfigAttempt}
     * to actually create the configuration.
     * @param interpreterType The interpreter's Python type: Python, Jython, or IronPython.
     * @param advanced Set to true if advanced auto-config is to be used, which allows users to choose
     * an interpreter out of the ones found.
     * @param logger May be set to null to use a new logger.
     * @param nameToInfo A map of names as keys to the IInterpreterInfos of existing interpreters. Set
     * to null if no other interpreters exist at the time of the configuration attempt.
     */
    public AutoConfigMaker(InterpreterType interpreterType, boolean advanced,
            PrintWriter logger, Map<String, IInterpreterInfo> nameToInfo) {
        this.interpreterType = interpreterType;
        this.nameToInfo = nameToInfo;
        switch (interpreterType) {
            case JYTHON:
                interpreterManager = PydevPlugin.getJythonInterpreterManager(true);
                break;
            case IRONPYTHON:
                interpreterManager = PydevPlugin.getIronpythonInterpreterManager(true);
                break;
            default:
                interpreterManager = PydevPlugin.getPythonInterpreterManager(true);
        }
        this.advanced = advanced;

        if (logger != null) {
            this.charWriter = null;
            this.logger = logger;
        } else {
            //Use a new logger if one wasn't provided.
            this.charWriter = new CharArrayWriter();
            this.logger = new PrintWriter(this.charWriter);
        }
    }

    /**
     * Attempts to automatically find and apply an interpreter of the interpreter type specified
     * in the constructor, in cases when no interpreters of that type are yet configured.
     * @param onConfigComplete An optional JobChangeAdapter to be associated with the configure operation.
     */
    public boolean autoConfigSingleApply(JobChangeAdapter onConfigComplete) {
        if (interpreterManager.getInterpreterInfos().length != 0) {
            return false;
        }
        ObtainInterpreterInfoOperation operation = autoConfigSearch();
        //autoConfigSearch displays an error dialog if an interpreter couldn't be found, so don't display errors for null cases here.
        if (operation == null) {
            return false;
        }
        try {
            final IInterpreterInfo interpreterInfo = operation.result.makeCopy();
            final Set<String> interpreterNamesToRestore = new HashSet<String>(
                    Arrays.asList(operation.result.executableOrJar));

            //------------- Now, actually prepare the interpreter.
            Job applyOperationJob = new Job("Configure Interpreter") {

                @Override
                protected IStatus run(IProgressMonitor monitor) {
                    monitor = new AsynchronousProgressMonitorWrapper(monitor);
                    PyDialogHelpers.enableAskInterpreterStep(false);
                    monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
                    try {
                        //set this interpreter as the only interpreter, since none existed before this one
                        interpreterManager.setInfos(new IInterpreterInfo[] { interpreterInfo },
                                interpreterNamesToRestore, monitor);
                    } catch (Exception e) {
                        Log.log(e);
                        //show the user a message (so that it does not fail silently)...
                        String errorMsg = "Error configuring the chosen interpreter.\n"
                                + "Make sure the file containing the interpreter did not get corrupted during the configuration process.";
                        ErrorDialog.openError(EditorUtils.getShell(), "Interpreter configuration failure",
                                errorMsg, PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.", e));
                        return Status.CANCEL_STATUS;
                    } finally {
                        monitor.done();
                        PyDialogHelpers.enableAskInterpreterStep(true);
                    }
                    return Status.OK_STATUS;
                }

            };

            applyOperationJob.setUser(true);
            if (onConfigComplete != null) {
                applyOperationJob.addJobChangeListener(onConfigComplete);
            }
            applyOperationJob.schedule();
            return true;

        } catch (Exception e) {
            Log.log(e);
            String errorMsg = "Error getting info on the interpreter selected by the auto-configurer.\n"
                    + "Try manual configuration instead.\n\n"
                    + "Common reasons include:\n\n" + "- Using an unsupported version\n"
                    + "  (Python and Jython require at least version 2.1 and IronPython 2.6).\n"
                    + "\n" + "- Specifying an invalid interpreter\n"
                    + "  (usually a link to the actual interpreter on Mac or Linux)";
            //show the user a message (so that it does not fail silently)...
            ErrorDialog.openError(EditorUtils.getShell(), "Unable to get info on the interpreter.",
                    errorMsg, PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.", e));
            return false;
        } finally {
            if (charWriter != null) {
                Log.logInfo(charWriter.toString());
            }
        }
    }

    /**
     * Searches for a valid interpreter.
     * If quick auto-config, returns the first non-failing interpreter found.
     * If advanced, allows the user to choose from a list of verified interpreters, and returns the chosen one.
     *
     * @return The interpreter found by quick auto-config, or the one chosen by the user for advanced auto-config.
     */
    public ObtainInterpreterInfoOperation autoConfigSearch() {
        // get the possible interpreters
        final List<PossibleInterpreter> possibleInterpreters = getPossibleInterpreters();
        // keep track of the selected item
        PossibleInterpreter selectedFromPossible = null;

        // Query them for validity. If advanced or none installed, nothing will be selected.
        // If using quick config, choose the first installed interpreter (possibly none).
        Tuple3<PossibleInterpreter, Boolean, List<Exception>> r = removeInvalidPossibles(possibleInterpreters);
        selectedFromPossible = r.o1;
        boolean foundDuplicate = r.o2;
        List<Exception> exceptions = r.o3;

        // If we don't have anything we can add, exit now with an error message
        if (possibleInterpreters.size() > 0) {
            if (selectedFromPossible == null) {
                if (advanced && possibleInterpreters.size() > 1) {
                    // if using advanced config & we have more than 1 to choose from, ask the user
                    selectedFromPossible = promptToSelectInterpreter(possibleInterpreters);
                    // if selectedFromPossible is still null, user must have cancelled
                    if (selectedFromPossible == null) {
                        return null;
                    }
                } else {
                    // else, we can just auto-select for them
                    selectedFromPossible = possibleInterpreters.get(0);
                }
            }
            return selectedFromPossible.getOperation();
        } else {
            showNothingToConfigureError(foundDuplicate, exceptions);
        }
        return null;
    }

    private class PossibleInterpreter {
        private IInterpreterProvider provider;
        private ObtainInterpreterInfoOperation quickOperation;
        private Tuple<String, String> interpreterNameAndExecutable;

        public PossibleInterpreter(IInterpreterProvider provider) {
            this.provider = provider;
        }

        /**
         * Indicates whether or not an interpreter is valid for use.
         * @return <code>false</code> if the interpreter is a duplicate of a configured one,
         * or <code>true</code> if it is valid for use, or is yet to be installed.
         * @throws Exception An exception is thrown if the interpreter cannot be configured at all.
         */
        public boolean isValid() throws Exception {
            if (needInstall()) {
                return true;
            }

            // Try a quick config of the provider.
            // If getNameAndExecutable is successful, the interpreter won't be null & will have a unique name,
            // but it may a duplicate of something already configured.
            if (InterpreterConfigHelpers.getDuplicatedMessageError(null, getNameAndExecutable().o2,
                    nameToInfo) != null) {
                return false;
            }
            this.quickOperation = createOperation(false, false);
            return true;
        }

        private ObtainInterpreterInfoOperation createOperation(boolean advanced, boolean showErrors) throws Exception {
            return InterpreterConfigHelpers.tryInterpreter(getNameAndExecutable(), interpreterManager, !advanced,
                    showErrors, logger, EditorUtils.getShell());
        }

        private Tuple<String, String> getNameAndExecutable() throws Exception {
            if (interpreterNameAndExecutable != null) {
                return interpreterNameAndExecutable;
            }
            String executable = provider.getExecutableOrJar();
            if (executable != null && executable.trim().length() > 0) {
                String name = provider.getName();
                if (name == null) {
                    name = executable;
                }
                if (nameToInfo != null) {
                    name = InterpreterConfigHelpers.getUniqueInterpreterName(name, nameToInfo);
                }
                interpreterNameAndExecutable = new Tuple<String, String>(name, executable);
            } else {
                throw new Exception("Provider is invalid because it returned null from getExecutableOrJar()");
            }
            return interpreterNameAndExecutable;
        }

        public ObtainInterpreterInfoOperation getOperation() {
            if (needInstall()) {
                SafeRunner.run(new SafeRunnable() {
                    public void run() throws Exception {
                        provider.runInstall();
                    }
                });
                if (needInstall()) {
                    // runInstall failed, nothing else we can do (SafeRunnable or
                    // the installer itself will have displayed an error)
                    return null;
                }
                // name & executable may have changed, so set it to null to mark it for update the next time it's needed
                interpreterNameAndExecutable = null;
            }

            if (!advanced && quickOperation != null) {
                return quickOperation;
            } else {
                // Re-run an operation if user has to select folders (if advanced),
                // if the provider was uninstalled (if quickOperation is null),
                // or if the interpreter was missing libs (advanced & null quickOperation).
                try {
                    return createOperation(advanced, true);
                } catch (Exception e) {
                    // Failed to create operation, as we did "showErrors=true" we don't
                    // need to display them again though, so simply log them and exit
                    Log.log(e);
                    return null;
                }
            }
        }

        public boolean needInstall() {
            return !provider.isInstalled();
        }

        public String getExecutableOrJar() {
            return provider.getExecutableOrJar();
        }
    }

    private PossibleInterpreter promptToSelectInterpreter(final List<PossibleInterpreter> possibleInterpreters) {
        // Now we need to prompt the user to choose.
        ListDialog listDialog = new ListDialog(EditorUtils.getShell());

        listDialog.setContentProvider(new ArrayContentProvider());
        listDialog.setLabelProvider(new LabelProvider() {
            @Override
            public Image getImage(Object element) {
                return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
            }

            @Override
            public String getText(Object element) {
                PossibleInterpreter possible = (PossibleInterpreter) element;
                return possible.getExecutableOrJar();
            }
        });
        listDialog.setInput(possibleInterpreters.toArray());
        listDialog.setMessage("Multiple possible interpreters are available.\n"
                + "Please select which one you want to install and configure.");

        if (listDialog.open() == ListDialog.OK) {
            Object[] result = listDialog.getResult();
            return (PossibleInterpreter) result[0];
        } else {
            return null;
        }
    }

    private Tuple3<PossibleInterpreter, Boolean, List<Exception>> removeInvalidPossibles(
            final List<PossibleInterpreter> possibleInterpreters) {

        boolean foundDuplicate = false;
        List<Exception> exceptions = new LinkedListWarningOnSlowOperations<Exception>();

        // Iterate through the interpreters, removing the invalid ones
        for (Iterator<PossibleInterpreter> iterator = possibleInterpreters.iterator(); iterator.hasNext();) {
            PossibleInterpreter possibleInterpreter = iterator.next();
            Boolean validStatus = null;
            // Calling isValid may be a lengthy operation
            try {
                validStatus = possibleInterpreter.isValid();
                if (!validStatus) {
                    foundDuplicate = true;
                    throw new Exception("Duplicate interpreter.");
                }
            } catch (Exception e) {
                // If validStatus is null, an exception was thrown by isValid; save the first error found.
                if (validStatus == null) {
                    exceptions.add(e);
                }
                // Remove the interpreter if it is invalid or a duplicate.
                // Exception to this rule: if using advanced config, allow interpreters with no lib folders.
                if (!advanced || !e.getMessage().startsWith(InterpreterConfigHelpers.ERMSG_NOLIBS)) {
                    iterator.remove();
                }
                continue;
            }

            // We want to early exit for quick config
            if (!advanced && !possibleInterpreter.needInstall()) {
                // Early exit
                return new Tuple3<>(possibleInterpreter, foundDuplicate, exceptions);
            }
        }

        // keep going
        return new Tuple3<>(null, foundDuplicate, exceptions);
    }

    private void showNothingToConfigureError(boolean foundDuplicate, List<Exception> exceptions) {
        String errorMsg = "Auto-configurer could not find a valid interpreter"
                + (foundDuplicate ? " that has not already been configured" : "") + ".\n"
                + "Please manually configure a new interpreter instead.";

        String typeSpecificMessage;
        switch (interpreterType) {
            case PYTHON:
                typeSpecificMessage = "\n\nNote: the system environment variables that are used "
                        + "when auto-searching for a Jython interpreter are the following:\n"
                        + "- PATH\n"
                        + "- PYTHONHOME / PYTHON_HOME";
                break;
            case JYTHON:
                typeSpecificMessage = "\n\nNote: the system environment variables that are used "
                        + "when auto-searching for a Jython interpreter are the following:\n"
                        + "- PATH\n"
                        + "- PYTHONHOME / PYTHON_HOME\n"
                        + "- JYTHONHOME / JYTHON_HOME";
                break;
            default:
                typeSpecificMessage = "";
        }

        String message;
        if (exceptions.size() > 0) {
            message = "Errors getting info on discovered interpreter(s).\n"
                    + "See error log for details.";
        } else if (foundDuplicate) {
            message = "All interpreters found are already being used.";
        } else {
            // If there are no duplicates nor an interpreter error, nothing was found at all.
            message = "No interpreters were found.\n"
                    + "Make sure an interpreter is in the system PATH.";
        }
        String dialogTitle = "Unable to auto-configure.";

        if (exceptions.size() > 0) {
            IStatus[] children = new IStatus[exceptions.size()];
            for (int i = 0; i < exceptions.size(); i++) {
                Exception exception = exceptions.get(i);
                children[i] = PydevPlugin.makeStatus(IStatus.ERROR, null, exception);
            }
            MultiStatus multiStatus = new MultiStatus(PydevPlugin.getPluginID(), IStatus.ERROR, children, message,
                    null);
            ErrorDialog.openError(EditorUtils.getShell(), dialogTitle, errorMsg + typeSpecificMessage, multiStatus);
        } else {
            Status status = PydevPlugin.makeStatus(IStatus.ERROR, message, null);
            ErrorDialog.openError(EditorUtils.getShell(), dialogTitle, errorMsg + typeSpecificMessage, status);
        }
    }

    private List<PossibleInterpreter> getPossibleInterpreters() {
        final List<IInterpreterProvider> providers = getAllProviders();
        final List<PossibleInterpreter> possibleInterpreters = new ArrayList<>(providers.size());
        for (IInterpreterProvider provider : providers) {
            PossibleInterpreter possibleInterpreter = new PossibleInterpreter(provider);
            possibleInterpreters.add(possibleInterpreter);
        }
        return possibleInterpreters;
    }

    private List<IInterpreterProvider> getAllProviders() {
        final List<IInterpreterProvider> providers = new ArrayList<IInterpreterProvider>();
        @SuppressWarnings("unchecked")
        List<IInterpreterProviderFactory> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_PROVIDER);
        for (final IInterpreterProviderFactory providerFactory : participants) {
            SafeRunner.run(new SafeRunnable() {
                public void run() throws Exception {
                    IInterpreterProvider[] ips = providerFactory.getInterpreterProviders(interpreterType);
                    if (ips != null) {
                        providers.addAll(Arrays.asList(ips));
                    }
                }
            });
        }
        return providers;
    }
}
