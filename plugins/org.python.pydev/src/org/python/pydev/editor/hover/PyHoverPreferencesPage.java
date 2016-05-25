/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.AbstractConfigurationBlockPreferencePage;
import org.python.pydev.plugin.preferences.IPreferenceConfigurationBlock;
import org.python.pydev.plugin.preferences.PydevPrefs;

/**
 * Preferences page for showing or not hovering info.
 *
 * @author Fabio
 */
public class PyHoverPreferencesPage extends AbstractConfigurationBlockPreferencePage
        implements IWorkbenchPreferencePage {

    public static final String SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = "SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER";

    public static final boolean DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER = true;

    public static final String EDITOR_TEXT_HOVER_MODIFIERS = "EDITOR_TEXT_HOVER_MODIFIERS";

    public static final String EDITOR_TEXT_HOVER_MODIFIER_MASKS = "EDITOR_TEXT_HOVER_MODIFIER_MASKS";

    public static final String KEY_TEXT_HOVER_MODIFIER = "PYDEV_TEXT_HOVER_MODIFIER_";

    public static final String KEY_TEXT_HOVER_MODIFIER_MASK = "PYDEV_TEXT_HOVER_MODIFIER_MASK_";

    public static final String KEY_TEXT_HOVER_PRIORITY = "PYDEV_TEXT_HOVER_PRORITY_";

    public static final String KEY_TEXT_HOVER_PREEMPT = "PYDEV_TEXT_HOVER_PREEMPT_";

    public static final String KEY_TEXT_HOVER_ENABLE = "PYDEV_TEXT_HOVER_ENABLE_";

    public static final String EDITOR_ANNOTATION_ROLL_OVER = "EDITOR_ANNOTATION_ROLL_OVER"; //$NON-NLS-1$

    public static final String COMBINE_HOVER_INFO = "COMBINE_HOVER_INFO";

    public static final boolean DEFAULT_COMBINE_HOVER_INFO = true;

    public static final String USE_HOVER_DIVIDER = "USE_HOVER_DIVIDER";

    public static final boolean DEFAULT_USE_HOVER_DIVIDER = true;

    private PyEditorHoverConfigurationBlock config;

    public PyHoverPreferencesPage() {
        setPreferenceStore();
        setDescription();
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

    /**
     * @return whether info from enabled Hovers should be combined.
     */
    public static boolean getCombineHoverInfo() {
        return PydevPrefs.getPreferences().getBoolean(COMBINE_HOVER_INFO);
    }

    /**
     * @return whether the value of variables should be shown on hover while debugging.
     */
    public static boolean getShowValuesWhileDebuggingOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
    }

    /**
     * @return whether to include a divider between text contributions when
     * combining info from multiple Hovers.
     */
    public static boolean getUseHoverDelimiters() {
        return PydevPrefs.getPreferences().getBoolean(USE_HOVER_DIVIDER);
    }

    @Override
    protected IPreferenceConfigurationBlock createConfigurationBlock() {
        config = new PyEditorHoverConfigurationBlock(this);
        return config;
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
