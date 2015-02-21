/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under1 the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.newconsole.prefs;

import org.python.pydev.shared_core.preferences.IScopedPreferences;

/**
 * The preferences for the commands are actually loaded/saved in
 */
public class InteractiveConsoleCommandsPreferences {

    private IScopedPreferences scopedPreferences;

    public InteractiveConsoleCommandsPreferences(IScopedPreferences scopedPreferences) {
        this.scopedPreferences = scopedPreferences;
        System.out.println(this.scopedPreferences.getWorkspaceSettingsLocation());
        System.out.println(this.scopedPreferences.getUserSettingsLocation());
    }
}
