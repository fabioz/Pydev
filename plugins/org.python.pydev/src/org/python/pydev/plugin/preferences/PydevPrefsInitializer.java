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
import org.python.pydev.builder.todo.PyTodoPrefPage;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.preferences.FileTypesPreferences;
import org.python.pydev.core.preferences.InterpreterGeneralPreferences;
import org.python.pydev.core.preferences.PyDevCoreEditorPreferences;
import org.python.pydev.core.preferences.PyDevTypingPreferences;
import org.python.pydev.editor.codefolding.PyDevCodeFoldingPrefPage;
import org.python.pydev.editor.commentblocks.CommentBlocksPreferences;
import org.python.pydev.editor.correctionassist.docstrings.DocstringsPrefPage;
import org.python.pydev.editor.hover.PyHoverPreferencesPage;
import org.python.pydev.editor.saveactions.PydevSaveActionsPrefPage;
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.parser.PyParserManager;
import org.python.pydev.parser.preferences.PyDevBuilderPreferences;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_ui.word_boundaries.SubWordPreferences;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;

public class PydevPrefsInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        Preferences node = DefaultScope.INSTANCE.getNode(SharedCorePlugin.DEFAULT_PYDEV_PREFERENCES_SCOPE);

        //ironpython
        node.put(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS,
                IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS);

        //text
        node.putBoolean(PyDevTypingPreferences.SMART_INDENT_PAR, PyDevTypingPreferences.DEFAULT_SMART_INDENT_PAR);
        node.putBoolean(PyDevTypingPreferences.AUTO_PAR, PyDevTypingPreferences.DEFAULT_AUTO_PAR);
        node.putBoolean(PyDevTypingPreferences.INDENT_AFTER_PAR_AS_PEP8, PyDevTypingPreferences.DEFAULT_INDENT_AFTER_PAR_AS_PEP8);
        node.putBoolean(PyDevTypingPreferences.AUTO_LINK, PyDevTypingPreferences.DEFAULT_AUTO_LINK);
        node.putBoolean(PyDevTypingPreferences.AUTO_INDENT_TO_PAR_LEVEL, PyDevTypingPreferences.DEFAULT_AUTO_INDENT_TO_PAR_LEVEL);
        node.putBoolean(PyDevTypingPreferences.AUTO_DEDENT_ELSE, PyDevTypingPreferences.DEFAULT_AUTO_DEDENT_ELSE);
        node.putInt(PyDevTypingPreferences.AUTO_INDENT_AFTER_PAR_WIDTH, PyDevTypingPreferences.DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH);
        node.putBoolean(PyDevTypingPreferences.AUTO_COLON, PyDevTypingPreferences.DEFAULT_AUTO_COLON);
        node.putBoolean(PyDevTypingPreferences.AUTO_BRACES, PyDevTypingPreferences.DEFAULT_AUTO_BRACES);
        node.putBoolean(PyDevTypingPreferences.AUTO_WRITE_IMPORT_STR, PyDevTypingPreferences.DEFAULT_AUTO_WRITE_IMPORT_STR);
        node.putBoolean(PyDevTypingPreferences.AUTO_LITERALS, PyDevTypingPreferences.DEFAULT_AUTO_LITERALS);
        node.putBoolean(PyDevTypingPreferences.SMART_LINE_MOVE, PyDevTypingPreferences.DEFAULT_SMART_LINE_MOVE);

        node.putInt(PyDevCoreEditorPreferences.TAB_WIDTH, PyDevCoreEditorPreferences.DEFAULT_TAB_WIDTH);
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
        node.putBoolean(PyDevCoreEditorPreferences.SUBSTITUTE_TABS, PyDevCoreEditorPreferences.DEFAULT_SUBSTITUTE_TABS);
        node.putBoolean(PyDevTypingPreferences.AUTO_ADD_SELF, PyDevTypingPreferences.DEFAULT_AUTO_ADD_SELF);
        node.putBoolean(PyDevCoreEditorPreferences.GUESS_TAB_SUBSTITUTION, PyDevCoreEditorPreferences.DEFAULT_GUESS_TAB_SUBSTITUTION);
        node.putBoolean(PyDevEditorPreferences.USE_VERTICAL_INDENT_GUIDE, PyDevEditorPreferences.DEFAULT_USE_VERTICAL_INDENT_GUIDE);
        node.putBoolean(PyDevEditorPreferences.USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND,
                PyDevEditorPreferences.DEFAULT_USE_VERTICAL_INDENT_COLOR_EDITOR_FOREGROUND);
        node.putInt(PyDevEditorPreferences.VERTICAL_INDENT_TRANSPARENCY,
                PyDevEditorPreferences.DEFAULT_VERTICAL_INDENT_TRANSPARENCY);

        node.put(SubWordPreferences.WORD_NAVIGATION_STYLE, SubWordPreferences.DEFAULT_WORD_NAVIGATION_STYLE);

        //matching
        node.putBoolean(PyDevEditorPreferences.USE_MATCHING_BRACKETS, PyDevEditorPreferences.DEFAULT_USE_MATCHING_BRACKETS);
        node.put(PyDevEditorPreferences.MATCHING_BRACKETS_COLOR,
                StringConverter.asString(PyDevEditorPreferences.DEFAULT_MATCHING_BRACKETS_COLOR));
        node.putInt(PyDevEditorPreferences.MATCHING_BRACKETS_STYLE, PyDevEditorPreferences.DEFAULT_MATCHING_BRACKETS_STYLE);

        //colors
        node.put(PyDevEditorPreferences.VERTICAL_INDENT_COLOR,
                StringConverter.asString(PyDevEditorPreferences.DEFAULT_VERTICAL_INDENT_COLOR));
        node.put(PyDevEditorPreferences.CODE_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_CODE_COLOR));
        node.put(PyDevEditorPreferences.NUMBER_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_NUMBER_COLOR));
        node.put(PyDevEditorPreferences.DECORATOR_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_DECORATOR_COLOR));
        node.put(PyDevEditorPreferences.KEYWORD_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_KEYWORD_COLOR));
        node.put(PyDevEditorPreferences.SELF_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_SELF_COLOR));
        node.put(PyDevEditorPreferences.STRING_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_STRING_COLOR));
        node.put(PyDevEditorPreferences.UNICODE_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_UNICODE_COLOR));
        node.put(PyDevEditorPreferences.COMMENT_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_COMMENT_COLOR));
        node.put(PyDevEditorPreferences.BACKQUOTES_COLOR,
                StringConverter.asString(PyDevEditorPreferences.DEFAULT_BACKQUOTES_COLOR));
        node.put(PyDevEditorPreferences.CLASS_NAME_COLOR,
                StringConverter.asString(PyDevEditorPreferences.DEFAULT_CLASS_NAME_COLOR));
        node.put(PyDevEditorPreferences.FUNC_NAME_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_FUNC_NAME_COLOR));
        node.put(PyDevEditorPreferences.PARENS_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_PARENS_COLOR));
        node.put(PyDevEditorPreferences.OPERATORS_COLOR, StringConverter.asString(PyDevEditorPreferences.DEFAULT_OPERATORS_COLOR));
        node.put(PyDevEditorPreferences.DOCSTRING_MARKUP_COLOR,
                StringConverter.asString(PyDevEditorPreferences.DEFAULT_DOCSTRING_MARKUP_COLOR));
        //for selection colors see initializeDefaultColors()

        //font style
        node.putInt(PyDevEditorPreferences.CODE_STYLE, PyDevEditorPreferences.DEFAULT_CODE_STYLE);
        node.putInt(PyDevEditorPreferences.NUMBER_STYLE, PyDevEditorPreferences.DEFAULT_NUMBER_STYLE);
        node.putInt(PyDevEditorPreferences.DECORATOR_STYLE, PyDevEditorPreferences.DEFAULT_DECORATOR_STYLE);
        node.putInt(PyDevEditorPreferences.KEYWORD_STYLE, PyDevEditorPreferences.DEFAULT_KEYWORD_STYLE);
        node.putInt(PyDevEditorPreferences.SELF_STYLE, PyDevEditorPreferences.DEFAULT_SELF_STYLE);
        node.putInt(PyDevEditorPreferences.STRING_STYLE, PyDevEditorPreferences.DEFAULT_STRING_STYLE);
        node.putInt(PyDevEditorPreferences.UNICODE_STYLE, PyDevEditorPreferences.DEFAULT_UNICODE_STYLE);
        node.putInt(PyDevEditorPreferences.COMMENT_STYLE, PyDevEditorPreferences.DEFAULT_COMMENT_STYLE);
        node.putInt(PyDevEditorPreferences.BACKQUOTES_STYLE, PyDevEditorPreferences.DEFAULT_BACKQUOTES_STYLE);
        node.putInt(PyDevEditorPreferences.CLASS_NAME_STYLE, PyDevEditorPreferences.DEFAULT_CLASS_NAME_STYLE);
        node.putInt(PyDevEditorPreferences.FUNC_NAME_STYLE, PyDevEditorPreferences.DEFAULT_FUNC_NAME_STYLE);
        node.putInt(PyDevEditorPreferences.PARENS_STYLE, PyDevEditorPreferences.DEFAULT_PARENS_STYLE);
        node.putInt(PyDevEditorPreferences.OPERATORS_STYLE, PyDevEditorPreferences.DEFAULT_OPERATORS_STYLE);
        node.putInt(PyDevEditorPreferences.DOCSTRING_MARKUP_STYLE, PyDevEditorPreferences.DEFAULT_DOCSTRING_MARKUP_STYLE);

        //Debugger
        node.putInt(PyDevEditorPreferences.CONNECT_TIMEOUT, PyDevEditorPreferences.DEFAULT_CONNECT_TIMEOUT);
        node.putBoolean(PyDevEditorPreferences.RELOAD_MODULE_ON_CHANGE, PyDevEditorPreferences.DEFAULT_RELOAD_MODULE_ON_CHANGE);
        node.putBoolean(PyDevEditorPreferences.DONT_TRACE_ENABLED, PyDevEditorPreferences.DEFAULT_DONT_TRACE_ENABLED);
        node.putBoolean(PyDevEditorPreferences.SHOW_RETURN_VALUES, PyDevEditorPreferences.DEFAULT_SHOW_RETURN_VALUES);
        node.putBoolean(PyDevEditorPreferences.DEBUG_MULTIPROCESSING_ENABLED,
                PyDevEditorPreferences.DEFAULT_DEBUG_MULTIPROCESSING_ENABLED);
        node.putBoolean(PyDevEditorPreferences.KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS,
                PyDevEditorPreferences.DEFAULT_KILL_SUBPROCESSES_WHEN_TERMINATING_PROCESS);
        node.putBoolean(PyDevEditorPreferences.GEVENT_DEBUGGING, PyDevEditorPreferences.DEFAULT_GEVENT_DEBUGGING);
        node.putBoolean(PyDevEditorPreferences.TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS,
                PyDevEditorPreferences.DEFAULT_TRACE_DJANGO_TEMPLATE_RENDER_EXCEPTIONS);
        node.put(PyDevEditorPreferences.QT_THREADS_DEBUG_MODE, PyDevEditorPreferences.DEFAULT_QT_THREADS_DEBUG_MODE);

        //pydev todo tasks
        node.put(PyTodoPrefPage.PY_TODO_TAGS, PyTodoPrefPage.DEFAULT_PY_TODO_TAGS);

        //builders
        node.putBoolean(PyDevBuilderPreferences.USE_PYDEV_BUILDERS, PyDevBuilderPreferences.DEFAULT_USE_PYDEV_BUILDERS);
        node.putBoolean(PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
                PyDevBuilderPreferences.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);
        node.putInt(PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS,
                PyDevBuilderPreferences.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);
        node.putBoolean(PyDevBuilderPreferences.ANALYZE_ONLY_ACTIVE_EDITOR,
                PyDevBuilderPreferences.DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
        node.putBoolean(PyDevBuilderPreferences.REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED,
                PyDevBuilderPreferences.DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
        node.putInt(PyDevBuilderPreferences.PYC_DELETE_HANDLING, PyDevBuilderPreferences.DEFAULT_PYC_DELETE_HANDLING);

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
        node.putBoolean(PyCodeFormatterPage.MANAGE_BLANK_LINES,
                PyCodeFormatterPage.DEFAULT_MANAGE_BLANK_LINES);
        node.putInt(PyCodeFormatterPage.BLANK_LINES_TOP_LEVEL,
                PyCodeFormatterPage.DEFAULT_BLANK_LINES_TOP_LEVEL);
        node.putInt(PyCodeFormatterPage.BLANK_LINES_INNER,
                PyCodeFormatterPage.DEFAULT_BLANK_LINES_INNER);

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
        node.put(FileTypesPreferences.VALID_SOURCE_FILES, FileTypesPreferences.DEFAULT_VALID_SOURCE_FILES);
        node.put(FileTypesPreferences.FIRST_CHOICE_PYTHON_SOURCE_FILE,
                FileTypesPreferences.DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE);

        //imports
        node.putBoolean(ImportsPreferencesPage.GROUP_IMPORTS, ImportsPreferencesPage.DEFAULT_GROUP_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.MULTILINE_IMPORTS, ImportsPreferencesPage.DEFAULT_MULTILINE_IMPORTS);
        node.put(ImportsPreferencesPage.BREAK_IMPORTS_MODE, ImportsPreferencesPage.DEFAULT_BREAK_IMPORTS_MODE);
        node.put(ImportsPreferencesPage.IMPORT_ENGINE, ImportsPreferencesPage.DEFAULT_IMPORT_ENGINE);
        node.putBoolean(ImportsPreferencesPage.DELETE_UNUSED_IMPORTS,
                ImportsPreferencesPage.DEFAULT_DELETE_UNUSED_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.FROM_IMPORTS_FIRST, ImportsPreferencesPage.DEFAULT_FROM_IMPORTS_FIRST);
        node.putBoolean(ImportsPreferencesPage.SORT_NAMES_GROUPED, ImportsPreferencesPage.DEFAULT_SORT_NAMES_GROUPED);

        //hover
        node.putBoolean(PyHoverPreferencesPage.COMBINE_HOVER_INFO,
                PyHoverPreferencesPage.DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
        node.putBoolean(PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER,
                PyHoverPreferencesPage.DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
        node.putBoolean(PyHoverPreferencesPage.USE_HOVER_DIVIDER,
                PyHoverPreferencesPage.DEFAULT_USE_HOVER_DIVIDER);

        //source locator
        node.putInt(PySourceLocatorPrefs.ON_SOURCE_NOT_FOUND,
                PySourceLocatorPrefs.DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER);
        node.putInt(PySourceLocatorPrefs.FILE_CONTENTS_TIMEOUT, PySourceLocatorPrefs.DEFAULT_FILE_CONTENTS_TIMEOUT);

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
