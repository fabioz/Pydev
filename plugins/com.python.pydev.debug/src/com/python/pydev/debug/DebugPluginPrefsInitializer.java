/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.debug;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.plugin.PydevPlugin;

public class DebugPluginPrefsInitializer extends AbstractPreferenceInitializer {

    public static final String PYDEV_REMOTE_DEBUGGER_PORT = "PYDEV_REMOTE_DEBUGGER_PORT";
    public static final int DEFAULT_REMOTE_DEBUGGER_PORT = 5678;

    public static final String DEBUG_SERVER_STARTUP = "DEBUG_SERVER_STARTUP";
    public static final int DEBUG_SERVER_MANUAL = 0;
    public static final int DEBUG_SERVER_ON_WHEN_PLUGIN_STARTED = 1;
    public static final int DEBUG_SERVER_KEEY_ALWAYS_ON = 2;
    public static final int DEFAULT_DEBUG_SERVER_ALWAYS_ON = DEBUG_SERVER_MANUAL;

    public static final String FORCE_SHOW_SHELL_ON_BREAKPOINT = "FORCE_SHOW_SHELL_ON_BREAKPOINT2";

    public static final int FORCE_SHOW_SHELL_ON_BREAKPOINT_MAKE_NOTHING = 0;
    public static final int FORCE_SHOW_SHELL_ON_BREAKPOINT_MAKE_ACTIVE = 1;
    public static final int FORCE_SHOW_SHELL_ON_BREAKPOINT_SHOW_INDETERMINATE_PROGRESS = 2;

    public static final int DEFAULT_FORCE_SHOW_SHELL_ON_BREAKPOINT = FORCE_SHOW_SHELL_ON_BREAKPOINT_MAKE_NOTHING;

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);
        node.putInt(PYDEV_REMOTE_DEBUGGER_PORT, DEFAULT_REMOTE_DEBUGGER_PORT);

        node.putInt(DEBUG_SERVER_STARTUP, DEFAULT_DEBUG_SERVER_ALWAYS_ON);
        node.putInt(FORCE_SHOW_SHELL_ON_BREAKPOINT, DEFAULT_FORCE_SHOW_SHELL_ON_BREAKPOINT);
    }

    public static int getRemoteDebuggerPort() {
        return PydevPlugin.getDefault().getPreferenceStore().getInt(PYDEV_REMOTE_DEBUGGER_PORT);
    }

}
