package org.python.pydev.compare;

import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.IViewerCreator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.python.pydev.plugin.preferences.PydevPrefs;


/**
 * Required when creating a PyMergeViewer from the plugin.xml file.
 */
public class PyContentViewerCreator implements IViewerCreator {

	public Viewer createViewer(Composite parent, CompareConfiguration mp) {
		return new PyMergeViewer(parent, SWT.NULL, updatePreferenceStore(mp));
	}

	/**
	 * Creates a new configuration with the pydev preference store so that the colors appear correctly when using
	 * Aptana themes.
	 */
    private CompareConfiguration updatePreferenceStore(CompareConfiguration mp) {
        List<IPreferenceStore> stores = PydevPrefs.getDefaultStores(false);
        IPreferenceStore prefs = mp.getPreferenceStore();
        if(prefs != null){
            //Note, we could use the CompareUIPlugin.getDefault().getPreferenceStore() directly, but it's access
            //is restricted, so, we go to the preferences of the previously created compare configuration.
            stores.add(prefs);
        }
        return new CompareConfiguration(new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()])));
    }
}
