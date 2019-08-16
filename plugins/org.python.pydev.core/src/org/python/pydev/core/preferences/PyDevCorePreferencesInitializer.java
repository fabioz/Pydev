package org.python.pydev.core.preferences;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.shared_core.SharedCorePlugin;

public class PyDevCorePreferencesInitializer {

    public static void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);

        //ironpython
        node.put(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS,
                IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS);

        //text
        node.putBoolean(PyDevTypingPreferences.SMART_INDENT_PAR, PyDevTypingPreferences.DEFAULT_SMART_INDENT_PAR);
        node.putBoolean(PyDevTypingPreferences.AUTO_PAR, PyDevTypingPreferences.DEFAULT_AUTO_PAR);
        node.putBoolean(PyDevTypingPreferences.INDENT_AFTER_PAR_AS_PEP8,
                PyDevTypingPreferences.DEFAULT_INDENT_AFTER_PAR_AS_PEP8);
        node.putBoolean(PyDevTypingPreferences.AUTO_LINK, PyDevTypingPreferences.DEFAULT_AUTO_LINK);
        node.putBoolean(PyDevTypingPreferences.AUTO_INDENT_TO_PAR_LEVEL,
                PyDevTypingPreferences.DEFAULT_AUTO_INDENT_TO_PAR_LEVEL);
        node.putBoolean(PyDevTypingPreferences.AUTO_DEDENT_ELSE, PyDevTypingPreferences.DEFAULT_AUTO_DEDENT_ELSE);
        node.putInt(PyDevTypingPreferences.AUTO_INDENT_AFTER_PAR_WIDTH,
                PyDevTypingPreferences.DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH);
        node.putBoolean(PyDevTypingPreferences.AUTO_COLON, PyDevTypingPreferences.DEFAULT_AUTO_COLON);
        node.putBoolean(PyDevTypingPreferences.AUTO_BRACES, PyDevTypingPreferences.DEFAULT_AUTO_BRACES);
        node.putBoolean(PyDevTypingPreferences.AUTO_WRITE_IMPORT_STR,
                PyDevTypingPreferences.DEFAULT_AUTO_WRITE_IMPORT_STR);
        node.putBoolean(PyDevTypingPreferences.AUTO_LITERALS, PyDevTypingPreferences.DEFAULT_AUTO_LITERALS);
        node.putBoolean(PyDevTypingPreferences.SMART_LINE_MOVE, PyDevTypingPreferences.DEFAULT_SMART_LINE_MOVE);

        node.putInt(PyDevCoreEditorPreferences.TAB_WIDTH, PyDevCoreEditorPreferences.DEFAULT_TAB_WIDTH);

        //checkboxes
        node.putBoolean(PyDevCoreEditorPreferences.SUBSTITUTE_TABS, PyDevCoreEditorPreferences.DEFAULT_SUBSTITUTE_TABS);
        node.putBoolean(PyDevTypingPreferences.AUTO_ADD_SELF, PyDevTypingPreferences.DEFAULT_AUTO_ADD_SELF);
        node.putBoolean(PyDevCoreEditorPreferences.GUESS_TAB_SUBSTITUTION,
                PyDevCoreEditorPreferences.DEFAULT_GUESS_TAB_SUBSTITUTION);

        //code formatting
        node.putBoolean(PyFormatterPreferences.USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                PyFormatterPreferences.DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS);
        node.putBoolean(PyFormatterPreferences.USE_OPERATORS_WITH_SPACE,
                PyFormatterPreferences.DEFAULT_USE_OPERATORS_WITH_SPACE);
        node.putBoolean(PyFormatterPreferences.USE_SPACE_AFTER_COMMA,
                PyFormatterPreferences.DEFAULT_USE_SPACE_AFTER_COMMA);
        node.putBoolean(PyFormatterPreferences.ADD_NEW_LINE_AT_END_OF_FILE,
                PyFormatterPreferences.DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE);
        node.putBoolean(PyFormatterPreferences.FORMAT_ONLY_CHANGED_LINES,
                PyFormatterPreferences.DEFAULT_FORMAT_ONLY_CHANGED_LINES);
        node.putBoolean(PyFormatterPreferences.TRIM_LINES, PyFormatterPreferences.DEFAULT_TRIM_LINES);
        node.putBoolean(PyFormatterPreferences.USE_SPACE_FOR_PARENTESIS,
                PyFormatterPreferences.DEFAULT_USE_SPACE_FOR_PARENTESIS);
        node.putInt(PyFormatterPreferences.SPACES_BEFORE_COMMENT,
                PyFormatterPreferences.DEFAULT_SPACES_BEFORE_COMMENT);
        node.putInt(PyFormatterPreferences.SPACES_IN_START_COMMENT,
                PyFormatterPreferences.DEFAULT_SPACES_IN_START_COMMENT);
        node.putBoolean(PyFormatterPreferences.MANAGE_BLANK_LINES,
                PyFormatterPreferences.DEFAULT_MANAGE_BLANK_LINES);
        node.putInt(PyFormatterPreferences.BLANK_LINES_TOP_LEVEL,
                PyFormatterPreferences.DEFAULT_BLANK_LINES_TOP_LEVEL);
        node.putInt(PyFormatterPreferences.BLANK_LINES_INNER,
                PyFormatterPreferences.DEFAULT_BLANK_LINES_INNER);
        node.put(PyFormatterPreferences.BLACK_FORMATTER_LOCATION_OPTION,
                PyFormatterPreferences.DEFAULT_BLACK_FORMATTER_LOCATION_OPTION);

        //file types
        node.put(FileTypesPreferences.VALID_SOURCE_FILES, FileTypesPreferences.DEFAULT_VALID_SOURCE_FILES);
        node.put(FileTypesPreferences.FIRST_CHOICE_PYTHON_SOURCE_FILE,
                FileTypesPreferences.DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE);

        //general interpreters
        node.putBoolean(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_PY,
                InterpreterGeneralPreferences.DEFAULT_NOTIFY_NO_INTERPRETER_PY);
        node.putBoolean(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_JY,
                InterpreterGeneralPreferences.DEFAULT_NOTIFY_NO_INTERPRETER_JY);
        node.putBoolean(InterpreterGeneralPreferences.NOTIFY_NO_INTERPRETER_IP,
                InterpreterGeneralPreferences.DEFAULT_NOTIFY_NO_INTERPRETER_IP);

        node.putBoolean(InterpreterGeneralPreferences.CHECK_CONSISTENT_ON_STARTUP,
                InterpreterGeneralPreferences.DEFAULT_CHECK_CONSISTENT_ON_STARTUP);

        node.putBoolean(InterpreterGeneralPreferences.UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
                InterpreterGeneralPreferences.DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES);

    }

}
