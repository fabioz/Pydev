package com.python.pydev.debug;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;

public class DebugPluginPrefsInitializer extends AbstractPreferenceInitializer {

    public static final String PYDEV_REMOTE_DEBUGGER_PORT = "PYDEV_REMOTE_DEBUGGER_PORT";
    public static final int DEFAULT_REMOTE_DEBUGGER_PORT = 5678;

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);
        node.putInt(PYDEV_REMOTE_DEBUGGER_PORT, DEFAULT_REMOTE_DEBUGGER_PORT);
    }

    public static int getRemoteDebuggerPort() {
        return PydevPlugin.getDefault().getPreferenceStore().getInt(PYDEV_REMOTE_DEBUGGER_PORT);
    }

}
