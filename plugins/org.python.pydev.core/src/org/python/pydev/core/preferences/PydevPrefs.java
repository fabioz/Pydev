/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Helper to deal with the pydev preferences.
 *
 * @author Fabio
 */
public class PydevPrefs {

    public static IEclipsePreferences getEclipsePreferences() {
        return InstanceScope.INSTANCE.getNode("org.python.pydev");
    }

    public static IEclipsePreferences getDefaultEclipsePreferences() {
        return DefaultScope.INSTANCE.getNode("org.python.pydev");
    }

    public static IEclipsePreferences getAnalysisEclipsePreferences() {
        return getEclipsePreferences();
    }

    public static IEclipsePreferences getDefaultAnalysisEclipsePreferences() {
        return getDefaultEclipsePreferences();
    }

    public static IEclipsePreferences getComRefactoringEclipsePreferences() {
        return InstanceScope.INSTANCE.getNode("com.python.pydev.refactoring");
    }
}
