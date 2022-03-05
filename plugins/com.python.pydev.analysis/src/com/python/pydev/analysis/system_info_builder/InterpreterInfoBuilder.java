/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Andrew Ferrazzutti <aferrazz@redhat.com> - ongoing maintenance
******************************************************************************/
package com.python.pydev.analysis.system_info_builder;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.ast.codecompletion.revisited.ModulesFoundStructure;
import org.python.pydev.ast.codecompletion.revisited.ModulesManager;
import org.python.pydev.ast.codecompletion.revisited.PyPublicTreeMap;
import org.python.pydev.ast.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.ast.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.ast.interpreter_managers.IInterpreterInfoBuilder;
import org.python.pydev.ast.interpreter_managers.InterpreterInfo;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.logging.DebugSettings;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;

/**
 * @author fabioz
 */
public class InterpreterInfoBuilder implements IInterpreterInfoBuilder {

    public BuilderResult syncInfoToPythonPath(IProgressMonitor monitor, IPythonNature nature) {
        ICodeCompletionASTManager astManager = nature.getAstManager();
        if (astManager == null) {
            return BuilderResult.MUST_SYNCH_LATER;
        }

        PythonPathHelper pythonPathHelper = (PythonPathHelper) astManager.getModulesManager()
                .getPythonPathHelper();
        if (pythonPathHelper == null) {
            return BuilderResult.OK;
        }
        AbstractAdditionalDependencyInfo additionalInfo;
        try {
            additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature);
            IModulesManager modulesManager = astManager.getModulesManager();
            return this.syncInfoToPythonPath(monitor, pythonPathHelper, additionalInfo, modulesManager, null);
        } catch (MisconfigurationException e) {
            Log.log(e);
            return BuilderResult.OK;
        }
    }

    @Override
    public BuilderResult syncInfoToPythonPath(IProgressMonitor monitor, InterpreterInfo info) {
        ISystemModulesManager modulesManager = info.getModulesManager();
        IInterpreterManager manager = modulesManager.getInterpreterManager();
        AbstractAdditionalDependencyInfo additionalInfo;
        try {
            additionalInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(
                    manager,
                    info.getExecutableOrJar());
        } catch (MisconfigurationException e) {
            Log.log(e);
            return BuilderResult.OK;
        }

        return this.syncInfoToPythonPath(monitor, info, additionalInfo);
    }

    public BuilderResult syncInfoToPythonPath(IProgressMonitor monitor, InterpreterInfo info,
            AbstractAdditionalDependencyInfo additionalInfo) {
        ISystemModulesManager modulesManager = info.getModulesManager();
        PythonPathHelper pythonPathHelper = (PythonPathHelper) modulesManager.getPythonPathHelper();
        if (pythonPathHelper == null) {
            // Is this even possible?
            pythonPathHelper = new PythonPathHelper();
        }
        // Just making sure it's consistent at this point.
        pythonPathHelper.setPythonPath(info.libs);

        return this.syncInfoToPythonPath(monitor, pythonPathHelper, additionalInfo, modulesManager, info);
    }

    public BuilderResult syncInfoToPythonPath(IProgressMonitor monitor, PythonPathHelper pythonPathHelper,
            AbstractAdditionalDependencyInfo additionalInfo, IModulesManager modulesManager, InterpreterInfo info) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "--- Start run");
        }
        BuilderResult ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }

        ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(null, monitor);
        ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }

        PyPublicTreeMap<ModulesKey, ModulesKey> keysFound = ModulesManager.buildKeysFromModulesFound(monitor,
                modulesFound);

        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(
                    this,
                    StringUtils.format("Found: %s modules",
                            keysFound.size()));
        }
        ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }

        try {
            if (info != null) {
                String[] builtins = info.getBuiltins();
                //Note: consider builtins at this point: we do this only at this point and not in the regular process
                //(which would be the dialog where the interpreter is configured) because this can be a slow process
                //as we have to get the completions for all builtin modules from the shell.
                if (builtins != null) {
                    for (int i = 0; i < builtins.length; i++) {
                        String name = builtins[i];
                        final ModulesKey k = new ModulesKey(name, null);
                        //Note that it'll override source modules!
                        keysFound.put(k, k);
                    }
                }
            }
            synchronized (additionalInfo.updateKeysLock) {
                // Use a lock (if we have more than one builder updating we could get into a racing condition here).

                // Important: do the diff only after the builtins are added (otherwise the modules manager may become wrong)!
                Tuple<List<ModulesKey>, List<ModulesKey>> diffModules = modulesManager.diffModules(keysFound);

                if (diffModules.o1.size() > 0 || diffModules.o2.size() > 0) {
                    if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                        org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, StringUtils.format(
                                "Diff modules. Added: %s Removed: %s", diffModules.o1,
                                diffModules.o2));
                    }

                    //Update the modules manager itself (just pass all the keys as that should be fast)
                    if (modulesManager instanceof SystemModulesManager) {
                        ((SystemModulesManager) modulesManager).updateKeysAndSave(keysFound);
                    } else {
                        for (ModulesKey newEntry : diffModules.o1) {
                            modulesManager.addModule(newEntry);
                        }
                        modulesManager.removeModules(diffModules.o2);
                    }
                }
                additionalInfo.updateKeysIfNeededAndSave(keysFound, info, monitor);
            }

        } catch (Exception e) {
            Log.log(e);
        }

        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "--- End Run");
        }
        return BuilderResult.OK;
    }

    private BuilderResult checkEarlyReturn(IProgressMonitor monitor, InterpreterInfo info) {
        if (monitor.isCanceled()) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "Cancelled");
            }
            return BuilderResult.ABORTED;
        }

        if (info != null && !info.getLoadFinished()) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                org.python.pydev.shared_core.log.ToLogFile.toLogFile(this, "Load not finished (rescheduling)");
            }
            Log.log("The interpreter sync was cancelled (scheduling for checking the integrity later on again).\n"
                    + "To prevent any scheduling (at the cost of possible index corruptions),\n"
                    + "uncheck the setting at Preferences > PyDev > Interpreters.");
            return BuilderResult.MUST_SYNCH_LATER;
        }
        return BuilderResult.OK;
    }

}
