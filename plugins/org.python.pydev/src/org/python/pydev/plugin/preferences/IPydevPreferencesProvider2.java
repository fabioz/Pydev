package org.python.pydev.plugin.preferences;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.editor.preferences.PydevEditorPrefs;

public interface IPydevPreferencesProvider2 {

    /**
     * @return true if it handled the creation of the color options and false otherwise.
     */
    boolean createColorOptions(Composite appearanceComposite, PydevEditorPrefs prefs);

}
