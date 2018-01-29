/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.plugin.preferences;

import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.IPreferenceStore;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.callbacks.ICallback0;

/**
 * Helper to deal with the pydev preferences.
 *
 * @author Fabio
 */
public class PydevPrefs {

    /**
     * This is a preference store that combines the preferences for pydev with the general preferences for editors.
     */
    private static transient IPreferenceStore fChainedPrefStore;
    private static final Object fChainedPrefStoreLock = new Object();

    public static ICallback<List<IPreferenceStore>, Boolean> getDefaultStores;
    public static ICallback0<IPreferenceStore> getPreferenceStore;
    public static ICallback0<IPreferenceStore> getChainedPrefStore;

    /**
     * @return the place where this plugin preferences are stored.
     */
    public static IPreferenceStore getPreferences() {
        return getPreferenceStore();
    }

    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    public synchronized static IPreferenceStore getChainedPrefStore() {
        if (PydevPrefs.fChainedPrefStore == null) {
            synchronized (fChainedPrefStoreLock) {
                if (PydevPrefs.fChainedPrefStore == null) {
                    Assert.isNotNull(getChainedPrefStore, "Callback must be set prior to use.");
                    PydevPrefs.fChainedPrefStore = getChainedPrefStore.call();
                }
            }
        }
        return PydevPrefs.fChainedPrefStore;
    }

    public static List<IPreferenceStore> getDefaultStores(boolean addEditorsUIStore) {
        Assert.isNotNull(getDefaultStores, "Callback must be set prior to use.");
        return getDefaultStores.call(addEditorsUIStore);
    }

    public static IPreferenceStore getPreferenceStore() {
        Assert.isNotNull(getPreferenceStore, "Callback must be set prior to use.");
        return getPreferenceStore.call();
    }

    public static IEclipsePreferences getEclipsePreferences() {
        return InstanceScope.INSTANCE.getNode("org.python.pydev");
    }

    public static IEclipsePreferences getDefaultEclipsePreferences() {
        return DefaultScope.INSTANCE.getNode("org.python.pydev");
    }

    public static IEclipsePreferences getAnalysisEclipsePreferences() {
        return InstanceScope.INSTANCE.getNode("com.python.pydev.analysis");
    }

    public static IEclipsePreferences getDefaultAnalysisEclipsePreferences() {
        return DefaultScope.INSTANCE.getNode("com.python.pydev.analysis");
    }
}
