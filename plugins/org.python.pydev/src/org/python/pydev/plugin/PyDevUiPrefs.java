package org.python.pydev.plugin;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public class PyDevUiPrefs {

    /**
     * This is a preference store that combines the preferences for pydev with the general preferences for editors.
     */
    static transient IPreferenceStore fChainedPrefStore;
    static final Object fChainedPrefStoreLock = new Object();

    public static IPreferenceStore getPreferenceStore() {
        return PydevPlugin.getDefault().getPreferenceStore();
    }

    public static List<IPreferenceStore> getDefaultStores(boolean addEditorsUIStore) {
        List<IPreferenceStore> stores = new ArrayList<IPreferenceStore>();
        stores.add(PydevPlugin.getDefault().getPreferenceStore());
        if (addEditorsUIStore) {
            stores.add(EditorsUI.getPreferenceStore());
        }
        return stores;
    }

    /**
     * @return a preference store that has the pydev preference store and the default editors text store
     */
    public synchronized static IPreferenceStore getChainedPrefStore() {
        if (fChainedPrefStore == null) {
            synchronized (fChainedPrefStoreLock) {
                List<IPreferenceStore> stores = PyDevUiPrefs.getDefaultStores(true);
                fChainedPrefStore = new ChainedPreferenceStore(
                        stores.toArray(new IPreferenceStore[stores.size()]));
            }
        }
        return fChainedPrefStore;
    }

}
