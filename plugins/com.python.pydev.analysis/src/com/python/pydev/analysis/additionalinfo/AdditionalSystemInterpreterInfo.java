/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 13, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PyStringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.interpreters.PythonInterpreterManager;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.AnalysisPlugin;

public class AdditionalSystemInterpreterInfo extends AbstractAdditionalInfoWithBuild {

    private final IInterpreterManager manager;
    private final String additionalInfoInterpreter;

    /**
     * holds system info (interpreter name points to system info)
     */
    private final static Map<Tuple<String, String>, AbstractAdditionalTokensInfo> additionalSystemInfo = new HashMap<Tuple<String, String>, AbstractAdditionalTokensInfo>();

    private final static Object additionalSystemInfoLock = new Object();

    private final File persistingFolder;

    private final File persistingLocation;

    public IInterpreterManager getManager() {
        return manager;
    }

    public String getAdditionalInfoInterpreter() {
        return additionalInfoInterpreter;
    }

    @Override
    protected String getUIRepresentation() {
        return manager != null ? manager.getManagerRelatedName() : "Unknown manager";
    }

    /**
     * @return the path to the folder we want to keep things on
     * @throws MisconfigurationException 
     */
    @Override
    protected File getPersistingFolder() {
        return persistingFolder;
    }

    @Override
    protected File getPersistingLocation() throws MisconfigurationException {
        return persistingLocation;
    }

    @Override
    protected Set<String> getPythonPathFolders() {
        Set<String> ret = new HashSet<>();
        try {
            IInterpreterInfo interpreterInfo = this.manager.getInterpreterInfo(additionalInfoInterpreter,
                    new NullProgressMonitor());
            ret.addAll(interpreterInfo.getPythonPath());
        } catch (MisconfigurationException e) {
            Log.log(e);
        }
        return ret;
    }

    public AdditionalSystemInterpreterInfo(IInterpreterManager manager, String interpreter)
            throws MisconfigurationException {
        super(false); //don't call init just right now...
        this.manager = manager;
        this.additionalInfoInterpreter = interpreter;

        File base;
        try {
            IPath stateLocation = AnalysisPlugin.getDefault().getStateLocation();
            base = stateLocation.toFile();
        } catch (Exception e) {
            //it may fail in tests... (save it in default folder in this cases)
            Log.logInfo("Error getting persisting folder", e);
            base = new File(".");
        }
        File file = new File(base, manager.getManagerRelatedName() + "_"
                + PyStringUtils.getExeAsFileSystemValidPath(this.additionalInfoInterpreter));

        try {
            if (!file.exists()) {
                file.mkdirs();
            }
        } catch (Exception e) {
            Log.log(e);
        }
        persistingFolder = file;
        persistingLocation = new File(persistingFolder, manager.getManagerRelatedName() + ".pydevsysteminfo");

        init();
    }

    public static AbstractAdditionalDependencyInfo getAdditionalSystemInfo(IInterpreterManager manager,
            String interpreter) throws MisconfigurationException {
        return getAdditionalSystemInfo(manager, interpreter, false);
    }

    /**
     * Should only be used in tests.
     */
    public static void setAdditionalSystemInfo(PythonInterpreterManager manager, String executableOrJar,
            AdditionalSystemInterpreterInfo additionalInfo) {
        synchronized (additionalSystemInfoLock) {
            Tuple<String, String> key = new Tuple<String, String>(manager.getManagerRelatedName(), executableOrJar);
            additionalSystemInfo.put(key, additionalInfo);
        }
    }

    /**
     * @param m the module manager that we want to get info on (python, jython...)
     * @return the additional info for the system
     * @throws MisconfigurationException 
     */
    public static AbstractAdditionalDependencyInfo getAdditionalSystemInfo(IInterpreterManager manager,
            String interpreter, boolean errorIfNotAvailable) throws MisconfigurationException {
        Tuple<String, String> key = new Tuple<String, String>(manager.getManagerRelatedName(), interpreter);
        synchronized (additionalSystemInfoLock) {
            AbstractAdditionalDependencyInfo info = (AbstractAdditionalDependencyInfo) additionalSystemInfo.get(key);
            if (info == null) {
                //lazy-load.
                info = new AdditionalSystemInterpreterInfo(manager, interpreter);
                additionalSystemInfo.put(key, info);

                if (!info.load()) {
                    try {
                        recreateAllInfo(manager, interpreter, new NullProgressMonitor());
                        info = (AbstractAdditionalDependencyInfo) additionalSystemInfo.get(key);
                    } catch (Exception e) {
                        Log.log(e);
                    }
                }
            }
            return info;
        }
    }

    public static void recreateAllInfo(IInterpreterManager manager, String interpreter, IProgressMonitor monitor) {
        synchronized (additionalSystemInfoLock) {
            try {
                final IInterpreterInfo interpreterInfo = manager.getInterpreterInfo(interpreter, monitor);
                int grammarVersion = interpreterInfo.getGrammarVersion();
                AbstractAdditionalTokensInfo currInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                        manager, interpreter);
                if (currInfo != null) {
                    currInfo.clearAllInfo();
                }
                InterpreterInfo defaultInterpreterInfo = (InterpreterInfo) manager.getInterpreterInfo(interpreter,
                        monitor);
                ISystemModulesManager m = defaultInterpreterInfo.getModulesManager();
                AbstractAdditionalTokensInfo info = restoreInfoForModuleManager(monitor, m,
                        "(system: " + manager.getManagerRelatedName() + " - " + interpreter + ")",
                        new AdditionalSystemInterpreterInfo(manager, interpreter), null, grammarVersion);

                if (info != null) {
                    //ok, set it and save it
                    additionalSystemInfo.put(new Tuple<String, String>(manager.getManagerRelatedName(), interpreter),
                            info);
                    info.save();
                }
            } catch (Throwable e) {
                Log.log(e);
            }
        }
    }

    //Make it available for being in a HashSet.

    @Override
    public int hashCode() {
        return this.additionalInfoInterpreter.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AdditionalSystemInterpreterInfo)) {
            return false;
        }
        AdditionalSystemInterpreterInfo additionalSystemInterpreterInfo = (AdditionalSystemInterpreterInfo) obj;
        return this.additionalInfoInterpreter.equals(additionalSystemInterpreterInfo.additionalInfoInterpreter);
    }

}
