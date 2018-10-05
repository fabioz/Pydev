package org.python.pydev.core.formatter;

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.preferences.PyScopedPreferences;

public class PyFormatterPreferences {

    public static final String FORMATTER_STYLE = "FORMATTER_STYLE";

    // DEPRECATED IN FAVOR OF FORMATTER_STYLE
    public static final String FORMAT_WITH_AUTOPEP8 = "FORMAT_WITH_AUTOPEP8";
    public static final boolean DEFAULT_FORMAT_WITH_AUTOPEP8 = false;

    public static final String AUTOPEP8_PARAMETERS = "AUTOPEP8_PARAMETERS";

    public static final String FORMAT_ONLY_CHANGED_LINES = "FORMAT_ONLY_CHANGED_LINES";
    public static final boolean DEFAULT_FORMAT_ONLY_CHANGED_LINES = false;

    public static final String TRIM_LINES = "TRIM_EMPTY_LINES";
    public static final boolean DEFAULT_TRIM_LINES = false;

    public static final String TRIM_MULTILINE_LITERALS = "TRIM_MULTILINE_LITERALS";
    public static final boolean DEFAULT_TRIM_MULTILINE_LITERALS = false;

    public static final String ADD_NEW_LINE_AT_END_OF_FILE = "ADD_NEW_LINE_AT_END_OF_FILE";
    public static final boolean DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE = true;

    //a, b, c
    public static final String USE_SPACE_AFTER_COMMA = "USE_SPACE_AFTER_COMMA";
    public static final boolean DEFAULT_USE_SPACE_AFTER_COMMA = true;

    //call( a )
    public static final String USE_SPACE_FOR_PARENTESIS = "USE_SPACE_FOR_PARENTESIS";
    public static final boolean DEFAULT_USE_SPACE_FOR_PARENTESIS = false;

    //call(a = 1)
    public static final String USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS = "USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS";
    public static final boolean DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS = false;

    //operators =, !=, <, >, //, etc.
    public static final String USE_OPERATORS_WITH_SPACE = "USE_OPERATORS_WITH_SPACE";
    public static final boolean DEFAULT_USE_OPERATORS_WITH_SPACE = true;

    //Spaces before '#'.
    public static final String SPACES_BEFORE_COMMENT = "SPACES_BEFORE_COMMENT";
    public static final int DEFAULT_SPACES_BEFORE_COMMENT = 2; //pep-8 says 2 spaces before inline comment.

    //Spaces after '#'.
    public static final String SPACES_IN_START_COMMENT = "SPACES_IN_START_COMMENT";
    public static final int DEFAULT_SPACES_IN_START_COMMENT = 1; //pep-8 says 1 space after '#'

    // Leave at most 1 blank line by default
    public static final String MANAGE_BLANK_LINES = "MANAGE_BLANK_LINES";
    public static final boolean DEFAULT_MANAGE_BLANK_LINES = true;

    public static final String BLANK_LINES_TOP_LEVEL = "BLANK_LINES_TOP_LEVEL";
    public static final int DEFAULT_BLANK_LINES_TOP_LEVEL = 2;

    public static final String BLANK_LINES_INNER = "BLANK_LINES_INNER";
    public static final int DEFAULT_BLANK_LINES_INNER = 1;

    public static boolean getFormatWithAutopep8(IAdaptable projectAdaptable) {
        return getBoolean(FORMAT_WITH_AUTOPEP8, projectAdaptable);
    }

    public static boolean getBoolean(String setting, IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(setting, projectAdaptable);
    }

    public static String getString(String setting, IAdaptable projectAdaptable) {
        return PyScopedPreferences.getString(setting, projectAdaptable);
    }

    public static String getAutopep8Parameters(IAdaptable projectAdaptable) {
        return getString(AUTOPEP8_PARAMETERS, projectAdaptable);
    }

    public static boolean getFormatOnlyChangedLines(IAdaptable projectAdaptable) {
        if (getFormatWithAutopep8(projectAdaptable)) {
            return false; //i.e.: not available with autopep8.
        }
        return getBoolean(FORMAT_ONLY_CHANGED_LINES, projectAdaptable);
    }

    public static boolean getAddNewLineAtEndOfFile(IAdaptable projectAdaptable) {
        return getBoolean(ADD_NEW_LINE_AT_END_OF_FILE, projectAdaptable);
    }

    public static boolean getTrimLines(IAdaptable projectAdaptable) {
        return getBoolean(TRIM_LINES, projectAdaptable);
    }

    public static boolean getTrimMultilineLiterals(IAdaptable projectAdaptable) {
        return getBoolean(TRIM_MULTILINE_LITERALS, projectAdaptable);
    }

    public static boolean useSpaceAfterComma(IAdaptable projectAdaptable) {
        return getBoolean(USE_SPACE_AFTER_COMMA, projectAdaptable);
    }

    public static boolean useSpaceForParentesis(IAdaptable projectAdaptable) {
        return getBoolean(USE_SPACE_FOR_PARENTESIS, projectAdaptable);
    }

    public static boolean useAssignWithSpacesInsideParenthesis(IAdaptable projectAdaptable) {
        return getBoolean(USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS, projectAdaptable);
    }

    public static boolean useOperatorsWithSpace(IAdaptable projectAdaptable) {
        return getBoolean(USE_OPERATORS_WITH_SPACE, projectAdaptable);
    }

    public static int getSpacesBeforeComment(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SPACES_BEFORE_COMMENT, projectAdaptable, FormatStd.DONT_HANDLE_SPACES);
    }

    public static int getSpacesInStartComment(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(SPACES_IN_START_COMMENT, projectAdaptable, FormatStd.DONT_HANDLE_SPACES);
    }

    public static boolean getManageBlankLines(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getBoolean(MANAGE_BLANK_LINES, projectAdaptable);
    }

    public static int getBlankLinesTopLevel(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(BLANK_LINES_TOP_LEVEL, projectAdaptable, 0);
    }

    public static int getBlankLinesInner(IAdaptable projectAdaptable) {
        return PyScopedPreferences.getInt(BLANK_LINES_INNER, projectAdaptable, 0);
    }

    /**
     * @return the format standard that should be used to do the formatting
     */
    public static FormatStd getFormatStd(IAdaptable projectAdaptable) {
        FormatStd formatStd = new FormatStd();
        formatStd.assignWithSpaceInsideParens = useAssignWithSpacesInsideParenthesis(projectAdaptable);
        formatStd.operatorsWithSpace = useOperatorsWithSpace(projectAdaptable);
        formatStd.parametersWithSpace = useSpaceForParentesis(projectAdaptable);
        formatStd.spaceAfterComma = useSpaceAfterComma(projectAdaptable);
        formatStd.addNewLineAtEndOfFile = getAddNewLineAtEndOfFile(projectAdaptable);
        formatStd.trimLines = getTrimLines(projectAdaptable);
        formatStd.trimMultilineLiterals = getTrimMultilineLiterals(projectAdaptable);
        formatStd.spacesBeforeComment = getSpacesBeforeComment(projectAdaptable);
        formatStd.spacesInStartComment = getSpacesInStartComment(projectAdaptable);
        formatStd.formatWithAutopep8 = getFormatWithAutopep8(projectAdaptable);
        formatStd.autopep8Parameters = getAutopep8Parameters(projectAdaptable);
        formatStd.manageBlankLines = getManageBlankLines(projectAdaptable);
        formatStd.blankLinesTopLevel = getBlankLinesTopLevel(projectAdaptable);
        formatStd.blankLinesInner = getBlankLinesInner(projectAdaptable);
        formatStd.updateAutopep8();
        return formatStd;
    }

}
