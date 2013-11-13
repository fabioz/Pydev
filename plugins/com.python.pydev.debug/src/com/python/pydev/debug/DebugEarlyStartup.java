package com.python.pydev.debug;

import java.lang.reflect.Method;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IDebugEventSetListener;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IStartup;
import org.python.pydev.core.log.Log;
import org.python.pydev.debug.model.PyThread;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.shared_ui.utils.UIUtils;

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

        DebugPlugin.getDefault().addDebugEventListener(new IDebugEventSetListener() {

            @Override
            public void handleDebugEvents(DebugEvent[] events) {
                if (events != null) {
                    for (DebugEvent debugEvent : events) {
                        if (debugEvent.getKind() == DebugEvent.SUSPEND) {
                            if (debugEvent.getDetail() == DebugEvent.BREAKPOINT) {
                                if (debugEvent.getSource() instanceof PyThread) {

                                    IPreferenceStore preferenceStore2 = PydevPlugin.getDefault().getPreferenceStore();
                                    if (preferenceStore2
                                            .getBoolean(DebugPluginPrefsInitializer.FORCE_SHOW_SHELL_ON_BREAKPOINT)) {
                                        Runnable r = new Runnable() {

                                            @Override
                                            public void run() {
                                                Shell activeShell = UIUtils.getActiveShell();
                                                if (activeShell != null) {
                                                    forceActive(activeShell);

                                                }
                                            }
                                        };
                                        boolean runNowIfInUiThread = true;
                                        RunInUiThread.async(r, runNowIfInUiThread);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /**
     * There are some issues with just forceActive as it doesn't actually bring it to the front on windows on some situations.
     * 
     * - https://bugs.eclipse.org/bugs/show_bug.cgi?id=192036: outlines the win32 solution implemented in here (using reflection to avoid issues compiling on other platforms).
     * 
     * Some possible alternatives: 
     * - we could change the text/icon in the taskbar (http://git.eclipse.org/c/platform/eclipse.platform.swt.git/tree/examples/org.eclipse.swt.snippets/src/org/eclipse/swt/snippets/Snippet336.java)
     * - Creating our own windows-dependent dll (but this is probably too much for the build process too) http://stackoverflow.com/questions/2773364/make-jface-window-blink-in-taskbar-or-get-users-attention
     * - https://github.com/jnr/jnr-ffi using the approach commented on http://stackoverflow.com/questions/2315560/how-do-you-force-a-java-swt-program-to-move-itself-to-the-foreground seems a possible acceptable workaround
     */
    public void forceActive(Shell shell) {
        //First, make sure it's not minimized
        shell.setMinimized(false);

        if (PlatformUtils.isWindowsPlatform()) {
            try {
                Class<?> OSClass = Class.forName("org.eclipse.swt.internal.win32.OS");

                Method hFromMethod = OSClass.getMethod("GetForegroundWindow");
                Method SetForegroundWindowMethod = OSClass.getMethod("SetForegroundWindow", int.class);
                Method GetWindowThreadProcessIdMethod = OSClass.getMethod("GetWindowThreadProcessId", int.class,
                        int[].class);

                int hFrom = (int) hFromMethod.invoke(OSClass);
                //int hFrom = OS.GetForegroundWindow();

                int shellHandle = shell.handle;
                if (hFrom <= 0) {
                    //OS.SetForegroundWindow(shell.handle);
                    SetForegroundWindowMethod.invoke(OSClass, shellHandle);
                    return;
                }

                if (shellHandle == hFrom) {
                    return;
                }

                //int pid = OS.GetWindowThreadProcessId(hFrom, null);
                int pid = (int) GetWindowThreadProcessIdMethod.invoke(OSClass, hFrom, null);

                //int _threadid = OS.GetWindowThreadProcessId(shell.handle, null);
                int _threadid = (int) GetWindowThreadProcessIdMethod.invoke(OSClass, shellHandle, null);

                if (_threadid == pid) {
                    //OS.SetForegroundWindow(shell.handle);
                    SetForegroundWindowMethod.invoke(OSClass, shellHandle);
                    return;
                }

                if (pid > 0) {
                    Method AttachThreadInputMethod = OSClass.getMethod("AttachThreadInput", int.class, int.class,
                            boolean.class);
                    //if (!OS.AttachThreadInput(_threadid, pid, true)) {
                    if (!((boolean) AttachThreadInputMethod.invoke(OSClass, _threadid, pid, true))) {
                        return;
                    }
                    //OS.SetForegroundWindow(shell.handle);
                    SetForegroundWindowMethod.invoke(OSClass, shellHandle);
                    //OS.AttachThreadInput(_threadid, pid, false);
                    AttachThreadInputMethod.invoke(OSClass, _threadid, pid, false);
                }

                //OS.BringWindowToTop(shell.handle);
                //OS.UpdateWindow(shell.handle);
                //OS.SetActiveWindow(shell.handle);
                for (String s : new String[] { "BringWindowToTop", "UpdateWindow", "SetActiveWindow" }) {
                    Method method = OSClass.getMethod(s, int.class);
                    method.invoke(OSClass, shellHandle);
                }
                return; //ok, workaround on win32 worked.
            } catch (Throwable e) {
                // Log and go the usual platform-independent route...
                Log.log(e);
            }
        }

        //As specified from http://www.eclipsezone.com/eclipse/forums/t28413.html:
        shell.forceActive();
        shell.setActive();
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
