package org.python.pydev.core.docstrings;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;

public class DocstringPreferences {

    /* Preference identifiers */
    public static final String DOCSTRING_CHARACTER = "DOCSTRING CHARACTER";
    public static final String DEFAULT_DOCSTRING_CHARACTER = "'";

    public static final String DOCSTRING_STYLE = "DOCSTRING STYLE";
    public static final String DOCSTRING_STYLE_SPHINX = ":";
    public static final String DOCSTRING_STYLE_EPYDOC = "@";
    public static final String DOCSTRING_STYLE_GOOGLE = "G";
    public static final String DEFAULT_DOCSTRING_STYLE = DOCSTRING_STYLE_SPHINX;

    public static final String TYPETAG_GENERATION_NEVER = "Never";
    public static final String TYPETAG_GENERATION_ALWAYS = "Always";
    public static final String TYPETAG_GENERATION_CUSTOM = "Custom";
    public static final String TYPETAG_GENERATION = "TYPETAGGENERATION";
    public static final String DEFAULT_TYPETAG_GENERATION = TYPETAG_GENERATION_NEVER;

    public static final String DONT_GENERATE_TYPETAGS = "DONT_GENERATE_TYPETAGS_PREFIXES";
    public static final String DEFAULT_DONT_GENERATE_TYPETAGS = "sz\0n\0f";

    /**
     * Getter for the preferred docstring character. Only a shortcut.
     *
     * @return
     */
    public static String getPreferredDocstringCharacter() {
        if (SharedCorePlugin.inTestMode()) {
            return "'";//testing...

        }
        return PydevPrefs.getEclipsePreferences().get(DOCSTRING_CHARACTER, DEFAULT_DOCSTRING_CHARACTER);
    }

    public static String getPreferredDocstringStyle() {
        if (SharedCorePlugin.inTestMode()) {
            return ":"; //testing
        }

        return PydevPrefs.getEclipsePreferences().get(DOCSTRING_STYLE, DEFAULT_DOCSTRING_STYLE);
    }

    public final static Map<String, String> strToMarker = new HashMap<String, String>();

    static {
        DocstringPreferences.strToMarker.put("'", "'''");
        DocstringPreferences.strToMarker.put("\"", "\"\"\"");
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

    public static boolean GENERATE_TYPE_DOCSTRING_ON_TESTS = true;

    /**
     * Determines, from the preferences, whether a type tag should be generated
     * for a function / method parameter.
     *
     * @param parameterName The name of the parameter.
     * @return true if it should be generated and false otherwise
     */
    public static boolean getTypeTagShouldBeGenerated(String parameterName) {
        if (SharedCorePlugin.inTestMode()) {
            return GENERATE_TYPE_DOCSTRING_ON_TESTS;
        }
        IEclipsePreferences preferences = PydevPrefs.getEclipsePreferences();
        String preference = preferences.get(TYPETAG_GENERATION,
                DEFAULT_TYPETAG_GENERATION);
        if (preference.equals(TYPETAG_GENERATION_NEVER)) {
            return false;
        } else if (preference.equals(TYPETAG_GENERATION_ALWAYS)) {
            return true;
        } else {// TYPETAG_GENERATION_CUSTOM - check prefix.
            String prefixesString = preferences.get(DONT_GENERATE_TYPETAGS,
                    DEFAULT_DONT_GENERATE_TYPETAGS);
            StringTokenizer st = new StringTokenizer(prefixesString, "\0"); // "\0" is the separator

            while (st.hasMoreTokens()) {
                if (parameterName.startsWith(st.nextToken())) {
                    return false;
                }
            }

            return true; // No match
        }
    }

}
