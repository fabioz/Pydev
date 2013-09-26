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
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
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
import org.python.pydev.shared_ui.utils.AsynchronousProgressMonitorDialog;
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

    private Shell shell;
    private InterpreterType interpreterType;
    private IInterpreterManager interpreterManager;

    public CancelException cancelException = new CancelException();

    public AutoConfigMaker(Shell shell,
            InterpreterType interpreterType) {
        this.shell = shell;
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
    }

    /**
     * Attempts to perform an interpreter auto-config of the interpreter type specified in the constructor.
     * @return <code>true</code> if the auto-config was successful, <code>false</code> otherwise. 
     */
    public boolean autoConfigAttempt() {
        CharArrayWriter charWriter = new CharArrayWriter();
        PrintWriter logger = new PrintWriter(charWriter);
        logger.println("Information about process of adding new interpreter:");
        try {
            final Tuple<String, String> interpreterNameAndExecutable = autoConfig(interpreterType, cancelException);
            if (interpreterNameAndExecutable == null) {
                return false;
            }

            boolean foundError = InterpreterConfigHelpers.checkInterpreterNameAndExecutable(
                    interpreterNameAndExecutable, logger, "Error getting info on interpreter",
                    null, shell);

            if (foundError) {
                return false;
            }

            logger.println("- Chosen interpreter (name and file):'" + interpreterNameAndExecutable);

            if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                //ok, now that we got the file, let's see if it is valid and get the library info.
                ObtainInterpreterInfoOperation operation = InterpreterConfigHelpers.findInterpreter(
                        interpreterNameAndExecutable, interpreterManager,
                        true, logger, null, shell);

                if (operation != null) {
                    interpreterInfo = operation.result.makeCopy();
                    final Set<String> interpreterNamesToRestore = new HashSet<String>(
                            Arrays.asList(operation.result.executableOrJar));

                    //------------- Now, actually prepare the interpreter.
                    ProgressMonitorDialog applyMonitorDialog = new AsynchronousProgressMonitorDialog(shell);
                    applyMonitorDialog.setBlockOnOpen(false);

                    try {
                        IRunnableWithProgress applyOperation = new IRunnableWithProgress() {

                            public void run(IProgressMonitor monitor) throws InvocationTargetException,
                                    InterruptedException {
                                monitor.beginTask("Restoring PYTHONPATH", IProgressMonitor.UNKNOWN);
                                try {
                                    //clear all but the ones that appear
                                    interpreterManager.setInfos(new IInterpreterInfo[] { interpreterInfo },
                                            interpreterNamesToRestore, monitor);
                                } finally {
                                    monitor.done();
                                }
                            }
                        };

                        applyMonitorDialog.run(true, true, applyOperation);

                    } catch (Exception e) {
                        Log.log(e);
                        return false;
                    }
                    //------------- Done.
                    return true;
                } else {
                    return false;
                }
            }

        } catch (Exception e) {
            Log.log(e);
            return false;
        } finally {
            Log.logInfo(charWriter.toString());
        }

        return false;
    }

    static Tuple<String, String> autoConfig(final InterpreterType interpreterType, final CancelException cancelException) {
        final Tuple<String, String> interpreterNameAndExecutable;
        try {
            @SuppressWarnings("unchecked")
            List<IInterpreterProviderFactory> participants = ExtensionHelper
                    .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_PROVIDER);
            final List<IInterpreterProvider> providers = new ArrayList<IInterpreterProvider>();
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

            if (providers.size() == 0) {
                // If there are no providers at this point it means that
                // the selected target (python/jython/etc)
                // was no found to be available on any known location
                interpreterNameAndExecutable = null;
            } else {

                final IInterpreterProvider selectedProvider;
                if (providers.size() == 1) {
                    selectedProvider = providers.get(0);
                } else {
                    // The user should select which one to use...
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

                    selectedProvider = (IInterpreterProvider) result[0];
                }

                if (!selectedProvider.isInstalled()) {
                    SafeRunner.run(new SafeRunnable() {
                        public void run() throws Exception {
                            selectedProvider.runInstall();
                        }
                    });

                    if (!selectedProvider.isInstalled()) {
                        // if still not installed, user pressed cancel or an
                        // error was handled and displayed to the user during
                        // the thirdparty install process
                        throw cancelException;
                    }
                }
                String executable = selectedProvider.getExecutableOrJar();
                if (executable == null) {
                    interpreterNameAndExecutable = null;
                } else {
                    String name = selectedProvider.getName();
                    if (name == null) {
                        name = executable;
                    }
                    interpreterNameAndExecutable = new Tuple<String, String>(name, executable);
                }
            }

        } catch (CancelException e) {
            // user cancelled.
            return null;
        }
        if (interpreterNameAndExecutable == null) {
            reportAutoConfigProblem(null, null);
            return null;
        }
        return interpreterNameAndExecutable;
    }

    static void reportAutoConfigProblem(Exception e, Shell shell) {
        if (shell == null) {
            shell = EditorUtils.getShell();
        }
        String errorMsg = "Unable to auto-configure the interpreter.\n"
                + "Please manually configure a new interpreter instead.";
        ErrorDialog.openError(shell, "Unable to auto-configure.", errorMsg,
                PydevPlugin.makeStatus(IStatus.ERROR, "Unable to gather the needed info from the system.\n" + "\n"
                        + "This usually means that your interpreter is not in\n" + "the system PATH.", e));
    }
}
