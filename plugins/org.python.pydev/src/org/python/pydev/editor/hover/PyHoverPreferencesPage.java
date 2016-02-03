/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractConfigurationBlockPreferencePage;
import org.python.pydev.plugin.preferences.IPreferenceConfigurationBlock;
import org.python.pydev.plugin.preferences.OverlayPreferenceStore;
import org.python.pydev.plugin.preferences.PydevEditorHoverConfigurationBlock;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Preferences page for showing or not hovering info.
 * 
 * @author Fabio
 */
public class PyHoverPreferencesPage extends AbstractConfigurationBlockPreferencePage
        implements IWorkbenchPreferencePage {

    public static final String SHOW_DOCSTRING_ON_HOVER = "SHOW_DOCSTRING_ON_HOVER";

    public static final boolean DEFAULT_SHOW_DOCSTRING_ON_HOVER = true;

    public static final String SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = "SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER";

    public static final boolean DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = true;

    public static final String EDITOR_TEXT_HOVER_MODIFIERS = "hoverModifiers";

    public static final String EDITOR_TEXT_HOVER_MODIFIER_MASKS = "hoverModifierMasks";

    public static final String EDITOR_TEXT_HOVER_PRORITIES = "hoverPriorities";

    public static final String EDITOR_TEXT_HOVER_PREEMPTS = "hoverPreempts";

    public static final String EDITOR_ANNOTATION_ROLL_OVER = "editor_annotation_roll_over"; //$NON-NLS-1$

    public PyHoverPreferencesPage() {
        setPreferenceStore();
        setDescription();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite panel = (Composite) super.createContents(parent);
        final Button showDocstrings = new Button(parent, SWT.CHECK);
        showDocstrings.setText("Show docstrings?");
        showDocstrings.setSelection(getPreferenceStore().getBoolean(SHOW_DOCSTRING_ON_HOVER));
        showDocstrings.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getPreferenceStore().setValue(SHOW_DOCSTRING_ON_HOVER, showDocstrings.getSelection());
            }
        });

        final Button debugShowVars = new Button(parent, SWT.CHECK);
        debugShowVars.setText("Show variables values while debugging?");
        debugShowVars.setSelection(getPreferenceStore().getBoolean(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER));
        debugShowVars.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                getPreferenceStore().setValue(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER, debugShowVars.getSelection());
            }
        });
        return panel;
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * @return whether the docstring should be shown when hovering.
     */
    public static boolean getShowDocstringOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DOCSTRING_ON_HOVER);
    }

    /**
     * @return whether the value of variables should be shown on hover while debugging.
     */
    public static boolean getShowValuesWhileDebuggingOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
    }

    @Override
    protected IPreferenceConfigurationBlock createConfigurationBlock(OverlayPreferenceStore overlayPreferenceStore) {
        return new PydevEditorHoverConfigurationBlock(this, overlayPreferenceStore);
    }

    @Override
    protected String getHelpId() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void setDescription() {
        setDescription("Hover Preferences");
    }

    @Override
    protected void setPreferenceStore() {
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }
}
