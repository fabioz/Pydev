package org.python.pydev.plugin.preferences;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Helper to deal with the pydev preferences.
 * 
 * @author Fabio
 */
public class PydevPrefs {
    
    /**
     * This is a preference store that combines the preferences for pydev with the general preferences for editors.
     */
    private static IPreferenceStore fChainedPrefStore;
    

    /**
     * @return the place where this plugin preferences are stored.
     */
    public static Preferences getPreferences() {
        return PydevPlugin.getDefault().getPluginPreferences();
    }

    
    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    public synchronized static IPreferenceStore getChainedPrefStore() {
        if(PydevPrefs.fChainedPrefStore == null){
            IPreferenceStore general = EditorsUI.getPreferenceStore();
            IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
            PydevPrefs.fChainedPrefStore = new ChainedPreferenceStore(new IPreferenceStore[] { preferenceStore, general });
        }
        return PydevPrefs.fChainedPrefStore;
    }
}
