/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.editorinput.PySourceLocatorPrefs;
import org.python.pydev.parser.PyParserManager;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.pyunit.preferences.PyUnitPrefsPage2;
import org.python.pydev.ui.filetypes.FileTypesPreferencesPage;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;
import org.python.pydev.ui.wizards.project.IWizardNewProjectNameAndLocationPage;

public class PydevPrefsInitializer  extends AbstractPreferenceInitializer{


    @Override
    public void initializeDefaultPreferences() {
        Preferences node = new DefaultScope().getNode(PydevPlugin.DEFAULT_PYDEV_SCOPE);
        
        //ironpython
        node.put(IInterpreterManager.IRONPYTHON_INTERNAL_SHELL_VM_ARGS, IInterpreterManager.IRONPYTHON_DEFAULT_INTERNAL_SHELL_VM_ARGS);

        //text
        node.putBoolean(PydevEditorPrefs.SMART_INDENT_PAR, PydevEditorPrefs.DEFAULT_SMART_INDENT_PAR);
        node.putBoolean(PydevEditorPrefs.AUTO_PAR, PydevEditorPrefs.DEFAULT_AUTO_PAR);
        node.putBoolean(PydevEditorPrefs.AUTO_LINK, PydevEditorPrefs.DEFAULT_AUTO_LINK);
        node.putBoolean(PydevEditorPrefs.AUTO_INDENT_TO_PAR_LEVEL, PydevEditorPrefs.DEFAULT_AUTO_INDENT_TO_PAR_LEVEL);
        node.putBoolean(PydevEditorPrefs.AUTO_DEDENT_ELSE, PydevEditorPrefs.DEFAULT_AUTO_DEDENT_ELSE);
        node.putInt(PydevEditorPrefs.AUTO_INDENT_AFTER_PAR_WIDTH, PydevEditorPrefs.DEFAULT_AUTO_INDENT_AFTER_PAR_WIDTH);
        node.putBoolean(PydevEditorPrefs.AUTO_COLON, PydevEditorPrefs.DEFAULT_AUTO_COLON);
        node.putBoolean(PydevEditorPrefs.AUTO_BRACES, PydevEditorPrefs.DEFAULT_AUTO_BRACES);
        node.putBoolean(PydevEditorPrefs.AUTO_WRITE_IMPORT_STR, PydevEditorPrefs.DEFAULT_AUTO_WRITE_IMPORT_STR);
        node.putBoolean(PydevEditorPrefs.AUTO_LITERALS, PydevEditorPrefs.DEFAULT_AUTO_LITERALS);
        node.putBoolean(PydevEditorPrefs.SMART_LINE_MOVE, PydevEditorPrefs.DEFAULT_SMART_LINE_MOVE);
    
        node.putInt(PydevEditorPrefs.TAB_WIDTH, PydevEditorPrefs.DEFAULT_TAB_WIDTH);
        node.putInt(IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PREFERENCES, 
                IWizardNewProjectNameAndLocationPage.PYDEV_NEW_PROJECT_CREATE_PROJECT_AS_SRC_FOLDER);
        
        //comment blocks
        node.put(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_CHAR, CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_CHAR);
        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME, CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_CLASS_NAME);
        node.putBoolean(CommentBlocksPreferences.MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME, CommentBlocksPreferences.DEFAULT_MULTI_BLOCK_COMMENT_SHOW_ONLY_FUNCTION_NAME);
        node.put(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_CHAR, CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_CHAR);
        node.putBoolean(CommentBlocksPreferences.SINGLE_BLOCK_COMMENT_ALIGN_RIGHT, CommentBlocksPreferences.DEFAULT_SINGLE_BLOCK_COMMENT_ALIGN_RIGHT);
        
        //checkboxes
        node.putBoolean(PydevEditorPrefs.SUBSTITUTE_TABS, PydevEditorPrefs.DEFAULT_SUBSTITUTE_TABS);
        node.putBoolean(PydevEditorPrefs.AUTO_ADD_SELF, PydevEditorPrefs.DEFAULT_AUTO_ADD_SELF);
        node.putBoolean(PydevEditorPrefs.GUESS_TAB_SUBSTITUTION, PydevEditorPrefs.DEFAULT_GUESS_TAB_SUBSTITUTION);
        
        //matching
        node.putBoolean(PydevEditorPrefs.USE_MATCHING_BRACKETS, PydevEditorPrefs.DEFAULT_USE_MATCHING_BRACKETS);
        node.put(PydevEditorPrefs.MATCHING_BRACKETS_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_MATCHING_BRACKETS_COLOR));
        node.putInt(PydevEditorPrefs.MATCHING_BRACKETS_STYLE, PydevEditorPrefs.DEFAULT_MATCHING_BRACKETS_STYLE);
        
        //colors
        node.put(PydevEditorPrefs.CODE_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_CODE_COLOR));
        node.put(PydevEditorPrefs.NUMBER_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_NUMBER_COLOR));
        node.put(PydevEditorPrefs.DECORATOR_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_DECORATOR_COLOR));
        node.put(PydevEditorPrefs.KEYWORD_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_KEYWORD_COLOR));
        node.put(PydevEditorPrefs.SELF_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_SELF_COLOR));
        node.put(PydevEditorPrefs.STRING_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_STRING_COLOR));
        node.put(PydevEditorPrefs.COMMENT_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_COMMENT_COLOR));
        node.put(PydevEditorPrefs.BACKQUOTES_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_BACKQUOTES_COLOR));
        node.put(PydevEditorPrefs.CLASS_NAME_COLOR, StringConverter.asString(PydevEditorPrefs.DEFAULT_CLASS_NAME_COLOR));
        node.put(PydevEditorPrefs.FUNC_NAME_COLOR,  StringConverter.asString(PydevEditorPrefs.DEFAULT_FUNC_NAME_COLOR));
        node.put(PydevEditorPrefs.PARENS_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_PARENS_COLOR));
        node.put(PydevEditorPrefs.OPERATORS_COLOR,StringConverter.asString(PydevEditorPrefs.DEFAULT_OPERATORS_COLOR));
        //for selection colors see initializeDefaultColors()
        
        //font style
        node.putInt(PydevEditorPrefs.CODE_STYLE, PydevEditorPrefs.DEFAULT_CODE_STYLE);
        node.putInt(PydevEditorPrefs.NUMBER_STYLE, PydevEditorPrefs.DEFAULT_NUMBER_STYLE);
        node.putInt(PydevEditorPrefs.DECORATOR_STYLE, PydevEditorPrefs.DEFAULT_DECORATOR_STYLE);
        node.putInt(PydevEditorPrefs.KEYWORD_STYLE, PydevEditorPrefs.DEFAULT_KEYWORD_STYLE);
        node.putInt(PydevEditorPrefs.SELF_STYLE, PydevEditorPrefs.DEFAULT_SELF_STYLE);
        node.putInt(PydevEditorPrefs.STRING_STYLE, PydevEditorPrefs.DEFAULT_STRING_STYLE);
        node.putInt(PydevEditorPrefs.COMMENT_STYLE, PydevEditorPrefs.DEFAULT_COMMENT_STYLE);
        node.putInt(PydevEditorPrefs.BACKQUOTES_STYLE, PydevEditorPrefs.DEFAULT_BACKQUOTES_STYLE);
        node.putInt(PydevEditorPrefs.CLASS_NAME_STYLE, PydevEditorPrefs.DEFAULT_CLASS_NAME_STYLE);
        node.putInt(PydevEditorPrefs.FUNC_NAME_STYLE, PydevEditorPrefs.DEFAULT_FUNC_NAME_STYLE);
        node.putInt(PydevEditorPrefs.PARENS_STYLE, PydevEditorPrefs.DEFAULT_PARENS_STYLE);
        node.putInt(PydevEditorPrefs.OPERATORS_STYLE, PydevEditorPrefs.DEFAULT_OPERATORS_STYLE);
        
        //no UI
        node.putInt(PydevEditorPrefs.CONNECT_TIMEOUT, PydevEditorPrefs.DEFAULT_CONNECT_TIMEOUT);
        
        
        //pydev todo tasks
        node.put(PyTodoPrefPage.PY_TODO_TAGS, PyTodoPrefPage.DEFAULT_PY_TODO_TAGS);
        
        //builders
        node.putBoolean(PyDevBuilderPrefPage.USE_PYDEV_BUILDERS, PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_BUILDERS);
        node.putBoolean(PyParserManager.USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE, PyDevBuilderPrefPage.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);
        node.putInt(PyParserManager.PYDEV_ELAPSE_BEFORE_ANALYSIS, PyDevBuilderPrefPage.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);
        node.putBoolean(PyDevBuilderPrefPage.ANALYZE_ONLY_ACTIVE_EDITOR, PyDevBuilderPrefPage.DEFAULT_ANALYZE_ONLY_ACTIVE_EDITOR);
        node.putBoolean(PyDevBuilderPrefPage.REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED, PyDevBuilderPrefPage.DEFAULT_REMOVE_ERRORS_WHEN_EDITOR_IS_CLOSED);
        node.putInt(PyDevBuilderPrefPage.PYC_DELETE_HANDLING, PyDevBuilderPrefPage.DEFAULT_PYC_DELETE_HANDLING);
        
        //code folding 
        node.putBoolean(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING, PyDevCodeFoldingPrefPage.DEFAULT_USE_CODE_FOLDING);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IF);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WHILE, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WHILE);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FOR, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FOR);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_CLASSDEF);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_FUNCTIONDEF);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_COMMENTS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_COMMENTS);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_STRINGS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_STRINGS);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_WITH, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_WITH);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_TRY, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_TRY);
        node.putBoolean(PyDevCodeFoldingPrefPage.FOLD_IMPORTS, PyDevCodeFoldingPrefPage.DEFAULT_FOLD_IMPORTS);
        

        //coding style
        node.putBoolean(PyCodeStylePreferencesPage.USE_LOCALS_AND_ATTRS_CAMELCASE, PyCodeStylePreferencesPage.DEFAULT_USE_LOCALS_AND_ATTRS_CAMELCASE);
        node.putInt(PyCodeStylePreferencesPage.USE_METHODS_FORMAT, PyCodeStylePreferencesPage.DEFAULT_USE_METHODS_FORMAT);
        
        //Editor title
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_NAMES_UNIQUE, PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_NAMES_UNIQUE);
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_SHOW_EXTENSION, PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_SHOW_EXTENSION);
        node.putBoolean(PyTitlePreferencesPage.TITLE_EDITOR_CUSTOM_INIT_ICON, PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_CUSTOM_INIT_ICON);
        node.put(PyTitlePreferencesPage.TITLE_EDITOR_INIT_HANDLING, PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_INIT_HANDLING);
        node.put(PyTitlePreferencesPage.TITLE_EDITOR_DJANGO_MODULES_HANDLING, PyTitlePreferencesPage.DEFAULT_TITLE_EDITOR_DJANGO_MODULES_HANDLING);
        
        //code formatting
        node.putBoolean(PyCodeFormatterPage.USE_ASSIGN_WITH_PACES_INSIDER_PARENTESIS, PyCodeFormatterPage.DEFAULT_USE_ASSIGN_WITH_PACES_INSIDE_PARENTESIS);
        node.putBoolean(PyCodeFormatterPage.USE_OPERATORS_WITH_SPACE, PyCodeFormatterPage.DEFAULT_USE_OPERATORS_WITH_SPACE);
        node.putBoolean(PyCodeFormatterPage.USE_SPACE_AFTER_COMMA,    PyCodeFormatterPage.DEFAULT_USE_SPACE_AFTER_COMMA);
        node.putBoolean(PyCodeFormatterPage.ADD_NEW_LINE_AT_END_OF_FILE,    PyCodeFormatterPage.DEFAULT_ADD_NEW_LINE_AT_END_OF_FILE);
        node.putBoolean(PyCodeFormatterPage.FORMAT_BEFORE_SAVING,    PyCodeFormatterPage.DEFAULT_FORMAT_BEFORE_SAVING);
        node.putBoolean(PyCodeFormatterPage.FORMAT_ONLY_CHANGED_LINES,    PyCodeFormatterPage.DEFAULT_FORMAT_ONLY_CHANGED_LINES);
        node.putBoolean(PyCodeFormatterPage.TRIM_LINES,    PyCodeFormatterPage.DEFAULT_TRIM_LINES);
        node.putBoolean(PyCodeFormatterPage.USE_SPACE_FOR_PARENTESIS, PyCodeFormatterPage.DEFAULT_USE_SPACE_FOR_PARENTESIS);

        //initialize pyunit prefs
        node.putInt(PyUnitPrefsPage2.TEST_RUNNER, PyUnitPrefsPage2.DEFAULT_TEST_RUNNER);
        node.putBoolean(PyUnitPrefsPage2.USE_PYUNIT_VIEW, PyUnitPrefsPage2.DEFAULT_USE_PYUNIT_VIEW);
        node.put(PyUnitPrefsPage2.TEST_RUNNER_DEFAULT_PARAMETERS, PyUnitPrefsPage2.DEFAULT_TEST_RUNNER_DEFAULT_PARAMETERS);
        
        // Docstrings
        node.put(DocstringsPrefPage.P_DOCSTRINGCHARACTER, DocstringsPrefPage.DEFAULT_P_DOCSTRINGCHARACTER);
        node.put(DocstringsPrefPage.P_DOCSTRINGSTYLE, DocstringsPrefPage.DEFAULT_P_DOCSTIRNGSTYLE);
        node.put(DocstringsPrefPage.P_TYPETAGGENERATION, DocstringsPrefPage.DEFAULT_P_TYPETAGGENERATION);
        node.put(DocstringsPrefPage.P_DONT_GENERATE_TYPETAGS, DocstringsPrefPage.DEFAULT_P_DONT_GENERATE_TYPETAGS);
        
        //file types
        node.put(FileTypesPreferencesPage.VALID_SOURCE_FILES, FileTypesPreferencesPage.DEFAULT_VALID_SOURCE_FILES);
        node.put(FileTypesPreferencesPage.FIRST_CHOICE_PYTHON_SOURCE_FILE, FileTypesPreferencesPage.DEFAULT_FIRST_CHOICE_PYTHON_SOURCE_FILE);
        
        //imports
        node.putBoolean(ImportsPreferencesPage.GROUP_IMPORTS, ImportsPreferencesPage.DEFAULT_GROUP_IMPORTS);
        node.putBoolean(ImportsPreferencesPage.MULTILINE_IMPORTS, ImportsPreferencesPage.DEFAULT_MULTILINE_IMPORTS);
        node.put(ImportsPreferencesPage.BREAK_IMPORTS_MODE, ImportsPreferencesPage.DEFAULT_BREAK_IMPORTS_MODE);
        
        //hover
        node.putBoolean(PyHoverPreferencesPage.SHOW_DOCSTRING_ON_HOVER, PyHoverPreferencesPage.DEFAULT_SHOW_DOCSTRING_ON_HOVER);
        node.putBoolean(PyHoverPreferencesPage.SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER, PyHoverPreferencesPage.DEFAULT_SHOW_DEBUG_VARIABLES_VALUES_ON_HOVER);
        
        //source locator
        node.putInt(PySourceLocatorPrefs.ON_SOURCE_NOT_FOUND, PySourceLocatorPrefs.DEFAULT_ON_FILE_NOT_FOUND_IN_DEBUGGER);
        node.putInt(PySourceLocatorPrefs.FILE_CONTENTS_TIMEOUT, PySourceLocatorPrefs.DEFAULT_FILE_CONTENTS_TIMEOUT);
    }
    

}
