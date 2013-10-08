package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.structure.OrderedSet;
import org.python.pydev.ui.pythonpathconf.IInterpreterInfoBuilder;
import org.python.pydev.ui.pythonpathconf.InterpreterInfo;

/**
 * This is a helper class to keep the PYTHONPATH of interpreters configured inside Eclipse with the PYTHONPATH that
 * the interpreter actually has currently.
 *
 * I.e.: doing on the command-line:
 *
 * d:\bin\Python265\Scripts\pip-2.6.exe install path.py --egg
 * d:\bin\Python265\Scripts\pip-2.6.exe uninstall path.py
 *
 * which will change the pythonpath should actually request a pythonpath update
 *
 *
 * Also, doing:
 *
 * d:\bin\Python265\Scripts\pip-2.6.exe install path.py
 * d:\bin\Python265\Scripts\pip-2.6.exe uninstall path.py
 *
 * which will only download the path.py without actually changing the pythonpath must also work!
 *
 * @author Fabio
 */
public class SynchSystemModulesManager {

    private static final boolean DEBUG = true;

    private static final Job job = new Job("Synch System PYTHONPATH") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            if (DEBUG) {
                System.out.println("SynchSystemModulesManager: job starting");
            }
            try {
                IInterpreterManager[] allInterpreterManagers = PydevPlugin.getAllInterpreterManagers();
                for (int i = 0; i < allInterpreterManagers.length; i++) {
                    IInterpreterManager manager = allInterpreterManagers[i];
                    IInterpreterInfo[] interpreterInfos = manager.getInterpreterInfos();
                    for (int j = 0; j < interpreterInfos.length; j++) {
                        IInterpreterInfo internalInfo = interpreterInfos[j];
                        String executable = internalInfo.getExecutableOrJar();
                        boolean askUser = false;
                        IInterpreterInfo newInterpreterInfo = manager.createInterpreterInfo(executable, monitor,
                                askUser);

                        OrderedSet<String> newEntries = new OrderedSet<String>(newInterpreterInfo.getPythonPath());
                        newEntries.removeAll(internalInfo.getPythonPath());

                        OrderedSet<String> removedEntries = new OrderedSet<String>(internalInfo.getPythonPath());
                        removedEntries.removeAll(newInterpreterInfo.getPythonPath());

                        if (newEntries.size() > 0 || removedEntries.size() > 0) {
                            System.out.println("Added to PYTHONPATH: " + newEntries);
                            System.out.println("Removed from PYTHONPATH: " + removedEntries);

                        } else {
                            System.out.println("SynchSystemModulesManager: info remained equal");
                        }

                        //If it was changed or not, we must check the internal structure too!
                        IInterpreterInfoBuilder builder = (IInterpreterInfoBuilder) ExtensionHelper.getParticipant(
                                ExtensionHelper.PYDEV_INTERPRETER_INFO_BUILDER, false);
                        builder.synchInfoToPythonPath(monitor, (InterpreterInfo) internalInfo);
                    }
                }
            } finally {
                this.schedule(20 * 1000); //Reschedule again for 30 seconds from now
            }
            return Status.OK_STATUS;
        }
    };

    public static void start() {
        job.setPriority(Job.BUILD);
        job.schedule(10 * 1000); //Wait 30 seconds to start doing something...
    }
}
