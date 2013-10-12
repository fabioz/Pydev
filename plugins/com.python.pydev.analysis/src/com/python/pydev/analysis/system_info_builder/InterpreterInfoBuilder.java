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
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.ModulesFoundStructure;
import org.python.pydev.editor.codecompletion.revisited.PyPublicTreeMap;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.editor.codecompletion.revisited.SystemModulesManager;
import org.python.pydev.logging.DebugSettings;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.ui.pythonpathconf.IInterpreterInfoBuilder;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;

/**
 * @author fabioz
 */
public class InterpreterInfoBuilder implements IInterpreterInfoBuilder {

    public BuilderResult synchInfoToPythonPath(IProgressMonitor monitor, InterpreterInfo info) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }
        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            Log.toLogFile(this, "--- Start run");
        }
        BuilderResult ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }

        PythonPathHelper pythonPathHelper = new PythonPathHelper();
        pythonPathHelper.setPythonPath(info.libs);
        ModulesFoundStructure modulesFound = pythonPathHelper.getModulesFoundStructure(monitor);
        ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }

        SystemModulesManager modulesManager = (SystemModulesManager) info.getModulesManager();
        PyPublicTreeMap<ModulesKey, ModulesKey> keysFound = modulesManager.buildKeysFromModulesFound(monitor,
                modulesFound);

        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            Log.toLogFile(
                    this,
                    org.python.pydev.shared_core.string.StringUtils.format("Found: %s modules",
                            keysFound.size()));
        }
        ret = checkEarlyReturn(monitor, info);
        if (ret != BuilderResult.OK) {
            return ret;
        }
        Tuple<List<ModulesKey>, List<ModulesKey>> diffModules = modulesManager.diffModules(keysFound);
        if (diffModules.o1.size() > 0 || diffModules.o2.size() > 0) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                Log.toLogFile(this, org.python.pydev.shared_core.string.StringUtils.format(
                        "Diff modules. Added: %s Removed: %s", diffModules.o1,
                        diffModules.o2));
            }

            //Update the modules manager itself (just pass all the keys as that should be fast)
            modulesManager.updateKeysAndSave(keysFound);

            //Now, the additional info can be slower, so, let's work only on the deltas...
            IInterpreterManager manager = info.getModulesManager().getInterpreterManager();
            try {
                AbstractAdditionalDependencyInfo additionalSystemInfo = AdditionalSystemInterpreterInfo
                        .getAdditionalSystemInfo(manager, info.getExecutableOrJar());
                additionalSystemInfo.updateKeysIfNeededAndSave(keysFound);
            } catch (Exception e) {
                Log.log(e);
            }
        }

        if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
            Log.toLogFile(this, "--- End Run");
        }
        return BuilderResult.OK;
    }

    private BuilderResult checkEarlyReturn(IProgressMonitor monitor, InterpreterInfo info) {
        if (monitor.isCanceled()) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                Log.toLogFile(this, "Cancelled");
            }
            return BuilderResult.ABORTED;
        }

        if (!info.getLoadFinished()) {
            if (DebugSettings.DEBUG_INTERPRETER_AUTO_UPDATE) {
                Log.toLogFile(this, "Load not finished (rescheduling)");
            }
            return BuilderResult.MUST_SYNCH_LATER;
        }
        return BuilderResult.OK;
    }

}
