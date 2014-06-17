/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 17, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.interpreters;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;
import org.python.copiedfromeclipsesrc.JDTNotAvailableException;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IInterpreterManagerListener;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IPythonPathNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SyncSystemModulesManagerScheduler;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.plugin.nature.PythonNatureListenersManager;
import org.python.pydev.shared_core.callbacks.ListenerList;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.AutoConfigMaker;
import org.python.pydev.ui.pythonpathconf.IInterpreterProviderFactory.InterpreterType;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * Does not write directly in INTERPRETER_PATH, just loads from it and works with it.
 *
 * @author Fabio Zadrozny
 */
public abstract class AbstractInterpreterManager implements IInterpreterManager {

    private int modificationStamp = 0;

    /**
     * This is the cache, that points from an interpreter to its information.
     */
    protected final Map<String, InterpreterInfo> exeToInfo = new HashMap<String, InterpreterInfo>();
    private final IPreferenceStore prefs;

    //caches that are filled at runtime -------------------------------------------------------------------------------
    /**
     * This is used to keep the builtin completions
     */
    protected final Map<String, IToken[]> builtinCompletions = new HashMap<String, IToken[]>();

    /**
     * This is used to keep the builtin module
     */
    protected final Map<String, IModule> builtinMod = new HashMap<String, IModule>();

    public void clearBuiltinCompletions(String projectInterpreterName) {
        this.builtinCompletions.remove(projectInterpreterName);
    }

    private ListenerList<IInterpreterManagerListener> listeners = new ListenerList<>(
            IInterpreterManagerListener.class);

    @Override
    public void addListener(IInterpreterManagerListener listener) {
        listeners.add(listener);
    }

    public IToken[] getBuiltinCompletions(String projectInterpreterName) {
        //Cache with the internal name.
        projectInterpreterName = getInternalName(projectInterpreterName);
        if (projectInterpreterName == null) {
            return null;
        }

        IToken[] toks = this.builtinCompletions.get(projectInterpreterName);

        if (toks == null || toks.length == 0) {
            IModule builtMod = getBuiltinMod(projectInterpreterName);
            if (builtMod != null) {
                toks = builtMod.getGlobalTokens();
                this.builtinCompletions.put(projectInterpreterName, toks);
            }
        }
        return this.builtinCompletions.get(projectInterpreterName);
    }

    public IModule getBuiltinMod(String projectInterpreterName) {
        //Cache with the internal name.
        projectInterpreterName = getInternalName(projectInterpreterName);
        if (projectInterpreterName == null) {
            return null;
        }
        IModule mod = builtinMod.get(projectInterpreterName);
        if (mod != null) {
            return mod;
        }

        try {
            InterpreterInfo interpreterInfo = this.getInterpreterInfo(projectInterpreterName, null);
            ISystemModulesManager modulesManager = interpreterInfo.getModulesManager();

            mod = modulesManager.getBuiltinModule("__builtin__", false);
            if (mod == null) {
                //Python 3.0 has builtins and not __builtin__
                mod = modulesManager.getBuiltinModule("builtins", false);
            }
            if (mod != null) {
                builtinMod.put(projectInterpreterName, mod);
            }

        } catch (MisconfigurationException e) {
            Log.log(e);
        }
        return builtinMod.get(projectInterpreterName);
    }

    private String getInternalName(String projectInterpreterName) {
        if (IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)) {
            //if it's the default, let's translate it to the outside world
            try {
                return this.getDefaultInterpreterInfo(true).getExecutableOrJar();
            } catch (NotConfiguredInterpreterException e) {
                Log.log(e);
                return projectInterpreterName;
            }
        }
        return projectInterpreterName;
    }

    public void clearBuiltinMod(String projectInterpreterName) {
        this.builtinMod.remove(projectInterpreterName);
    }

    /**
     * Constructor
     */
    @SuppressWarnings("unchecked")
    public AbstractInterpreterManager(IPreferenceStore prefs) {
        this.prefs = prefs;
        prefs.setDefault(getPreferenceName(), "");

        //Just called to force the information to be recreated!
        this.getInterpreterInfos();

        List<IInterpreterObserver> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
        for (IInterpreterObserver observer : participants) {
            observer.notifyInterpreterManagerRecreated(this);
        }
    }

    public boolean isConfigured() {
        try {
            String defaultInterpreter = getDefaultInterpreterInfo(false).getExecutableOrJar();
            if (defaultInterpreter == null) {
                return false;
            }
            if (defaultInterpreter.length() == 0) {
                return false;
            }
        } catch (NotConfiguredInterpreterException e) {
            return false;
        }
        return true;
    }

    public void clearCaches() {
        builtinMod.clear();
        builtinCompletions.clear();
        clearInterpretersFromPersistedString();
    }

    /**
     * @return the preference name where the options for this interpreter manager should be stored
     */
    protected abstract String getPreferenceName();

    /**
     * @throws NotConfiguredInterpreterException
     * @see org.python.pydev.core.IInterpreterManager#getDefaultInterpreterInfo()
     */
    public IInterpreterInfo getDefaultInterpreterInfo(boolean autoConfigureIfNotConfigured)
            throws NotConfiguredInterpreterException {
        IInterpreterInfo[] interpreters = getInterpreterInfos();
        String errorMsg = null;
        if (interpreters.length > 0) {
            IInterpreterInfo defaultInfo = interpreters[0];
            String interpreter = defaultInfo.getExecutableOrJar();
            if (interpreter == null) {
                errorMsg = "The configured interpreter for " + getInterpreterUIName()
                        + " is null, some error happened getting it.";
            }
            return defaultInfo;
        } else {
            errorMsg = getInterpreterUIName() + " not configured.";
        }

        if (autoConfigureIfNotConfigured) {
            //If we got here, the interpreter is not properly configured, let's try to auto-configure it
            if (PyDialogHelpers.getAskAgainInterpreter(this)) {
                configureInterpreterJob.addInterpreter(this);
                configureInterpreterJob.schedule(50);
            }
        }
        throw new NotConfiguredInterpreterException(errorMsg);
    }

    private static class ConfigureInterpreterJob extends UIJob {

        private volatile Set<AbstractInterpreterManager> interpreters = new HashSet<AbstractInterpreterManager>();

        public void addInterpreter(AbstractInterpreterManager abstractInterpreterManager) {
            this.interpreters.add(abstractInterpreterManager);
        }

        public ConfigureInterpreterJob() {
            super("Configure interpreter");
        }

        @Override
        public IStatus runInUIThread(IProgressMonitor monitor) {
            Set<AbstractInterpreterManager> current = interpreters;
            interpreters = new HashSet<AbstractInterpreterManager>();
            for (AbstractInterpreterManager m : current) {
                try {
                    m.getDefaultInterpreterInfo(false);
                    continue; //Maybe it got configured at some other point...
                } catch (NotConfiguredInterpreterException e) {
                    int ret = PyDialogHelpers.openQuestionConfigureInterpreter(m);
                    if (ret == InterpreterConfigHelpers.CONFIG_MANUAL) {
                        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                                m.getPreferencesPageId(), null, null);
                        dialog.open();
                    }
                    else if (ret != PyDialogHelpers.INTERPRETER_CANCEL_CONFIG) {
                        InterpreterType interpreterType;
                        switch (m.getInterpreterType()) {
                            case IPythonNature.INTERPRETER_TYPE_JYTHON:
                                interpreterType = InterpreterType.JYTHON;
                                break;

                            case IPythonNature.INTERPRETER_TYPE_IRONPYTHON:
                                interpreterType = InterpreterType.IRONPYTHON;
                                break;

                            default:
                                interpreterType = InterpreterType.PYTHON;
                        }
                        boolean advanced = ret == InterpreterConfigHelpers.CONFIG_ADV_AUTO;
                        Shell shell = EditorUtils.getShell();
                        AutoConfigMaker a = new AutoConfigMaker(interpreterType, advanced, null, null);
                        a.autoConfigSingleApply(null);
                    }
                }
            }
            return Status.OK_STATUS;
        }

    }

    private static ConfigureInterpreterJob configureInterpreterJob = new ConfigureInterpreterJob();

    /**
     * @return
     */
    protected abstract String getPreferencesPageId();

    /**
     * @return a message to show to the user when there is no configured interpreter
     */
    public abstract String getInterpreterUIName();

    private void clearInterpretersFromPersistedString() {
        synchronized (lock) {
            if (interpreterInfosFromPersistedString != null) {
                this.exeToInfo.clear();
                interpreterInfosFromPersistedString = null;
            }
        }

    }

    private volatile IInterpreterInfo[] interpreterInfosFromPersistedString;

    public IInterpreterInfo[] getInterpreterInfos() {
        return internalRecreateCacheGetInterpreterInfos();

    }

    private IInterpreterInfo[] internalRecreateCacheGetInterpreterInfos() {
        IInterpreterInfo[] interpreters = interpreterInfosFromPersistedString;
        if (interpreters == null) {
            synchronized (lock) {
                if (interpreterInfosFromPersistedString != null) {
                    //Some other thread restored it while we're locked.
                    interpreters = interpreterInfosFromPersistedString;

                } else {
                    interpreters = getInterpretersFromPersistedString(getPersistedString());
                    try {
                        this.exeToInfo.clear();
                        for (IInterpreterInfo info : interpreters) {
                            exeToInfo.put(info.getExecutableOrJar(), (InterpreterInfo) info);
                        }

                    } finally {
                        interpreterInfosFromPersistedString = interpreters;
                    }
                }

                for (IInterpreterInfo iInterpreterInfo : interpreterInfosFromPersistedString) {
                    iInterpreterInfo.setModificationStamp(modificationStamp);
                }
            }
        }
        return interpreters;
    }

    /**
     * Given an executable, should create the interpreter info that corresponds to it
     *
     * @param executable the executable that should be used to create the info
     * @param monitor a monitor to keep track of the info
     *
     * @return the interpreter info for the executable
     * @throws CoreException
     * @throws JDTNotAvailableException
     */
    protected abstract Tuple<InterpreterInfo, String> internalCreateInterpreterInfo(String executable,
            IProgressMonitor monitor, boolean askUser) throws CoreException, JDTNotAvailableException;

    /**
     * Creates the information for the passed interpreter.
     */
    public IInterpreterInfo createInterpreterInfo(String executable, IProgressMonitor monitor, boolean askUser) {

        monitor.worked(5);
        //ok, we have to get the info from the executable (and let's cache results for future use)...
        Tuple<InterpreterInfo, String> tup = null;
        InterpreterInfo info;
        try {

            tup = internalCreateInterpreterInfo(executable, monitor, askUser);
            if (tup == null) {
                //Canceled (in the dialog that asks the user to choose the valid paths)
                return null;
            }
            info = tup.o1;

        } catch (RuntimeException e) {
            Log.log(e);
            throw e;
        } catch (Exception e) {
            Log.log(e);
            throw new RuntimeException(e);
        }
        if (info.executableOrJar == null || info.executableOrJar.trim().length() == 0) {
            //it is null or empty
            final String title = "Invalid interpreter:" + executable;
            final String msg = "Unable to get information on interpreter!";
            String reasonCreation = "The interpreter (or jar): '" + executable
                    + "' is not valid - info.executable found: " + info.executableOrJar + "\n";
            if (tup != null) {
                reasonCreation += "The standard output gotten from the executed shell was: >>" + tup.o2 + "<<";
            }
            final String reason = reasonCreation;

            try {
                final Display disp = Display.getDefault();
                disp.asyncExec(new Runnable() {
                    public void run() {
                        ErrorDialog.openError(null, title, msg, new Status(Status.ERROR, PydevPlugin.getPluginID(), 0,
                                reason, null));
                    }
                });
            } catch (Throwable e) {
                // ignore error communication error
            }
            throw new RuntimeException(reason);
        }

        return info;

    }

    /**
     * Creates the interpreter info from the output. Checks for errors.
     */
    protected static InterpreterInfo createInfoFromOutput(IProgressMonitor monitor, Tuple<String, String> outTup,
            boolean askUser, String executableName, boolean executableIsUserSpecified) {
        if (outTup.o1 == null || outTup.o1.trim().length() == 0) {
            throw new RuntimeException(
                    "No output was in the standard output when"
                            + "\ntrying to create the interpreter info for: " + executableName
                            + "\nThe error output contains:>>" + outTup.o2 + "<<");
        }
        String executableToUse = executableIsUserSpecified ? executableName : null;
        if (executableToUse != null) {
            if (!new File(executableToUse).exists()) {
                //Only use the user-specified version if the executable does exist (otherwise use the internal info).
                executableToUse = null;
            }
        }
        InterpreterInfo info = InterpreterInfo.fromString(outTup.o1, askUser,
                executableToUse);
        return info;
    }

    /**
     * @throws MisconfigurationException
     * @see org.python.pydev.core.IInterpreterManager#getInterpreterInfo(java.lang.String)
     */
    public InterpreterInfo getInterpreterInfo(String nameOrExecutableOrJar, IProgressMonitor monitor)
            throws MisconfigurationException {
        synchronized (lock) {
            if (interpreterInfosFromPersistedString == null) {
                internalRecreateCacheGetInterpreterInfos(); //recreate cache!
            }
            for (IInterpreterInfo info : this.exeToInfo.values()) {
                if (info != null) {
                    if (info.matchNameBackwardCompatible(nameOrExecutableOrJar)) {
                        return (InterpreterInfo) info;
                    }
                }
            }
        }

        throw new MisconfigurationException(StringUtils.format(
                "Interpreter: %s not found", nameOrExecutableOrJar));
    }

    private Object lock = new Object();
    //little cache...
    private String persistedCache;
    private IInterpreterInfo[] persistedCacheRet;

    /**
     * @see org.python.pydev.core.IInterpreterManager#getInterpretersFromPersistedString(java.lang.String)
     */
    public IInterpreterInfo[] getInterpretersFromPersistedString(String persisted) {
        synchronized (lock) {
            if (persisted == null || persisted.trim().length() == 0) {
                return new IInterpreterInfo[0];
            }

            if (persistedCache == null || persistedCache.equals(persisted) == false) {
                List<IInterpreterInfo> ret = new ArrayList<IInterpreterInfo>();

                try {
                    List<InterpreterInfo> list = new ArrayList<InterpreterInfo>();
                    String[] strings = persisted.split("&&&&&");

                    //first, get it...
                    for (String string : strings) {
                        try {
                            list.add(InterpreterInfo.fromString(string, false));
                        } catch (Exception e) {
                            //ok, its format might have changed
                            String errMsg = "Interpreter storage changed.\r\n"
                                    + "Please restore it (window > preferences > Pydev > Interpreter)";
                            Log.log(errMsg, e);

                            return new IInterpreterInfo[0];
                        }
                    }

                    //then, put it in the list to be returned
                    for (InterpreterInfo info : list) {
                        if (info != null && info.executableOrJar != null) {
                            ret.add(info);
                        }
                    }

                    //and at last, restore the system info
                    for (final InterpreterInfo info : list) {
                        try {
                            info.getModulesManager().load();
                        } catch (Exception e) {
                            Log.logInfo(new RuntimeException("Restoring info for: " + info.getExecutableOrJar(), e));

                            info.setLoadFinished(false);
                            try {
                                //if it does not work it (probably) means that the internal storage format changed among versions,
                                //so, we have to recreate that info.

                                IProgressMonitor monitor = new NullProgressMonitor();
                                //ok, maybe its file-format changed... let's re-create it then.
                                info.restorePythonpath(monitor);
                                //after restoring it, let's save it.
                                info.getModulesManager().save();

                                //Note: All the code below is the same as the 2 lines above. It's no longer done needing the
                                //UI access because of issue: https://sourceforge.net/tracker/?func=detail&aid=3515102&group_id=85796&atid=577329
                                //(Can hang Eclipse at startup updating interpreter info)

                                //                                final Display def = Display.getDefault();
                                //                                def.syncExec(new Runnable(){
                                //
                                //                                    public void run() {
                                //                                        Shell shell = def.getActiveShell();
                                //                                        ProgressMonitorDialog dialog = new AsynchronousProgressMonitorDialog(shell);
                                //                                        dialog.setBlockOnOpen(false);
                                //                                        try {
                                //                                            dialog.run(false, false, new IRunnableWithProgress(){
                                //
                                //                                                public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
                                //                                                    monitor.beginTask("Updating the interpreter info.", 100);
                                //                                                    //ok, maybe its file-format changed... let's re-create it then.
                                //                                                    info.restorePythonpath(monitor);
                                //                                                    //after restoring it, let's save it.
                                //                                                    info.getModulesManager().save();
                                //                                                    monitor.done();
                                //                                                }}
                                //                                            );
                                //                                        } catch (Exception e) {
                                //                                            throw new RuntimeException(e);
                                //                                        }
                                //                                    }
                                //
                                //                                });
                            } finally {
                                info.setLoadFinished(true);
                            }
                            Log.logInfo(("Finished restoring information for: " + info.executableOrJar + " at: " + info
                                    .getModulesManager().getIoDirectory()));
                        }
                    }

                } catch (Exception e) {
                    Log.log(e);

                    //ok, some error happened (maybe it's not configured)
                    return new IInterpreterInfo[0];
                }

                persistedCache = persisted;
                persistedCacheRet = ret.toArray(new IInterpreterInfo[0]);
            }
        }
        return persistedCacheRet;
    }

    /**
     * @param executables executables that should be persisted
     * @return string to persist with the passed executables.
     */
    public static String getStringToPersist(IInterpreterInfo[] executables) {
        FastStringBuffer buf = new FastStringBuffer();
        for (IInterpreterInfo info : executables) {
            if (info != null) {
                buf.append(info.toString());
                buf.append("&&&&&");
            }
        }

        return buf.toString();
    }

    protected static File getInterpreterInfoPy() throws CoreException {
        File script = PydevPlugin.getScriptWithinPySrc("interpreterInfo.py");
        if (!script.exists()) {
            throw new RuntimeException("The file specified does not exist: " + script);
        }
        return script;
    }

    String persistedString;

    public String getPersistedString() {
        if (persistedString == null) {
            persistedString = prefs.getString(getPreferenceName());
        }
        return persistedString;
    }

    /* (non-Javadoc)
     * @see org.python.pydev.core.IInterpreterManager#setInfos(org.python.pydev.core.IInterpreterInfo[], java.util.Set, org.eclipse.core.runtime.IProgressMonitor)
     */
    public void setInfos(IInterpreterInfo[] infos, Set<String> interpreterNamesToRestore, IProgressMonitor monitor) {
        //Set the string to persist!
        String s = AbstractInterpreterManager.getStringToPersist(infos);
        prefs.setValue(getPreferenceName(), s);
        if (prefs instanceof IPersistentPreferenceStore) {
            try {
                //expected in tests: java.io.IOException: File name not specified
                ((IPersistentPreferenceStore) prefs).save();
            } catch (Exception e) {
                String message = e.getMessage();
                if (message == null || message.indexOf("File name not specified") == -1) {
                    Log.log(e);
                }
            }
        }

        IInterpreterInfo[] interpreterInfos;

        try {
            synchronized (this.lock) {
                modificationStamp += 1;
                clearInterpretersFromPersistedString();
                persistedString = s;
                //After setting the preference, get the actual infos (will be recreated).
                interpreterInfos = internalRecreateCacheGetInterpreterInfos();

                this.restorePythopathForInterpreters(monitor, interpreterNamesToRestore);
                //When we call performOk, the editor is going to store its values, but after actually restoring the modules, we
                //need to serialize the SystemModulesManager to be used when reloading the PydevPlugin

                //This method persists all the modules managers that are within this interpreter manager
                //(so, all the SystemModulesManagers will be saved -- and can be later restored).

                for (InterpreterInfo info : this.exeToInfo.values()) {
                    try {
                        ISystemModulesManager modulesManager = info.getModulesManager();
                        Object pythonPathHelper = modulesManager.getPythonPathHelper();
                        if (!(pythonPathHelper instanceof PythonPathHelper)) {
                            continue;
                        }
                        PythonPathHelper pathHelper = (PythonPathHelper) pythonPathHelper;
                        List<String> pythonpath = pathHelper.getPythonpath();
                        if (pythonpath == null || pythonpath.size() == 0) {
                            continue;
                        }
                        modulesManager.save();
                    } catch (Throwable e) {
                        Log.log(e);
                    }
                }
            }

            //Now, last step is updating the natures (the call must NOT be locked in this case).
            this.restorePythopathForNatures(monitor);

            //We also need to restart our code-completion shell after doing that, as we may have new environment variables!
            //And in jython, changing the classpath also needs to restore it.
            for (IInterpreterInfo interpreter : interpreterInfos) {
                for (int id : AbstractShell.getAllShellIds()) {
                    AbstractShell.stopServerShell(interpreter, id);
                }
            }
            IInterpreterManagerListener[] managerListeners = listeners.getListeners();
            for (IInterpreterManagerListener iInterpreterManagerListener : managerListeners) {
                iInterpreterManagerListener.afterSetInfos(this, interpreterInfos);
            }

        } finally {
            AbstractShell.restartAllShells();
        }

        //In the regular process we do not create the global indexing for forced builtins, thus, we schedule a process
        //now which will be able to do that when checking if things are correct in the configuration.
        PydevPlugin plugin = PydevPlugin.getDefault();
        if (plugin != null && interpreterNamesToRestore != null && interpreterNamesToRestore.size() > 0) {
            SyncSystemModulesManagerScheduler syncScheduler = plugin.syncScheduler;
            ArrayList<IInterpreterInfo> lst = new ArrayList<>(interpreterNamesToRestore.size());
            for (IInterpreterInfo info : interpreterInfos) {
                if (interpreterNamesToRestore.contains(info.getExecutableOrJar())) {
                    lst.add(info);
                }
            }
            syncScheduler.addToCheck(this, lst.toArray(new IInterpreterInfo[lst.size()]));
        }
    }

    /**
     * @param interpreterNamesToRestore if null, all interpreters are restored, otherwise, only the interpreters
     *      whose name is in this set are restored.
     *
     * Must be called with the synchronized(lock) in place!!
     */
    @SuppressWarnings("unchecked")
    private void restorePythopathForInterpreters(IProgressMonitor monitor, Set<String> interpretersNamesToRestore) {
        Set<Entry<String, InterpreterInfo>> entrySet = exeToInfo.entrySet();
        for (Entry<String, InterpreterInfo> entry : entrySet) {
            String interpreterExecutableOrJar = entry.getKey();
            if (interpretersNamesToRestore != null) {
                if (!interpretersNamesToRestore.contains(interpreterExecutableOrJar)) {
                    continue; //only restore the ones specified
                }
            }
            InterpreterInfo info = entry.getValue();
            info.restorePythonpath(monitor); //that's it, info.modulesManager contains the SystemModulesManager

            List<IInterpreterObserver> participants = ExtensionHelper
                    .getParticipants(ExtensionHelper.PYDEV_INTERPRETER_OBSERVER);
            for (IInterpreterObserver observer : participants) {
                try {
                    observer.notifyDefaultPythonpathRestored(this, interpreterExecutableOrJar, monitor);
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        }
    }

    private void restorePythopathForNatures(IProgressMonitor monitor) {
        IInterpreterInfo defaultInterpreterInfo;
        try {
            defaultInterpreterInfo = getDefaultInterpreterInfo(false);
        } catch (NotConfiguredInterpreterException e1) {
            defaultInterpreterInfo = null; //go on as usual... (the natures must know that they're not bound to an interpreter anymore).
        }

        FastStringBuffer buf = new FastStringBuffer();
        //Also notify that all the natures had the pythonpath changed (it's the system pythonpath, but still,
        //clients need to know about it)
        List<IPythonNature> pythonNatures;
        try {
            pythonNatures = PythonNature.getAllPythonNatures();
        } catch (IllegalStateException e1) {
            //java.lang.IllegalStateException: Workspace is closed.
            //      at org.eclipse.core.resources.ResourcesPlugin.getWorkspace(ResourcesPlugin.java:367)
            return;
        }
        for (IPythonNature nature : pythonNatures) {
            try {
                //If they have the same type of the interpreter manager, notify.
                if (this.getInterpreterType() == nature.getInterpreterType()) {
                    IPythonPathNature pythonPathNature = nature.getPythonPathNature();

                    //There's a catch here: if the nature uses some variable defined in the string substitution
                    //from the interpreter info, we need to do a full build instead of only making a notification.
                    String complete = pythonPathNature.getProjectExternalSourcePath(false)
                            + pythonPathNature.getProjectSourcePath(false);

                    PythonNature n = (PythonNature) nature;
                    String projectInterpreterName = n.getProjectInterpreterName();
                    IInterpreterInfo info;
                    if (IPythonNature.DEFAULT_INTERPRETER.equals(projectInterpreterName)) {
                        //if it's the default, let's translate it to the outside world
                        info = defaultInterpreterInfo;
                    } else {
                        synchronized (lock) {
                            info = exeToInfo.get(projectInterpreterName);
                        }
                    }

                    boolean makeCompleteRebuild = false;
                    if (info != null) {
                        Properties stringSubstitutionVariables = info.getStringSubstitutionVariables();
                        if (stringSubstitutionVariables != null) {
                            Enumeration<Object> keys = stringSubstitutionVariables.keys();
                            while (keys.hasMoreElements()) {
                                Object key = keys.nextElement();
                                buf.clear();
                                buf.append("${");
                                buf.append(key.toString());
                                buf.append("}");

                                if (complete.indexOf(buf.toString()) != -1) {
                                    makeCompleteRebuild = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (!makeCompleteRebuild) {
                        //just notify that it changed
                        if (nature instanceof PythonNature) {
                            ((PythonNature) nature).clearCaches(true);
                        }
                        PythonNatureListenersManager.notifyPythonPathRebuilt(nature.getProject(), nature);
                    } else {
                        //Rebuild the whole info.
                        nature.rebuildPath();
                    }
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

}
