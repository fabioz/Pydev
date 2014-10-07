/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codefolding;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_ui.ColorCache;
import org.python.pydev.shared_ui.editor.IVerticalIndentGuidePreferencesProvider;

public abstract class PyAbstractIndentGuidePreferencesProvider implements IVerticalIndentGuidePreferencesProvider,
        IPropertyChangeListener {

    private boolean showIndentGuide;
    private boolean useEditorForegroundColor;
    private int transparency;
    private IPreferenceStore chainedPrefStore;

    public PyAbstractIndentGuidePreferencesProvider() {
        chainedPrefStore = PydevPrefs.getChainedPrefStore();
        showIndentGuide = chainedPrefStore.getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE);
        useEditorForegroundColor = chainedPrefStore
                .getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND);
        setTransparency(chainedPrefStore.getInt(PydevEditorPrefs.VERTICAL_INDENT_TRANSPARENCY));
        chainedPrefStore.addPropertyChangeListener(this);
    }

    private void setTransparency(int newVal) {
        if (newVal < 0) {
            newVal = 0;
        }
        if (newVal > 255) {
            newVal = 255;
        }
        transparency = newVal;
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
        if (PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE.equals(event.getProperty())) {
            this.showIndentGuide = chainedPrefStore.getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE);

        } else if (PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND.equals(event.getProperty())) {
            this.useEditorForegroundColor = chainedPrefStore
                    .getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND);

        } else if (PydevEditorPrefs.VERTICAL_INDENT_TRANSPARENCY.equals(event.getProperty())) {
            setTransparency(chainedPrefStore.getInt(PydevEditorPrefs.VERTICAL_INDENT_TRANSPARENCY));
        }
    }

    @Override
    public Color getColor(StyledText styledText) {
        if (useEditorForegroundColor) {
            return styledText.getForeground();
        }
        ColorCache colorCache = PydevPlugin.getColorCache();
        return colorCache.getColor(PydevEditorPrefs.VERTICAL_INDENT_COLOR);
    }

    @Override
    public int getTransparency() {
        return transparency;
    }
}
