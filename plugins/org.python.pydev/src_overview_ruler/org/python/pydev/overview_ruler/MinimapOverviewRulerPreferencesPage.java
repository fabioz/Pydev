/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author fabioz
 *
 */
public class MinimapOverviewRulerPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private static final String USE_MINIMAP = "PYDEV_USE_MINIMAP";

    public MinimapOverviewRulerPreferencesPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        BooleanFieldEditor useMinimap = new BooleanFieldEditor(USE_MINIMAP,
                "Use minimap in overview ruler? (NOTE: Only applied on editor restart)", p);
        addField(useMinimap);
    }

    public static boolean useMinimap() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(USE_MINIMAP);
    }

}
