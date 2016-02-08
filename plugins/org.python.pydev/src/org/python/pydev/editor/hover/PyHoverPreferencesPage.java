/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.hover;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.progress.UIJob;
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

    public static final String COMBINE_HOVER_INFO = "combineHoverInfo";

    public static final boolean DEFAULT_COMBINE_HOVER_INFO = true;

    private PydevEditorHoverConfigurationBlock config;

    public PyHoverPreferencesPage() {
        setPreferenceStore();
        setDescription();
    }

    @Override
    public void init(IWorkbench workbench) {
        // pass
    }

    @Override
    public void createControl(Composite parent) {
        super.createControl(parent);
        //need to delay somewhat or it won't have any effect
        new UIJob("Show/Hide Preempt Column") {

            @Override
            public IStatus runInUIThread(IProgressMonitor monitor) {
                config.showPreemptColumn(PyHoverPreferencesPage.getCombineHoverInfo());
                return Status.OK_STATUS;
            }

        }.schedule(500);
    }

    /**@return
    
    whether the docstring should be shown when hovering.*/

    public static boolean getShowDocstringOnHover() {
        return PydevPrefs.getPreferences().getBoolean(SHOW_DOCSTRING_ON_HOVER);
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

    @Override
    protected IPreferenceConfigurationBlock createConfigurationBlock(OverlayPreferenceStore overlayPreferenceStore) {
        config = new PydevEditorHoverConfigurationBlock(this,
                overlayPreferenceStore);
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
