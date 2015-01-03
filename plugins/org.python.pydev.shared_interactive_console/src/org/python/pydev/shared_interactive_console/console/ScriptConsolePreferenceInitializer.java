/**
 * Copyright (c) 2013-2015 by Brainwy Software Ltda, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.shared_interactive_console.InteractiveConsolePlugin;
import org.python.pydev.shared_interactive_console.console.ui.ScriptConsoleUIConstants;

public class ScriptConsolePreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(InteractiveConsolePlugin.PLUGIN_ID);

        //console history
        node.putInt(ScriptConsoleUIConstants.INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES,
                ScriptConsoleUIConstants.DEFAULT_INTERACTIVE_CONSOLE_PERSISTENT_HISTORY_MAXIMUM_ENTRIES);
    }

}
