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
    private boolean advanced;

    public CancelException cancelException = new CancelException();

    public AutoConfigMaker(Shell shell,
            InterpreterType interpreterType, boolean advanced) {
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
        this.advanced = advanced;
    }

    /**
     * Attempts to perform an interpreter auto-config of the interpreter type specified in the constructor.
     * NOTE: This method is only called in cases when no other interpreters exist for a project.
     * @return <code>true</code> if the auto-config was successful, <code>false</code> otherwise. 
     */
    public boolean autoConfigAttempt() {
        CharArrayWriter charWriter = new CharArrayWriter();
        PrintWriter logger = new PrintWriter(charWriter);
        logger.println("Information about process of adding new interpreter:");
        try {
            List<Tuple<String, String>> interpreterNameAndExecutables = autoConfig(interpreterType, advanced,
                    cancelException);
            if (interpreterNameAndExecutables.size() == 0) {
                return false;
            }

            for (Tuple<String, String> interpreterNameAndExecutable : interpreterNameAndExecutables) {
                //Note: don't need getUniqueInterpreterName, since no configured interpreters exist when this method is called.
                boolean foundError = InterpreterConfigHelpers.checkInterpreterNameAndExecutable(
                        interpreterNameAndExecutable, logger, "Error getting info on interpreter",
                        null, shell);

                if (foundError) {
                    return false;
                }
            }

            //Iterate through all chosen interpreters until one works, or until they all fail.
            ObtainInterpreterInfoOperation operation = null;
            Exception op_e = null;
            for (Tuple<String, String> interpreterNameAndExecutable : interpreterNameAndExecutables) {
                logger.println("- Chosen interpreter (name and file):'" + interpreterNameAndExecutable);

                if (interpreterNameAndExecutable != null && interpreterNameAndExecutable.o2 != null) {
                    try {
                        //ok, now that we got the file, let's see if it is valid and get the library info.
                        operation = InterpreterConfigHelpers.findInterpreter(
                                interpreterNameAndExecutable, interpreterManager,
                                !advanced, logger, null, shell);
                        if (operation != null) {
                            break;
                        }
                    } catch (Exception e) {
                        Log.log(e);
                        op_e = e;
                    }
                }
            }

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
            }
            else if (op_e != null) {
                throw op_e; //Only throw the final error
            }

        } catch (Exception e) {
            // Only show an error dialog on advanced auto-config, for which a single interpreter was chosen.
            if (!advanced) {
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
            }
            return false;
        } finally {
            Log.logInfo(charWriter.toString());
        }

        return false;
    }

    /**
     * Performs a search for valid interpreters.
     * 
     * @param interpreterType The interpreter's Python type: Python, Jython, or IronPython.
     * @param advanced Set to true if advanced auto-config is to be used, which allows users to choose
     * an interpreter out of the ones found.
     * @param cancelException
     */
    static List<Tuple<String, String>> autoConfig(final InterpreterType interpreterType, boolean advanced,
            final CancelException cancelException) {
        final List<Tuple<String, String>> interpreterNameAndExecutables = new ArrayList<Tuple<String, String>>();
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

            // If there are no providers at this point it means that
            // the selected target (python/jython/etc)
            // was no found to be available on any known location
            if (providers.size() != 0) {
                if (advanced && providers.size() > 1) {
                    // The user should select which one to use...
                    ListDialog listDialog = new ListDialog(EditorUtils.getShell());

                    listDialog.setContentProvider(new ArrayContentProvider());
                    listDialog.setLabelProvider(new LabelProvider() {
                        public Image getImage(Object element) {
                            return PydevPlugin.getImageCache().get(UIConstants.PY_INTERPRETER_ICON);
                        }

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

                    providers.clear();
                    providers.add((IInterpreterProvider) result[0]);
                }

                for (final IInterpreterProvider provider : providers) {
                    if (!provider.isInstalled()) {
                        SafeRunner.run(new SafeRunnable() {
                            public void run() throws Exception {
                                provider.runInstall();
                            }
                        });

                        if (!provider.isInstalled()) {
                            // if still not installed, user pressed cancel or an
                            // error was handled and displayed to the user during
                            // the thirdparty install process
                            throw cancelException;
                        }
                    }
                    String executable = provider.getExecutableOrJar();
                    if (executable != null) {
                        String name = provider.getName();
                        if (name == null) {
                            name = executable;
                        }
                        interpreterNameAndExecutables.add(new Tuple<String, String>(name, executable));
                    }
                }
            }

        } catch (CancelException e) {
            // user cancelled.
            return null;
        }
        if (interpreterNameAndExecutables.size() == 0) {
            String errorMsg = "Auto-configurer could not find a valid interpreter.\n"
                    + "Please manually configure a new interpreter instead.";
            ErrorDialog.openError(EditorUtils.getShell(), "Unable to auto-configure.", errorMsg,
                    PydevPlugin.makeStatus(IStatus.ERROR, "Unable to gather the needed info from the system.\n" + "\n"
                            + "This usually means that your interpreter is not in\n" + "the system PATH.", null));
            return null;
        }
        return interpreterNameAndExecutables;
    }
}
