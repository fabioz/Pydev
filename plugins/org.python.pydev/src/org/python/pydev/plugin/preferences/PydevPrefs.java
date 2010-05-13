package org.python.pydev.plugin.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.python.pydev.core.ExtensionHelper;
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
        	List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        	List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>();
        	for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
				IPreferenceStore preferenceStore = iPydevPreferencesProvider.getPreferenceStore();
				if(preferenceStore != null){
					stores.add(preferenceStore);
				}
			}
        	stores.add(PydevPlugin.getDefault().getPreferenceStore());
        	stores.add(EditorsUI.getPreferenceStore());
            PydevPrefs.fChainedPrefStore = new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
        }
        return PydevPrefs.fChainedPrefStore;
    }
}
