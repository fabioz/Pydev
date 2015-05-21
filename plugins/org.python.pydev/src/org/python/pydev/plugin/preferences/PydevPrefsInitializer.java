/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 20/08/2005
 */
package org.python.pydev.plugin.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.resource.StringConverter;
import org.osgi.service.prefs.Preferences;
import org.python.pydev.builder.PyDevBuilderPrefPage;
import org.python.pydev.builder.todo.PyTodoPrefPage;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.editor.codefolding.PyDevCodeFoldingPrefPage;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.editor.correctionassist.docstrings.DocstringsPrefPage;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.editor.preferences.PydevTypingPrefs;
import org.python.pydev.editor.saveactions.PydevSaveActionsPrefPage;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.parser.PyParserManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;
import org.python.pydev.ui.pythonpathconf.InterpreterGeneralPreferencesPage;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;

public class PydevPrefsInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);

        //ironpython
        node.put(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS,
                IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS);

        //text
        node.putBoolean(PydevTypingPrefs.SMART_INDENT_PAR, PydevTypingPrefs.DEFAULT_SMART_INDENT_PAR);
        node.putBoolean(PydevTypingPrefs.AUTO_PAR, PydevTypingPrefs.DEFAULT_AUTO_PAR);
        node.putBoolean(PydevTypingPrefs.AUTO_LINK, PydevTypingPrefs.DEFAULT_AUTO_LINK);
        node.putBoolean(PydevTypingPrefs.AUTO_INDENT_TO_PAR_LEVEL, PydevTypingPrefs.DEFAULT_AUTO_INDENT_TO_PAR_LEVEL);
        node.putBoolean(PydevTypingPrefs.AUTO_DEDENT_ELSE, PydevTypingPrefs.DEFAULT_AUTO_DEDENT_ELSE);
        node.putInt(PydevTypingPrefs.AUTO_INDENT_AFTER_PAR_WIDTH, PydevTypingPrefs.DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH);
        node.putBoolean(PydevTypingPrefs.AUTO_COLON, PydevTypingPrefs.DEFAULT_AUTO_COLON);
        node.putBoolean(PydevTypingPrefs.AUTO_BRACES, PydevTypingPrefs.DEFAULT_AUTO_BRACES);
        node.putBoolean(PydevTypingPrefs.AUTO_WRITE_IMPORT_STR, PydevTypingPrefs.DEFAULT_AUTO_WRITE_IMPORT_STR);
        node.putBoolean(PydevTypingPrefs.AUTO_LITERALS, PydevTypingPrefs.DEFAULT_AUTO_LITERALS);
        node.putBoolean(PydevTypingPrefs.SMART_LINE_MOVE, PydevTypingPrefs.DEFAULT_SMART_LINE_MOVE);

        node.putInt(PydevEditorPrefs.TAB_WIDTH, PydevEditorPrefs.DEFAULT_TAB_WIDTH);
        node.putInt(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES,
                IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER);

        //comment blocks
        node.put(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR,
                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_CHAR);
        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME,
                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME);
        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME,
                CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME);
        node.put(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR,
                CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_CHAR);
        node.putBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT,
                CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);

        //checkboxes
        node.putBoolean(PydevEditorPrefs.SUBSTITUTE_TABS, PydevEditorPrefs.DEFAULT_SUBSTITUTE_TABS);
        node.putBoolean(PydevTypingPrefs.AUTO_ADD_SELF, PydevTypingPrefs.DEFAULT_AUTO_ADD_SELF);
        node.putBoolean(PydevEditorPrefs.GUESS_TAB_SUBSTITUTION, PydevEditorPrefs.DEFAULT_GUESS_TAB_SUBSTITUTION);
        node.putBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_GUIDE, PydevEditorPrefs.DEFAULT_USE_VERTICAL_INDENT_GUIDE);
        node.putBoolean(PydevEditorPrefs.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND,
                PydevEditorPrefs.DEFAULT_USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND);
        node.putInt(PydevEditorPrefs.VERTICAL_INDENT_TRANSPARENCY,
                PydevEditorPrefs.DEFAULT_VERTICAL_INDENT_TRANSPARENCY);

        //matching
        node.putBoolean(PydevEditorPrefs.USE_MATCHING_BRACKETS, PydevEditorPrefs.DEFAULT_USE_MATCHING_BRACKETS);
        node.put(PydevEditorPrefs.MATCHING_BRACKETS_COLOR,
                StringConverter.asString(PydevEditorPrefs.DEFAULT_MATCHING_BRACKETS_COLOR));
        node.putInt(PydevEditorPrefs.MATCHING_BRACKETS_STYLE, PydevEditorPrefs.DEFAULT_MATCHING_BRACKETS_STYLE);

        //colors
        node.put(PydevEditorPrefs.VERTICAL_INDENT_COLOR,
                StringConverter.asString(PydevEditorPrefs.DEFAULT_VERTICAL_INDENT_COLOR));
        node.put(PydevEditorPrefs.CODE_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_CODE_COLOR));
        node.put(PydevEditorPrefs.NUMBER_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_NUMBER_COLOR));
        node.put(PydevEditorPrefs.DECORATOR_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_DECORATOR_COLOR));
        node.put(PydevEditorPrefs.KEYWORD_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_KEYWORD_COLOR));
        node.put(PydevEditorPrefs.SELF_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_SELF_COLOR));
        node.put(PydevEditorPrefs.STRING_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_STRING_COLOR));
        node.put(PydevEditorPrefs.UNICODE_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_UNICODE_COLOR));
        node.put(PydevEditorPrefs.COMMENT_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_COMMENT_COLOR));
        node.put(PydevEditorPrefs.BACKQUOTES_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_BACKQUOTES_COLOR));
        node.put(PydevEditorPrefs.CLASS_NAME_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_CLASS_NAME_COLOR));
        node.put(PydevEditorPrefs.FUNC_NAME_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_FUNC_NAME_COLOR));
        node.put(PydevEditorPrefs.PARENS_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_PARENS_COLOR));
        node.put(PydevEditorPrefs.OPERATORS_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_OPERATORS_COLOR));
        node.put(PydevEditorPrefs.DOCSTRING_MARKUP_COLOR,
                StringConverter.asString(PydevEditorPrefs.DEFAULT_DOCSTRING_MARKUP_COLOR));
        //for selection colors see initializeDefaultColors()

        //font style
        node.putInt(PydevEditorPrefs.CODE_STYLE, PydevEditorPrefs.DEFAULT_CODE_STYLE);
        node.putInt(PydevEditorPrefs.NUMBER_STYLE, PydevEditorPrefs.DEFAULT_NUMBER_STYLE);
        node.putInt(PydevEditorPrefs.DECORATOR_STYLE, PydevEditorPrefs.DEFAULT_DECORATOR_STYLE);
        node.putInt(PydevEditorPrefs.KEYWORD_STYLE, PydevEditorPrefs.DEFAULT_KEYWORD_STYLE);
        node.putInt(PydevEditorPrefs.SELF_STYLE, PydevEditorPrefs.DEFAULT_SELF_STYLE);
        node.putInt(PydevEditorPrefs.STRING_STYLE, PydevEditorPrefs.DEFAULT_STRING_STYLE);
        node.putInt(PydevEditorPrefs.UNICODE_STYLE, PydevEditorPrefs.DEFAULT_UNICODE_STYLE);
        node.putInt(PydevEditorPrefs.COMMENT_STYLE, PydevEditorPrefs.DEFAULT_COMMENT_STYLE);
        node.putInt(PydevEditorPrefs.BACKQUOTES_STYLE, PydevEditorPrefs.DEFAULT_BACKQUOTES_STYLE);
        node.putInt(PydevEditorPrefs.CLASS_NAME_STYLE, PydevEditorPrefs.DEFAULT_CLASS_NAME_STYLE);
        node.putInt(PydevEditorPrefs.FUNC_NAME_STYLE, PydevEditorPrefs.DEFAULT_FUNC_NAME_STYLE);
        node.putInt(PydevEditorPrefs.PARENS_STYLE, PydevEditorPrefs.DEFAULT_PARENS_STYLE);
        node.putInt(PydevEditorPrefs.OPERATORS_STYLE, PydevEditorPrefs.DEFAULT_OPERATORS_STYLE);
        node.putInt(PydevEditorPrefs.DOCSTRING_MARKUP_STYLE, PydevEditorPrefs.DEFAULT_DOCSTRING_MARKUP_STYLE);

        //Debugger
        node.putInt(PydevEditorPrefs.CONNECT_TIMEOUT, PydevEditorPrefs.DEFAULT_CONNECT_TIMEOUT);
        node.putBoolean(PydevEditorPrefs.RELOAD_MODULE_ON_CHANGE, PydevEditorPrefs.DEFAULT_RELOAD_MODULE_ON_CHANGE);
        node.putBoolean(PydevEditorPrefs.DONT_TRACE_ENABLED, PydevEditorPrefs.DEFAULT_DONT_TRACE_ENABLED);
        node.putBoolean(PydevEditorPrefs.DEBUG_MULTIPROCESSING_ENABLED,
                PydevEditorPrefs.DEFAULT_DEBUG_MULTIPROCESSING_ENABLED);
        node.putBoolean(PydevEditorPrefs.KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS,
                PydevEditorPrefs.DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS);
        node.putBoolean(PydevEditorPrefs.GEVENT_DEBUGGING, PydevEditorPrefs.DEFAULT_GEVENT_DEBUGGING);
        node.putBoolean(PydevEditorPrefs.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS,
                PydevEditorPrefs.DEFAULT_TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS);

        //pydev todo tasks
        node.put(PyTodoPrefPage.PY_TODO_TAGS, PyTodoPrefPage.DEFAULT_PY_TODO_TAGS);

        //builders
        node.putBoolean(PyDevBuilderPrefPage.USE_PYDEV_BUILDERS, PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_BUILDERS);
        node.putBoolean(PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
                PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);
        node.putInt(PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS,
                PyDevBuilderPrefPage.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);
        node.putBoolean(PyDevBuilderPrefPage.ANALYZE_ONLY_ACTIVE_EDITOR,
                PyDevBuilderPrefPage.DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
        node.putBoolean(PyDevBuilderPrefPage.REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED,
                PyDevBuilderPrefPage.DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
        node.putInt(PyDevBuilderPrefPage.PYC_DELETE_HANDLING, PyDevBuilderPrefPage.DEFAULT_PYC_DELETE_HANDLING);

        //code folding
        node.putBoolean(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING, PyDevCodeFoldingPrefPage.DEFAULT_USE_CODE_FOLDING);

        /*[[[cog
        import cog
        template = '''node.putBoolean(PyDevCodeFoldingPrefPage.%s,
                PyDevCodeFoldingPrefPage.DEFAULT_%s);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_%s,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_%s);
        '''
        import folding_entries
        for s in folding_entries.FOLDING_ENTRIES:
            cog.outl(template % (s, s, s, s))
            
        ]]]*/
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IMPORTS,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IMPORTS);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_IMPORTS,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_IMPORTS);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_CLASSDEF);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_CLASSDEF,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_CLASSDEF);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FUNCTIONDEF);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_FUNCTIONDEF,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_FUNCTIONDEF);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_COMMENTS,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_COMMENTS);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_COMMENTS,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_COMMENTS);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_STRINGS,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_STRINGS);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_STRINGS,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_STRINGS);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IF,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IF);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_IF,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_IF);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WHILE,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WHILE);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_WHILE,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_WHILE);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WITH,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WITH);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_WITH,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_WITH);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_TRY,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_TRY);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_TRY,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_TRY);

        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FOR,
                PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FOR);
        node.putBoolean(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_FOR,
                PyDevCodeFoldingPrefPage.DEFAULT_INITIALLY_FOLD_FOR);

        //[[[end]]]

        //coding style
        node.putBoolean(PyCodeStylePreferencesPage.USE_LOCALS_AND_ATTRS_CAMELCASE,
                PyCodeStylePreferencesPage.DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE);
        node.putInt(PyCodeStylePreferencesPage.USE_METHODS_FORMAT,
                PyCodeStylePreferencesPage.DEFAULT_USE_METHODS_FORMAT);

        //Editor title
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_NAMES_UNIQUE,
                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_NAMES_UNIQUE);
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_SHOW_EXTENSION,
                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_SHOW_EXTENSION);
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON,
                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_CUSTOM_INIT_ICON);
        node.put(PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING,
                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_INIT_HANDLING);
        node.put(PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_HANDLING,
                PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_DJANGO_MODULES_HANDLING);

        //code formatting
        node.putBoolean(PyCodeFormatterPage.USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS,
                PyCodeFormatterPage.DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS);
        node.putBoolean(PyCodeFormatterPage.USE_OPERATORS_WITH_SPACE,
                PyCodeFormatterPage.DEFAULT_USE_OPERATORS_WITH_SPACE);
        node.putBoolean(PyCodeFormatterPage.USE_SPACE_AFTER_COMMA, PyCodeFormatterPage.DEFAULT_USE_SPACE_AFTER_COMMA);
        node.putBoolean(PyCodeFormatterPage.ADD_NEW_LINE_AT_END_OF_FILE,
                PyCodeFormatterPage.DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE);
        node.putBoolean(PydevSaveActionsPrefPage.FORMAT_BEFORE_SAVING,
                PydevSaveActionsPrefPage.DEFAULT_FORMAT_BEFORE_SAVING);
        node.putBoolean(PydevSaveActionsPrefPage.SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES,
                PydevSaveActionsPrefPage.DEFAULT_SAVE_ACTIONS_ONLY_ON_WORKSPACE_FILES);
        node.putBoolean(PyCodeFormatterPage.FORMAT_WITH_AUTOPEP8, PyCodeFormatterPage.DEFAULT_FORMAT_WITH_AUTOPEP8);
        node.putBoolean(PyCodeFormatterPage.FORMAT_ONLY_CHANGED_LINES,
                PyCodeFormatterPage.DEFAULT_FORMAT_ONLY_CHANGED_LINES);
        node.putBoolean(PyCodeFormatterPage.TRIM_LINES, PyCodeFormatterPage.DEFAULT_TRIM_LINES);
        node.putBoolean(PyCodeFormatterPage.USE_SPACE_FOR_PARENTESIS,
                PyCodeFormatterPage.DEFAULT_USE_SPACE_FOR_PARENTESIS);
        node.putInt(PyCodeFormatterPage.SPACES_BEFORE_COMMENT,
                PyCodeFormatterPage.DEFAULT_SPACES_BEFORE_COMMENT);
        node.putInt(PyCodeFormatterPage.SPACES_IN_START_COMMENT,
                PyCodeFormatterPage.DEFAULT_SPACES_IN_START_COMMENT);

        //initialize pyunit prefs
        node.putInt(PyUnitPrefsPage2.TEST_RUNNER, PyUnitPrefsPage2.DEFAULT_TEST_RUNNER);
        node.putBoolean(PyUnitPrefsPage2.USE_PYUNIT_VIEW, PyUnitPrefsPage2.DEFAULT_USE_PYUNIT_VIEW);
        node.put(PyUnitPrefsPage2.TEST_RUNNER_DEFAULT_PARAMETERS,
                PyUnitPrefsPage2.DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS);

        // Docstrings
        node.put(DocstringsPrefPage.P_DOCSTRINGCHARACTER, DocstringsPrefPage.DEFAULT_P_DOCSTRINGCHARACTER);
        node.put(DocstringsPrefPage.P_DOCSTRINGSTYLE, DocstringsPrefPage.DEFAULT_P_DOCSTIRNGSTYLE);
        node.put(DocstringsPrefPage.P_TYPETAGGENERATION, DocstringsPrefPage.DEFAULT_P_TYPETAGGENERATION);
        node.put(DocstringsPrefPage.P_DONT_GENERATE_TYPETAGS, DocstringsPrefPage.DEFAULT_P_DONT_GENERATE_TYPETAGS);

        //file types
        node.put(FileTypesPreferencesPage.VALID_SOURCE_FILES, FileTypesPreferencesPage.DEFAULT_VALID_SOURCE_FILES);
        node.put(FileTypesPreferencesPage.FIRST_CHOICE_PYTHON_SOURCE_FILE,
                FileTypesPreferencesPage.DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE);

        //imports
        node.putBoolean(ImportsPreferencesPage.GROUP_IMPORTS, ImportsPreferencesPage.DEFAULT_GROUP_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.MULTILINE_IMPORTS, ImportsPreferencesPage.DEFAULT_MULTILINE_IMPORTS);
        node.put(ImportsPreferencesPage.BREAK_IMPORTS_MODE, ImportsPreferencesPage.DEFAULT_BREAK_IMPORTS_MODE);
        node.putBoolean(ImportsPreferencesPage.PEP8_IMPORTS, ImportsPreferencesPage.DEFAULT_PEP8_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.DELETE_UNUSED_IMPORTS,
                ImportsPreferencesPage.DEFAULT_DELETE_UNUSED_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.FROM_IMPORTS_FIRST, ImportsPreferencesPage.DEFAULT_FROM_IMPORTS_FIRST);
        node.putBoolean(ImportsPreferencesPage.SORT_NAMES_GROUPED, ImportsPreferencesPage.DEFAULT_SORT_NAMES_GROUPED);

        //hover
        node.putBoolean(PyHoverPreferencesPage.SHOW_DOCSTRING_ON_HOVER,
                PyHoverPreferencesPage.DEFAULT_SHOW_DOCSTRING_ON_HOVER);
        node.putBoolean(PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER,
                PyHoverPreferencesPage.DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);

        //source locator
        node.putInt(PySourceLocatorPrefs.ON_SOURCE_NOT_FOUND,
                PySourceLocatorPrefs.DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER);
        node.putInt(PySourceLocatorPrefs.FILE_CONTENTS_TIMEOUT, PySourceLocatorPrefs.DEFAULT_FILE_CONTENTS_TIMEOUT);

        //general interpreters
        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_PY,
                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_PY);
        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_JY,
                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_JY);
        node.putBoolean(InterpreterGeneralPreferencesPage.NOTIFY_NO_INTERPRETER_IP,
                InterpreterGeneralPreferencesPage.DEFAULT_NOTIFY_NO_INTERPRETER_IP);

        node.putBoolean(InterpreterGeneralPreferencesPage.CHECK_CONSISTENT_ON_STARTUP,
                InterpreterGeneralPreferencesPage.DEFAULT_CHECK_CONSISTENT_ON_STARTUP);

        node.putBoolean(InterpreterGeneralPreferencesPage.UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES,
                InterpreterGeneralPreferencesPage.DEFAULT_UPDATE_INTERPRETER_INFO_ON_FILESYSTEM_CHANGES);

        //save actions
        node.putBoolean(PydevSaveActionsPrefPage.SORT_IMPORTS_ON_SAVE,
                PydevSaveActionsPrefPage.DEFAULT_SORT_IMPORTS_ON_SAVE);

        node.putBoolean(PydevSaveActionsPrefPage.ENABLE_DATE_FIELD_ACTION,
                PydevSaveActionsPrefPage.DEFAULT_ENABLE_DATE_FIELD_ACTION);

        node.put(PydevSaveActionsPrefPage.DATE_FIELD_NAME, PydevSaveActionsPrefPage.DEFAULT_DATE_FIELD_NAME);
        node.put(PydevSaveActionsPrefPage.DATE_FIELD_FORMAT, PydevSaveActionsPrefPage.DEFAULT_DATE_FIELD_FORMAT);

        //root
        node.putBoolean(PydevRootPrefs.CHECK_PREFERRED_PYDEV_SETTINGS,
                PydevRootPrefs.DEFAULT_CHECK_PREFERRED_PYDEV_SETTINGS);

    }

}
