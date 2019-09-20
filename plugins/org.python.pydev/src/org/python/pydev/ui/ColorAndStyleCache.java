/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.shared_ui.ColorCache;

public class ColorAndStyleCache extends ColorCache {

    public ColorAndStyleCache(IPreferenceStore prefs) {
        super(prefs);
    }

    public static boolean isColorOrStyleProperty(String property) {
        if (property.endsWith("_COLOR") || property.endsWith("_STYLE")) {
            return true;
        }
        return false;
    }

    //Note that to update the code below, the install.py of this plugin should be run.

    /*[[[cog
    import cog
    
    template = '''
    public TextAttribute get%sTextAttribute() {
    
        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.%s_COLOR), null, preferences.getInt(PyDevEditorPreferences.%s_STYLE));
    }'''
    
    for s in ('self', 'code', 'decorator', 'number', 'class_name', 'func_name', 'comment', 'backquotes', 'string', 'unicode', 'keyword', 'parens', 'operators', 'docstring_markup'):
    
        cog.outl(template % (s.title().replace('_', ''), s.upper(), s.upper()))
    
    ]]]*/

    public TextAttribute getSelfTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.SELF_COLOR), null, preferences.getInt(PyDevEditorPreferences.SELF_STYLE));
    }

    public TextAttribute getCodeTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.CODE_COLOR), null, preferences.getInt(PyDevEditorPreferences.CODE_STYLE));
    }

    public TextAttribute getDecoratorTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.DECORATOR_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.DECORATOR_STYLE));
    }

    public TextAttribute getNumberTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.NUMBER_COLOR), null, preferences.getInt(PyDevEditorPreferences.NUMBER_STYLE));
    }

    public TextAttribute getClassNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.CLASS_NAME_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.CLASS_NAME_STYLE));
    }

    public TextAttribute getFuncNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.FUNC_NAME_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.FUNC_NAME_STYLE));
    }

    public TextAttribute getCommentTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.COMMENT_COLOR), null, preferences.getInt(PyDevEditorPreferences.COMMENT_STYLE));
    }

    public TextAttribute getBackquotesTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.BACKQUOTES_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.BACKQUOTES_STYLE));
    }

    public TextAttribute getStringTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.STRING_COLOR), null, preferences.getInt(PyDevEditorPreferences.STRING_STYLE));
    }

    public TextAttribute getUnicodeTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.UNICODE_COLOR), null, preferences.getInt(PyDevEditorPreferences.UNICODE_STYLE));
    }

    public TextAttribute getKeywordTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.KEYWORD_COLOR), null, preferences.getInt(PyDevEditorPreferences.KEYWORD_STYLE));
    }

    public TextAttribute getParensTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.PARENS_COLOR), null, preferences.getInt(PyDevEditorPreferences.PARENS_STYLE));
    }

    public TextAttribute getOperatorsTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.OPERATORS_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.OPERATORS_STYLE));
    }

    public TextAttribute getDocstringMarkupTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.DOCSTRING_MARKUP_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.DOCSTRING_MARKUP_STYLE));
    }

    public TextAttribute getVariableTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.VARIABLE_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.VARIABLE_STYLE));
    }

    public TextAttribute getPropertyTextAttribute() {

        return new TextAttribute(getNamedColor(
                PyDevEditorPreferences.PROPERTY_COLOR), null,
                preferences.getInt(PyDevEditorPreferences.PROPERTY_STYLE));
    }
    //[[[end]]]

}
