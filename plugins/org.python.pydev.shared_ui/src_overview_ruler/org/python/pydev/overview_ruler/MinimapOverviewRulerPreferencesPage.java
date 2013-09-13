/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.shared_ui.SharedUiPlugin;

/**
 * @author fabioz
 *
 */
public class MinimapOverviewRulerPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_MINIMAP = "USE_MINIMAP";
    public static final String SHOW_SCROLLBAR = "SHOW_SCROLLBAR";
    public static final String SHOW_MINIMAP_CONTENTS = "SHOW_MINIMAP_CONTENTS";
    public static final String MINIMAP_WIDTH = "MINIMAP_WIDTH";

    public MinimapOverviewRulerPreferencesPage() {
        super(GRID);
        setPreferenceStore(SharedUiPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        BooleanFieldEditor useMinimap = new BooleanFieldEditor(USE_MINIMAP,
                "Show minimap? (applied on editor restart)", p);
        addField(useMinimap);

        //        BooleanFieldEditor showScrollbar = new BooleanFieldEditor(SHOW_SCROLLBAR,
        //                "Show scrollbar? (applied on editor restart)", p);
        //        addField(showScrollbar);

        BooleanFieldEditor showContents = new BooleanFieldEditor(SHOW_MINIMAP_CONTENTS,
                "Show text in overview ruler? (applied on text change)", p);
        addField(showContents);

        IntegerFieldEditor minimapWidth = new IntegerFieldEditor(MINIMAP_WIDTH,
                "Minimap Width: (applied on editor resize)", p);
        addField(minimapWidth);
    }

    public static boolean useMinimap() {
        return SharedUiPlugin.getDefault().getPreferenceStore().getBoolean(USE_MINIMAP);
    }

    public static boolean getShowMinimapContents() {
        return SharedUiPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_MINIMAP_CONTENTS);
    }

    public static boolean getShowScrollbar() {
        return true;
        //        return SharedUiPlugin.getDefault().getPreferenceStore().getBoolean(SHOW_SCROLLBAR);
    }

    private final static int MIN = 1;

    public static int getMinimapWidth() {
        IPreferenceStore preferenceStore = SharedUiPlugin.getDefault().getPreferenceStore();
        int i = preferenceStore.getInt(MINIMAP_WIDTH);
        if (i < MIN) {
            i = MIN;
        }
        return i;
    }

}
