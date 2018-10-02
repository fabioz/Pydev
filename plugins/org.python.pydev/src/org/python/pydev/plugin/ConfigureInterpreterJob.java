package org.python.pydev.plugin;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.progress.UIJob;
import org.python.pydev.ast.interpreter_managers.AbstractInterpreterManager;
import org.python.pydev.ast.interpreter_managers.IInterpreterProviderFactory.InterpreterType;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.NotConfiguredInterpreterException;
import org.python.pydev.core.log.Log;
import org.python.pydev.ui.dialogs.PyDialogHelpers;
import org.python.pydev.ui.pythonpathconf.AutoConfigMaker;
import org.python.pydev.ui.pythonpathconf.InterpreterConfigHelpers;

public class ConfigureInterpreterJob extends UIJob {

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
                if (ret != PyDialogHelpers.INTERPRETER_CANCEL_CONFIG) {
                    if (ret == InterpreterConfigHelpers.CONFIG_MANUAL) {
                        PreferenceDialog dialog = PreferencesUtil.createPreferenceDialogOn(null,
                                m.getPreferencesPageId(), null, null);
                        dialog.open();
                    } else if (ret == InterpreterConfigHelpers.CONFIG_ADV_AUTO
                            || ret == InterpreterConfigHelpers.CONFIG_AUTO) {
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
                        AutoConfigMaker a = new AutoConfigMaker(interpreterType, advanced, null, null);
                        a.autoConfigSingleApply(null);
                    } else {
                        Log.log("Unexpected option: " + ret);
                    }
                }
            }
        }
        return Status.OK_STATUS;
    }

}
