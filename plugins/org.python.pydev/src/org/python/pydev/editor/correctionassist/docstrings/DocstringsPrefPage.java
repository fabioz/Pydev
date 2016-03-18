/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.correctionassist.docstrings;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;

/**
 * Preferences related to docstrings. These preferences are used by the
 * docstring content assistant.
 */

public class DocstringsPrefPage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage,
        IPropertyChangeListener {

    /* Preference identifiers */
    public static final String P_DOCSTRINGCHARACTER = "DOCSTRING CHARACTER";

    public static final String DEFAULT_P_DOCSTRINGCHARACTER = "'";

    public static final String P_DOCSTRINGSTYLE = "DOCSTRING STYLE";

    public static final String DOCSTRINGSTYLE_SPHINX = ":";

    public static final String DOCSTRINGSTYLE_EPYDOC = "@";

    public static final String DEFAULT_P_DOCSTIRNGSTYLE = DOCSTRINGSTYLE_SPHINX;

    public static final String TYPETAG_GENERATION_NEVER = "Never";

    public static final String TYPETAG_GENERATION_ALWAYS = "Always";

    public static final String TYPETAG_GENERATION_CUSTOM = "Custom";

    public static final String P_TYPETAGGENERATION = "TYPETAGGENERATION";

    public static final String DEFAULT_P_TYPETAGGENERATION = TYPETAG_GENERATION_NEVER;

    public static final String P_DONT_GENERATE_TYPETAGS = "DONT_GENERATE_TYPETAGS_PREFIXES";

    public static final String DEFAULT_P_DONT_GENERATE_TYPETAGS = "sz\0n\0f";

    public DocstringsPrefPage() {
        super(GRID);
        setPreferenceStore(PydevPlugin.getDefault().getPreferenceStore());
        setDescription("Docstring preferences");
    }

    /**
     * Getter for the preferred docstring character. Only a shortcut.
     * 
     * @return
     */
    public static String getPreferredDocstringCharacter() {
        if (SharedCorePlugin.inTestMode()) {
            return "'";//testing...

        }
        IPreferenceStore preferences = PydevPrefs.getPreferences();
        return preferences.getString(P_DOCSTRINGCHARACTER);
    }

    public static String getPreferredDocstringStyle() {
        if (SharedCorePlugin.inTestMode()) {
            return ":"; //testing
        }

        IPreferenceStore preferences = PydevPrefs.getPreferences();
        return preferences.getString(P_DOCSTRINGSTYLE);
    }

    private final static Map<String, String> strToMarker = new HashMap<String, String>();
    static {
        strToMarker.put("'", "'''");
        strToMarker.put("\"", "\"\"\"");
    }

    /**
     * 
     * @return The string that should be used to mark the beginning or end of a
     *         docstring. (""") or (''')
     */
    public static String getDocstringMarker() {
        String docstringChar = getPreferredDocstringCharacter();
        String ret = strToMarker.get(docstringChar);
        if (ret == null) {
            ret = docstringChar + docstringChar + docstringChar;
            strToMarker.put(docstringChar, ret);
        }
        return ret;
    }

    /**
     * Determines, from the preferences, whether a type tag should be generated
     * for a function / method parameter.
     * 
     * @param parameterName The name of the parameter.
     * @return true if it should be generated and false otherwise
     */
    public static boolean getTypeTagShouldBeGenerated(String parameterName) {
        if (SharedCorePlugin.inTestMode()) {
            return true;
        }
        String preference = PydevPrefs.getPreferences().getString(P_TYPETAGGENERATION);
        if (preference.equals(TYPETAG_GENERATION_NEVER)) {
            return false;
        } else if (preference.equals(TYPETAG_GENERATION_ALWAYS)) {
            return true;
        } else {// TYPETAG_GENERATION_CUSTOM - check prefix.
            String prefixesString = PydevPrefs.getPreferences().getString(P_DONT_GENERATE_TYPETAGS);
            StringTokenizer st = new StringTokenizer(prefixesString, "\0"); // "\0" is the separator

            while (st.hasMoreTokens()) {
                if (parameterName.startsWith(st.nextToken())) {
                    return false;
                }
            }

            return true; // No match
        }
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

        RadioGroupFieldEditor docstringCharEditor = new RadioGroupFieldEditor(P_DOCSTRINGCHARACTER,
                "Docstring character", 1,
                new String[][] { { "Quotation mark (\")", "\"" }, { "Apostrophe (')", "'" } }, p2, true);
        addField(docstringCharEditor);

        RadioGroupFieldEditor docstringStyleEditor = new RadioGroupFieldEditor(P_DOCSTRINGSTYLE, "Docstring style", 1,
                new String[][] { { "Sphinx (:tag name:)", DOCSTRINGSTYLE_SPHINX },
                        { "EpyDoc (@tag name:)", DOCSTRINGSTYLE_EPYDOC } }, p2, true);
        addField(docstringStyleEditor);

        Group typeDoctagGroup = new Group(p2, 0);
        typeDoctagGroup.setText("Type doctag generation (@type x:...)");
        typeDoctagEditor = new RadioGroupFieldEditor(P_TYPETAGGENERATION, "", 1, new String[][] {
                { "&Always", TYPETAG_GENERATION_ALWAYS }, { "&Never", TYPETAG_GENERATION_NEVER },
                { "&Custom", TYPETAG_GENERATION_CUSTOM } }, typeDoctagGroup);

        addField(typeDoctagEditor);
        addField(new ParameterNamePrefixListEditor(P_DONT_GENERATE_TYPETAGS, "Don't create for parameters with prefix",
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
