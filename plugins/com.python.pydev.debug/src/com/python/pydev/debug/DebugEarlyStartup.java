package com.python.pydev.debug;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.IStartup;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_ui.utils.RunInUiThread;

import com.python.pydev.debug.remote.IRemoteDebuggerListener;
import com.python.pydev.debug.remote.RemoteDebuggerServer;
import com.python.pydev.debug.remote.client_api.PydevRemoteDebuggerServer;

public class DebugEarlyStartup implements IStartup {

    private final Job checkAlwaysOnJob = new Job("Check debug server always on") {

        @Override
        protected IStatus run(IProgressMonitor monitor) {
            checkAlwaysOn(PydevPlugin.getDefault().getPreferenceStore());
            return Status.OK_STATUS;
        }
    };

    @Override
    public void earlyStartup() {
        //Note: preferences are in the PydevPlugin, not in the debug plugin.
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        preferenceStore.addPropertyChangeListener(new IPropertyChangeListener() {

            @Override
            public void propertyChange(PropertyChangeEvent event) {
                if (DebugPluginPrefsInitializer.DEBUG_SERVER_ALWAYS_ON.equals(event.getProperty())) {
                    //On a change in the preferences, re-check if it should be always on...
                    checkAlwaysOnJob.schedule(200);
                }
            }
        });

        RemoteDebuggerServer.getInstance().addListener(new IRemoteDebuggerListener() {

            @Override
            public void stopped(RemoteDebuggerServer remoteDebuggerServer) {
                //When it stops, re-check if it should be always on.
                checkAlwaysOnJob.schedule(200);
            }
        });
        checkAlwaysOnJob.schedule(500); //wait a little bit more to enable on startup.
    }

    public void checkAlwaysOn(final IPreferenceStore preferenceStore) {
        if (preferenceStore.getBoolean(DebugPluginPrefsInitializer.DEBUG_SERVER_ALWAYS_ON)) {
            boolean runNowIfInUiThread = true;
            Runnable r = new Runnable() {

                @Override
                public void run() {
                    //Check if it didn't change in the meanwhile...
                    if (preferenceStore.getBoolean(DebugPluginPrefsInitializer.DEBUG_SERVER_ALWAYS_ON)
                            && !PydevRemoteDebuggerServer.isRunning()) {
                        PydevRemoteDebuggerServer.startServer();
                    }
                }
            };
            RunInUiThread.async(r, runNowIfInUiThread);
        }
    }

}
