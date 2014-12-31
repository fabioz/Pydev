package org.python.pydev.debug.profile;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.Properties;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.process.ProcessUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;
import org.python.pydev.shared_ui.utils.RunInUiThread;
import org.python.pydev.ui.dialogs.PyDialogHelpers;

public class PyProfilePreferences {

    public static final String ENABLE_PROFILING_FOR_NEW_LAUNCHES = "ENABLE_PROFILING_FOR_NEW_LAUNCHES";
    public static final String PYVMMONITOR_UI_LOCATION = "PYVMMONITOR_UI_LOCATION";
    public static final String PROFILE_MODE = "PROFILE_MODE";

    public static final int PROFILE_MODE_YAPPI = 0;
    public static final int PROFILE_MODE_LSPROF = 1;
    public static final int PROFILE_MODE_NONE = 2;

    // Volatile stuff (not persisted across restarts).
    public static boolean getAllRunsDoProfile() {
        return tempPreferenceStore.getBoolean(ENABLE_PROFILING_FOR_NEW_LAUNCHES);
    }

    private static PreferenceStore tempPreferenceStore = new PreferenceStore();
    static {
        tempPreferenceStore.setDefault(ENABLE_PROFILING_FOR_NEW_LAUNCHES, false);
    }

    public static IPreferenceStore getTemporaryPreferenceStore() {
        return tempPreferenceStore;
    }

    // Non-volatile stuff
    public static String getPyVmMonitorUILocation() {
        return getPermanentPreferenceStore().getString(PYVMMONITOR_UI_LOCATION);
    }

    private static boolean firstCall = true;
    private static final Object lock = new Object();

    public static IPreferenceStore getPermanentPreferenceStore() {
        IPreferenceStore preferenceStore = PydevPrefs.getPreferenceStore();
        if (firstCall) {
            synchronized (lock) {
                if (firstCall) {
                    firstCall = false;

                    //TODO: Cover other platforms!
                    if (PlatformUtils.isWindowsPlatform()) {
                        try {
                            //It may not be available in all versions of windows, but if it is, let's use it...
                            String env = System.getenv("LOCALAPPDATA");
                            if (env != null && env.length() > 0 && new File(env).exists()) {
                                File settings = new File(new File(env, "Brainwy"), "PyVmMonitor.ini");
                                if (settings.exists()) {
                                    Properties props = new Properties();
                                    props.load(new FileInputStream(settings));
                                    String property = props.getProperty("pyvmmonitor_ui_executable");
                                    preferenceStore.setDefault(PYVMMONITOR_UI_LOCATION, property);
                                }
                            }
                        } catch (Exception e) {
                            Log.log(e);
                        }
                    }
                    preferenceStore.setDefault(PROFILE_MODE, PROFILE_MODE_LSPROF);
                }
            }
        }
        return preferenceStore;
    }

    public static int getProfileMode() {
        return getPermanentPreferenceStore().getInt(PROFILE_MODE);
    }

    public static void addProfileArgs(List<String> cmdArgs, boolean profileRun, boolean actualRun) {
        if (profileRun) {
            // profile can use yappi or lsprof
            final String pyVmMonitorUILocation = PyProfilePreferences.getPyVmMonitorUILocation();
            if (pyVmMonitorUILocation == null || pyVmMonitorUILocation.length() == 0) {
                if (actualRun) {
                    RunInUiThread.async(new Runnable() {

                        public void run() {
                            PyDialogHelpers
                                    .openWarning(
                                            "Unable to run in profile mode.",
                                            "Unable to run in profile mode: pyvmmonitor-ui location not specified.");
                        }
                    });
                }
                return;

            }

            if (!new File(pyVmMonitorUILocation).exists()) {
                if (actualRun) {
                    RunInUiThread.async(new Runnable() {

                        public void run() {
                            PyDialogHelpers
                                    .openWarning(
                                            "Unable to run in profile mode.",
                                            "Unable to run in profile mode: Invalid location for pyvmmonitor-ui: "
                                                    + pyVmMonitorUILocation);
                        }
                    });
                }
                return;
            }

            // Ok, we have the pyvmmonitor-ui executable location, let's discover the pyvmmonitor.__init__ location
            // for doing the launch.
            File file = new File(pyVmMonitorUILocation);
            File publicApi = new File(file.getParentFile(), "public_api");
            File pyvmmonitorFolder = new File(publicApi, "pyvmmonitor");
            final File pyvmmonitorInit = new File(pyvmmonitorFolder, "__init__.py");
            if (!pyvmmonitorInit.exists()) {
                if (actualRun) {
                    RunInUiThread.async(new Runnable() {

                        public void run() {
                            PyDialogHelpers
                                    .openWarning(
                                            "Unable to run in profile mode.",
                                            "Unable to run in profile mode: Invalid location for pyvmmonitor/__init__.py: "
                                                    + FileUtils.getFileAbsolutePath(pyvmmonitorInit));
                        }
                    });
                }
                return;

            }

            // Now, for the profile to work we have to change the initial script to be pyvmmonitor.__init__.
            cmdArgs.add(FileUtils.getFileAbsolutePath(pyvmmonitorInit));

            int profileMode = PyProfilePreferences.getProfileMode();
            if (profileMode == PyProfilePreferences.PROFILE_MODE_YAPPI) {
                cmdArgs.add("--profile=yappi");
            } else if (profileMode == PyProfilePreferences.PROFILE_MODE_LSPROF) {
                cmdArgs.add("--profile=lsprof");
            } else {
                //Don't pass profile mode
            }

            // We'll spawn the UI ourselves (so, ask the backend to skip that step).
            // We have to do that because otherwise the process we launch will 'appear' to be live unless we
            // also close the profiler.
            cmdArgs.add("--spawn-ui=false");

            if (actualRun) {
                ProcessUtils.run(new String[] { pyVmMonitorUILocation, "--default-port-single-instance" }, null,
                        new File(pyVmMonitorUILocation).getParentFile(), null);
            }
        }
    }

}
