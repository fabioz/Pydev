/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.overview_ruler;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;

/**
 * @author fabioz
 *
 */
public class MinimapOverviewRulerPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_MINIMAP = "PYDEV_USE_MINIMAP";
    public static final String SHOW_SCROLLBAR = "PYDEV_SHOW_SCROLLBAR";
    public static final String SHOW_MINIMAP_CONTENTS = "PYDEV_SHOW_MINIMAP_CONTENTS";
    public static final String MINIMAP_WIDTH = "PYDEV_MINIMAP_WIDTH";

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
                "Show minimap? (applied on editor restart)", p);
        addField(useMinimap);

        BooleanFieldEditor showScrollbar = new BooleanFieldEditor(SHOW_SCROLLBAR,
                "Show scrollbar? (applied on editor restart)", p);
        addField(showScrollbar);

        BooleanFieldEditor showContents = new BooleanFieldEditor(SHOW_MINIMAP_CONTENTS,
                "Show text in overview ruler? (applied on text change)", p);
        addField(showContents);

        IntegerFieldEditor minimapWidth = new IntegerFieldEditor(MINIMAP_WIDTH,
                "Minimap Width: (applied on editor resize)", p);
        addField(minimapWidth);
    }

    public static boolean useMinimap() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(USE_MINIMAP);
    }

    public static boolean getShowMinimapContents() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_MINIMAP_CONTENTS);
    }

    public static boolean getShowScrollbar() {
        return PydevPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_SCROLLBAR);
    }

    private final static int MIN = 1;

    public static int getMinimapWidth() {
        IPreferenceStore preferenceStore = PydevPlugin.getDefault().getPreferenceStore();
        int i = preferenceStore.getInt(MINIMAP_WIDTH);
        if (i < MIN) {
            i = MIN;
        }
        return i;
    }

}
