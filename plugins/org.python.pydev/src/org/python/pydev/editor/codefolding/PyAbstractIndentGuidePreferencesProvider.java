package org.python.pydev.editor.codefolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.editor.IVerticalIndentGuidePreferencesProvider;

public abstract class PyAbstractIndentGuidePreferencesProvider implements IVerticalIndentGuidePreferencesProvider,
        IPropertyChangeListener {

    private boolean showIndentGuide;
    private IPreferenceStore chainedPrefStore;

    public PyAbstractIndentGuidePreferencesProvider() {
        chainedPrefStore = PydevPrefs.getChainedPrefStore();
        showIndentGuide = chainedPrefStore.getBoolean(PydevEditorPrefs.VERTICAL_INDENT_GUIDE);
        chainedPrefStore.addPropertyChangeListener(this);
    }

    @Override
    public boolean getShowIndentGuide() {
        return showIndentGuide;
    }

    @Override
    public void dispose() {
        if (chainedPrefStore != null) {
            chainedPrefStore.removePropertyChangeListener(this);
            chainedPrefStore = null;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if (PydevEditorPrefs.VERTICAL_INDENT_GUIDE.equals(event.getProperty())) {
            this.showIndentGuide = chainedPrefStore.getBoolean(PydevEditorPrefs.VERTICAL_INDENT_GUIDE);
        }
    }

}
