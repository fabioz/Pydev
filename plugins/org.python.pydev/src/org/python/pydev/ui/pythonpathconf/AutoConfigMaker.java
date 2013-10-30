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
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListDialog;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.Tuple;
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
    private Shell shell;

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
     * @param shell May be set to null to use a default shell.
     * @param nameToInfo A map of names as keys to the IInterpreterInfos of existing interpreters. Set
     * to null if no other interpreters exist at the time of the configuration attempt.    
     */
    public AutoConfigMaker(InterpreterType interpreterType, boolean advanced,
            PrintWriter logger, Shell shell, Map<String, IInterpreterInfo> nameToInfo) {
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
        this.shell = shell != null ? shell : EditorUtils.getShell();

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
    public void autoConfigSingleApply(JobChangeAdapter onConfigComplete) {
        if (interpreterManager.getInterpreterInfos().length != 0) {
            return;
        }
        ObtainInterpreterInfoOperation operation = autoConfigSearch();
        //autoConfigSearch displays an error dialog if an interpreter couldn't be found, so don't display errors for null cases here.
        if (operation == null) {
            return;
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
                        ErrorDialog.openError(shell, "Interpreter configuration failure",
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
            return;

        } catch (Exception e) {
            Log.log(e);
            String errorMsg = "Error getting info on the interpreter selected by the auto-configurer.\n"
                    + "Try manual configuration instead.\n\n"
                    + "Common reasons include:\n\n" + "- Using an unsupported version\n"
                    + "  (Python and Jython require at least version 2.1 and IronPython 2.6).\n"
                    + "\n" + "- Specifying an invalid interpreter\n"
                    + "  (usually a link to the actual interpreter on Mac or Linux)";
            //show the user a message (so that it does not fail silently)...
            ErrorDialog.openError(shell, "Unable to get info on the interpreter.",
                    errorMsg, PydevPlugin.makeStatus(IStatus.ERROR, "See error log for details.", e));
            return;
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
        // record for later error messages that we did have at least one possible
        final boolean foundSomething = possibleInterpreters.size() > 0;
        // keep track of the selected item
        PossibleInterpreter selectedFromPossible = null;

        // query them for validity, this step may choose an interpreter 
        selectedFromPossible = removeInvalidPossibles(possibleInterpreters);

        // We don't have anything we can add, exit now with an error message
        if (possibleInterpreters.size() > 0) {
            if (selectedFromPossible == null) {
                if (possibleInterpreters.size() > 1) {
                    // if we have more than 1 to choose from, ask the user
                    selectedFromPossible = promptToSelectInterpreter(possibleInterpreters);
                } else {
                    // else, we can just auto-select for them
                    selectedFromPossible = possibleInterpreters.get(0);
                }
            }
            // if selectedFromPossible is still null, user cancelled along the way or
            // an error message needs to have been displayed
            if (selectedFromPossible != null) {
                return selectedFromPossible.getOperation();
            }
        } else {
            showNothingToConfigureError(foundSomething);
        }
        return null;
    }

    private class PossibleInterpreter {
        private IInterpreterProvider provider;
        private ObtainInterpreterInfoOperation quickOperation;

        public PossibleInterpreter(IInterpreterProvider provider) {
            this.provider = provider;
        }

        public boolean isValid() {
            if (needInstall()) {
                return true;
            }

            // Try a quick config of the provider
            try {
                if (InterpreterConfigHelpers.checkInterpreterNameAndExecutable(
                        getNameAndExecutable(), logger, "Error adding interpreter", nameToInfo, null)) {
                    return false;
                }
                this.quickOperation = createOperation(false, false);
                return true;
            } catch (Exception e) {
                Log.log(e);
                return false;
            }
        }

        private ObtainInterpreterInfoOperation createOperation(boolean advanced, boolean showErrors) throws Exception {
            return InterpreterConfigHelpers.tryInterpreter(getNameAndExecutable(), interpreterManager, !advanced,
                    showErrors, logger, shell);
        }

        private Tuple<String, String> getNameAndExecutable() throws Exception {
            Tuple<String, String> interpreterNameAndExecutable;
            String executable = provider.getExecutableOrJar();
            if (executable != null) {
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
            }

            if (needInstall()) {
                // runInstall failed, nothing else we can do (SafeRunnable or
                // the installer itself will have displayed an error)
                return null;
            }

            if (!advanced && quickOperation != null) {
                return quickOperation;
            } else {
                try {
                    return createOperation(advanced, true);
                } catch (Exception e) {
                    // Failed to create operation, as we did "showErros=true" we don't
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

    private PossibleInterpreter removeInvalidPossibles(final List<PossibleInterpreter> possibleInterpreters) {

        // Iterate through the interpreters, removing the invalid ones
        for (Iterator<PossibleInterpreter> iterator = possibleInterpreters.iterator(); iterator.hasNext();) {
            PossibleInterpreter possibleInterpreter = iterator.next();
            // Calling isValid may be a lengthy operation
            if (!possibleInterpreter.isValid()) {
                iterator.remove();
                continue;
            }

            // We want to early exit for quick config
            if (!advanced && !possibleInterpreter.needInstall()) {
                // Early exit 
                return possibleInterpreter;
            }
        }

        // keep going
        return null;
    }

    private void showNothingToConfigureError(boolean foundSomething) {
        String errorMsg = "Auto-configurer could not find a valid interpreter"
                + (foundSomething ? " that has not already been configured" : "") + ".\n"
                + "Please manually configure a new interpreter instead.";
        String message = foundSomething ? "All valid interpreters are already being used." :
                "Unable to gather the needed info from the system.\n\n"
                        + "This usually means that your interpreter is not in\n"
                        + "the system PATH.";
        Status status = PydevPlugin.makeStatus(IStatus.ERROR, message, null);
        String dialogTitle = "Unable to auto-configure.";
        ErrorDialog.openError(EditorUtils.getShell(), dialogTitle, errorMsg, status);
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
