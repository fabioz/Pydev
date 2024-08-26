/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.docstrings;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.core.docstrings.DocstringPreferences;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Preferences related to docstrings. These preferences are used by the
 * docstring content assistant.
 */

public class DocstringsPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage,
        IPropertyChangeListener {

    public DocstringsPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Docstring preferences");
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common
     * GUI blocks needed to manipulate various types of preferences. Each field
     * editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        Composite p = getFieldEditorParent();

        Composite p2 = new Composite(p, 0);
        p2.setLayout(new RowLayout());

        RadioGroupFieldEditor docstringCharEditor = new RadioGroupFieldEditor(DocstringPreferences.DOCSTRING_CHARACTER,
                "Docstring character", 1,
                new String[][] { { "Quotation mark (\")", "\"" }, { "Apostrophe (')", "'" } }, p2, true);
        addField(docstringCharEditor);

        RadioGroupFieldEditor docstringStyleEditor = new RadioGroupFieldEditor(DocstringPreferences.DOCSTRING_STYLE,
                "Docstring style", 1,
                new String[][] { { "Sphinx (:tag name:)", DocstringPreferences.DOCSTRING_STYLE_SPHINX },
                        { "EpyDoc (@tag name:)", DocstringPreferences.DOCSTRING_STYLE_EPYDOC },
                        { "Google (name:)", DocstringPreferences.DOCSTRING_STYLE_GOOGLE }
                },
                p2, true);
        addField(docstringStyleEditor);

        Group typeDoctagGroup = new Group(p2, 0);
        typeDoctagGroup.setText("Type doctag generation (@type x:...)");
        typeDoctagEditor = new RadioGroupFieldEditor(DocstringPreferences.TYPETAG_GENERATION, "", 1, new String[][] {
                { "&Always", DocstringPreferences.TYPETAG_GENERATION_ALWAYS },
                { "&Never", DocstringPreferences.TYPETAG_GENERATION_NEVER },
                { "&Custom", DocstringPreferences.TYPETAG_GENERATION_CUSTOM } }, typeDoctagGroup);

        addField(typeDoctagEditor);
        addField(new ParameterNamePrefixListEditor(DocstringPreferences.DONT_GENERATE_TYPETAGS,
                "Don't create for parameters with prefix",
                typeDoctagGroup));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
     */
    @Override
    public void init(IWorkbench workbench) {
    }

    private RadioGroupFieldEditor typeDoctagEditor;
}
