/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.ui;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;
import org.python.pydev.core.cache.PyPreferencesCache;
import org.python.pydev.shared_ui.field_editors.LinkFieldEditor;

import com.python.pydev.refactoring.RefactoringPlugin;

public class MarkOccurrencesPreferencesPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    public static final String USE_MARK_OCCURRENCES = "USE_MARK_OCCURRENCES";
    public static final boolean DEFAULT_USE_MARK_OCCURRENCES = true;

    public static final String USE_MARK_OCCURRENCES_IN_STRINGS = "USE_MARK_OCCURRENCES_IN_STRINGS";
    public static final boolean DEFAULT_USE_MARK_OCCURRENCES_IN_STRINGS = true;

    private static PyPreferencesCache cache;

    public MarkOccurrencesPreferencesPage() {
        super(FLAT);
        IPreferenceStore prefs = RefactoringPlugin.getDefault().getPreferenceStore();
        setPreferenceStore(prefs);
    }

    @Override
    protected void createFieldEditors() {
        Composite p = getFieldEditorParent();

        addField(new BooleanFieldEditor(USE_MARK_OCCURRENCES, "Mark Occurrences?", p));
        addField(new BooleanFieldEditor(USE_MARK_OCCURRENCES_IN_STRINGS, "Mark Occurrences in strings and comments?", p));

        LinkFieldEditor colorsAndFontsLinkFieldEditor = new LinkFieldEditor("UNUSED",
                "Color of the occurences may be changed at\n" + "<a>Annotations</a>: Occurrences (Pydev)", p,
                new SelectionListener() {

                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        String id = "org.eclipse.ui.editors.preferencePages.Annotations";
                        IWorkbenchPreferenceContainer workbenchPreferenceContainer = ((IWorkbenchPreferenceContainer) getContainer());
                        workbenchPreferenceContainer.openPage(id, null);
                    }

                    @Override
                    public void widgetDefaultSelected(SelectionEvent e) {
                    }
                });
        colorsAndFontsLinkFieldEditor.getLinkControl(p);
    }

    @Override
    public void init(IWorkbench workbench) {
    }

    public static boolean useMarkOccurrences() {
        if (cache == null) {
            cache = new PyPreferencesCache(RefactoringPlugin.getDefault().getPreferenceStore());
        }
        return cache.getBoolean(USE_MARK_OCCURRENCES);
    }

    public static boolean useMarkOccurrencesInStrings() {
        if (cache == null) {
            cache = new PyPreferencesCache(RefactoringPlugin.getDefault().getPreferenceStore());
        }
        return cache.getBoolean(USE_MARK_OCCURRENCES_IN_STRINGS);
    }
}
