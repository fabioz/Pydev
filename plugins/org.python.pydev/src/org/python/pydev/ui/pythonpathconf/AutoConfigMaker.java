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
import org.python.pydev.ui.pythonpathconf.AbstractInterpreterEditor.CancelException;
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
    private IInterpreterInfo interpreterInfo;

    private InterpreterType interpreterType;
    private IInterpreterManager interpreterManager;
    private boolean advanced;

    private PrintWriter logger;
    private Shell shell;

    public CancelException cancelException = new CancelException();

    private CharArrayWriter charWriter;

    /**
     * Create a new AutoConfigMaker, which will hold all passed settings for automatically
     * creating a new interpreter configuration. Must call {@link AutoConfigMaker#autoConfigAttempt}
     * to actually create the configuration.
     * @param interpreterType The interpreter's Python type: Python, Jython, or IronPython.
     * @param advanced Set to true if advanced auto-config is to be used, which allows users to choose
     * an interpreter out of the ones found.
     * @param logger May be set to null to use a new logger.
     * @param shell May be set to null to use a default shell.
     */
    public AutoConfigMaker(InterpreterType interpreterType, boolean advanced,
            PrintWriter logger, Shell shell) {
        this.interpreterType = interpreterType;
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
        ObtainInterpreterInfoOperation operation = autoConfigSearch(null);
        //autoConfigSearch displays an error dialog if an interpreter couldn't be found, so don't display errors for null cases here.
        if (operation == null) {
            return;
        }
        try {
            interpreterInfo = operation.result.makeCopy();
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
     * @param nameToInfo A map of names as keys to the IInterpreterInfos of existing interpreters. Set
     * to null if no other interpreters exist at the time of the configuration attempt.
     * @return The interpreter found by quick auto-config, or the one chosen by the user for advanced auto-config.
     */
    ObtainInterpreterInfoOperation autoConfigSearch(Map<String, IInterpreterInfo> nameToInfo) {

        List<Tuple<String, String>> interpreterNameAndExecutables = new ArrayList<Tuple<String, String>>();
        final List<IInterpreterProvider> providers = new ArrayList<IInterpreterProvider>();
        List<ObtainInterpreterInfoOperation> operations = !advanced ? null
                : new ArrayList<ObtainInterpreterInfoOperation>();

        boolean foundSomething = false;
        boolean foundSomethingToInstall = false;
        logger.println("Information about process of adding new interpreter:");
        try {
            //First, search for interpreters that match an appropriate naming convention.
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

            // If there are no providers at this point it means that
            // the selected target (python/jython/etc)
            // was no found to be available on any known location
            if (providers.size() != 0) {
                for (int i = 0; i < providers.size(); i++) {
                    final IInterpreterProvider provider = providers.get(i);
                    String executable = provider.getExecutableOrJar();
                    if (executable != null) {
                        String name = provider.getName();
                        if (name == null) {
                            name = executable;
                        }
                        interpreterNameAndExecutables.add(new Tuple<String, String>(name, executable));
                    } else {
                        providers.remove(i);
                        i--;
                    }
                }
            }

            //Now test all interpreters found.
            if (interpreterNameAndExecutables.size() > 0) {

                for (int i = 0; i < interpreterNameAndExecutables.size(); i++) {
                    Tuple<String, String> interpreterNameAndExecutable = interpreterNameAndExecutables.get(i);
                    final IInterpreterProvider provider = providers.get(i);
                    if (!provider.isInstalled()) {
                        // we put in a null operation when the provider is not installed yet,
                        // we will create the operation later if it is installed
                        if (advanced) {
                            operations.add(null);
                        }
                        foundSomething = true;
                        foundSomethingToInstall = true;
                        continue;
                    }

                    if (nameToInfo != null) {
                        interpreterNameAndExecutable.o1 = InterpreterConfigHelpers.getUniqueInterpreterName(
                                interpreterNameAndExecutable.o1, nameToInfo);
                    }
                    boolean foundError = InterpreterConfigHelpers.checkInterpreterNameAndExecutable(
                            interpreterNameAndExecutable, logger, "Error adding interpreter", nameToInfo, null);
                    if (foundError) {
                        foundSomething = true;
                        interpreterNameAndExecutables.remove(i);
                        providers.remove(i);
                        i--;
                        continue;
                    }

                    logger.println("- Chosen interpreter (name and file):'" + interpreterNameAndExecutable);

                    if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                        try {
                            //ok, now that we got the file, let's see if it is valid and get the library info.
                            //Set the quickAutoConfig parameter to true, even for advanced auto-config, as this is just a testing phase.
                            ObtainInterpreterInfoOperation operation = null;
                            try {
                                operation = InterpreterConfigHelpers.tryInterpreter(
                                        interpreterNameAndExecutable, interpreterManager,
                                        true, false, logger, shell);
                            } catch (Exception e) {
                                Log.log(e);
                            }
                            if (operation != null) {
                                if (!advanced) {
                                    //If quick auto-config, return the first non-failing interpreter.
                                    return operation;
                                }
                                operations.add(operation);
                            } else if (operation == null && advanced) {
                                //Otherwise, remove all failed interpreters from the appropriate lists.
                                interpreterNameAndExecutables.remove(i);
                                providers.remove(i);
                                i--;
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                }
            }

            //If using quick config and haven't returned already, then no interpreter was found.
            //Also, use a different error message for when no more _unique_ interpreters can be found.
            if (!foundSomethingToInstall && (!advanced || operations.size() == 0)) {
                String errorMsg = "Auto-configurer could not find a valid interpreter"
                        + (foundSomething ? " that has not already been configured" : "") + ".\n"
                        + "Please manually configure a new interpreter instead.";
                ErrorDialog.openError(EditorUtils.getShell(), "Unable to auto-configure.", errorMsg,
                        PydevPlugin.makeStatus(IStatus.ERROR, foundSomething ? "All valid interpreters are already being used." :
                                "Unable to gather the needed info from the system.\n\n"
                                        + "This usually means that your interpreter is not in\n" + "the system PATH.",
                                null));
                return null;
            }

            // At this point, it is guaranteed advanced auto-config is in use or that the providers
            // have not been installed yet
            final int indexToUse;
            if (providers.size() == 1) {
                //If only one is valid, select that one.
                indexToUse = 0;
            }
            else {
                //If advanced, test them all and the user should choose which non-failing interpreter to use.
                ListDialog listDialog = new ListDialog(EditorUtils.getShell());

                listDialog.setContentProvider(new ArrayContentProvider());
                listDialog.setLabelProvider(new LabelProvider() {
                    @Override
                    public Image getImage(Object element) {
                        return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
                    }

                    @Override
                    public String getText(Object element) {
                        IInterpreterProvider provider = (IInterpreterProvider) element;
                        return provider.getExecutableOrJar();
                    }
                });
                listDialog.setInput(providers.toArray());
                listDialog.setMessage("Multiple possible interpreters are available.\n"
                        + "Please select which one you want to install and configure.");

                int open = listDialog.open();
                if (open != ListDialog.OK) {
                    throw cancelException;
                }
                Object[] result = listDialog.getResult();
                if (result == null || result.length == 0) {
                    throw cancelException;
                }

                //Select the interpreter that is equivalent to the chosen provider.
                indexToUse = providers.indexOf(result[0]);
            }

            // Extract the chosen operation and, if needed, the provider
            // The provider is needed iff the selected provider has
            // not already been installed.
            ObtainInterpreterInfoOperation chosenOperation = null;
            IInterpreterProvider providerToInstall = null;
            if (operations != null) {
                chosenOperation = operations.get(indexToUse);
            }
            if (chosenOperation == null) {
                providerToInstall = providers.get(indexToUse);
            }

            if (providerToInstall != null) {
                if (!providerToInstall.isInstalled()) {
                    final IInterpreterProvider provider = providerToInstall;
                    SafeRunner.run(new SafeRunnable() {
                        public void run() throws Exception {
                            provider.runInstall();
                        }
                    });

                    if (!providerToInstall.isInstalled()) {
                        // if still not installed, user pressed cancel or an
                        // error was handled and displayed to the user during
                        // the thirdparty install process
                        throw cancelException;
                    }
                }

                String name = providerToInstall.getName();
                String executable = providerToInstall.getExecutableOrJar();
                if (nameToInfo != null) {
                    name = InterpreterConfigHelpers.getUniqueInterpreterName(name, nameToInfo);
                }
                Tuple<String, String> interpreterNameAndExecutable = new Tuple<String, String>(name, executable);

                try {
                    chosenOperation = InterpreterConfigHelpers.tryInterpreter(
                            interpreterNameAndExecutable, interpreterManager,
                            true, false, logger, shell);
                } catch (Exception e1) {
                    // We have failed, re-run the operation, but display error this time
                    // and return null to indicate the error
                    try {
                        InterpreterConfigHelpers.tryInterpreter(
                                interpreterNameAndExecutable, interpreterManager,
                                true, true, logger, shell);
                    } catch (Exception e2) {
                        Log.log(e2);
                    }
                    return null;
                }
            }

            if (advanced) {
                //Since this is advanced auto-config, must reconfigure the interpreter to allow user selection of folders.
                try {
                    chosenOperation = InterpreterConfigHelpers.tryInterpreter(new Tuple<String, String>(
                            chosenOperation.result.getName(), chosenOperation.result.getExecutableOrJar()),
                            interpreterManager, false, true, logger, shell);
                } catch (Exception e) {
                    //displayErrors was set to true in the above call, so no need to handle error display here.
                    Log.log(e);
                    return null;
                }
            }

            return chosenOperation;

        } catch (CancelException e) {
            // user cancelled.
            return null;
        }
    }
}
