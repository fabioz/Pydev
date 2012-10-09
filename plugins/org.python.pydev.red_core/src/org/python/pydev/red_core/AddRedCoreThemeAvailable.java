/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.red_core;

import org.python.pydev.red_core.preferences.PydevRedCorePreferencesInitializer;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.theme.ThemePlugin;

/**
 * Helper just to know if red core is available.
 */
public class AddRedCoreThemeAvailable {

    private static volatile Boolean redCoreAvailable = null;

    public static boolean isRedCoreAvailableForTheming() {
        return (isRedCoreAvailable() && PydevRedCorePreferencesInitializer.getUseAptanaThemes());
    }

    public static boolean isRedCoreAvailable() {
        if (redCoreAvailable == null) {
            try {
                if (ThemePlugin.getDefault() != null && ThemePlugin.getDefault().getPreferenceStore() != null
                        && CommonEditorPlugin.getDefault() != null
                        && CommonEditorPlugin.getDefault().getPreferenceStore() != null) {
                    redCoreAvailable = true;
                } else {
                    redCoreAvailable = false;
                }
            } catch (Throwable e) {
                redCoreAvailable = false;//the plugin is not in the environment.
            }
        }
        return redCoreAvailable;
    }

    public static void setRedCoreAvailable(boolean b) {
        redCoreAvailable = false;
    }
}
