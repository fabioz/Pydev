package org.python.pydev.core.preferences;

public class PyDevTypingPreferences {

    public static final String AUTO_PAR = "AUTO_PAR";
    public static final boolean DEFAULT_AUTO_PAR = true;

    public static final String AUTO_LINK = "AUTO_LINK";
    public static final boolean DEFAULT_AUTO_LINK = false;

    public static final String AUTO_INDENT_TO_PAR_LEVEL = "AUTO_INDENT_TO_PAR_LEVEL";
    public static final boolean DEFAULT_AUTO_INDENT_TO_PAR_LEVEL = true;

    public static final String AUTO_INDENT_AFTER_PAR_WIDTH = "AUTO_INDENT_AFTER_PAR_WIDTH";
    public static final int DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH = 1;

    public static final String AUTO_DEDENT_ELSE = "AUTO_DEDENT_ELSE";
    public static final boolean DEFAULT_AUTO_DEDENT_ELSE = true;

    public static final String SMART_INDENT_PAR = "SMART_INDENT_PAR";
    public static final boolean DEFAULT_SMART_INDENT_PAR = true;

    public static final String INDENT_AFTER_PAR_AS_PEP8 = "INDENT_AFTER_PAR_AS_PEP8";
    public static final boolean DEFAULT_INDENT_AFTER_PAR_AS_PEP8 = true;

    public static final String SMART_LINE_MOVE = "SMART_LINE_MOVE";
    //Disabled by default (doesn't seem as useful as I though because Python does not have the end
    //braces and Java does (so, there are a number of cases where the indentation has to be hand-fixed
    //anyways)
    public static final boolean DEFAULT_SMART_LINE_MOVE = false;

    /**
     * fields for automatically replacing a colon
     * @see
     */
    public static final String AUTO_COLON = "AUTO_COLON";
    public static final boolean DEFAULT_AUTO_COLON = true;

    /**
     * fields for automatically skipping braces
     * @see  org.python.pydev.core.autoedit.PyAutoIndentStrategy
     */
    public static final String AUTO_BRACES = "AUTO_BRACES";
    public static final boolean DEFAULT_AUTO_BRACES = true;

    /**
     * Used if the 'import' should be written automatically in an from xxx import yyy
     */
    public static final String AUTO_WRITE_IMPORT_STR = "AUTO_WRITE_IMPORT_STR";
    public static final boolean DEFAULT_AUTO_WRITE_IMPORT_STR = true;

    public static final String AUTO_LITERALS = "AUTO_LITERALS";
    public static final boolean DEFAULT_AUTO_LITERALS = true;

    public static final String AUTO_ADD_SELF = "AUTO_ADD_SELF";
    public static final boolean DEFAULT_AUTO_ADD_SELF = true;

    public static final int TOOLTIP_WIDTH = 80;

}
