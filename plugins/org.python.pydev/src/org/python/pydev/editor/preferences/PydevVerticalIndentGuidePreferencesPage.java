/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ColorFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

public class PydevVerticalIndentGuidePreferencesPage extends FieldEditorPreferencePage implements
        IWorkbenchPreferencePage {

    private ColorFieldEditor selectionColorFieldEditor;
    private BooleanFieldEditor useEditorForegroundAsColorFieldEditor;
    private BooleanFieldEditor showVerticalindentGuideFieldEditor;
    private IntegerFieldEditor transparencyFieldEditor;

    public PydevVerticalIndentGuidePreferencesPage() {
        super(GRID);
        setDescription("Vertical Indent Guide");
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        showVerticalindentGuideFieldEditor = new BooleanFieldEditor(PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE,
                "Show vertical indent guide?", p);
        addField(showVerticalindentGuideFieldEditor);

        useEditorForegroundAsColorFieldEditor = new BooleanFieldEditor(
                PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND,
                "Use the editor foreground as the color?", p);
        addField(useEditorForegroundAsColorFieldEditor);

        selectionColorFieldEditor = new ColorFieldEditor(PydevEditorPrefs.VERTICAL_INDENT_COLOR,
                "Vertical indent guide color.", p);
        addField(selectionColorFieldEditor);

        transparencyFieldEditor = new IntegerFieldEditor(PydevEditorPrefs.VERTICAL_INDENT_TRANSPARENCY,
                "Vertical indent guide transparency\n(0 = transparent, 255 = opaque).", p);
        transparencyFieldEditor.setValidRange(0, 255);
        addField(transparencyFieldEditor);

        updateInitialState();
    }

    private void updateInitialState() {
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        boolean show = preferenceStore.getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE);
        update(show,
                show && !preferenceStore.getBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND));
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        super.propertyChange(event);
        updateCurrentState();
    }

    private void updateCurrentState() {
        boolean show = showVerticalindentGuideFieldEditor.getBooleanValue();
        update(show, show && !useEditorForegroundAsColorFieldEditor.getBooleanValue());
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        updateCurrentState();
    }

    private void update(boolean enableSelectEditorColor, boolean enableSelectionColor) {
        Composite p = getFieldEditorParent();
        useEditorForegroundAsColorFieldEditor.setEnabled(enableSelectEditorColor, p);
        selectionColorFieldEditor.setEnabled(enableSelectionColor, p);
        transparencyFieldEditor.setEnabled(enableSelectEditorColor, p);
    }

}
