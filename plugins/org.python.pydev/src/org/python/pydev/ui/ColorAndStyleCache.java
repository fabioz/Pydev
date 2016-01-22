/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
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
                PydevEditorPrefs.%s_COLOR), null, preferences.getInt(PydevEditorPrefs.%s_STYLE));
    }'''
    
    for s in ('self', 'code', 'decorator', 'number', 'class_name', 'func_name', 'comment', 'backquotes', 'string', 'unicode', 'keyword', 'parens', 'operators', 'docstring_markup'):
        
        cog.outl(template % (s.title().replace('_', ''), s.upper(), s.upper()))
    
    ]]]*/

    public TextAttribute getSelfTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.SELF_COLOR), null, preferences.getInt(PydevEditorPrefs.SELF_STYLE));
    }

    public TextAttribute getCodeTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.CODE_COLOR), null, preferences.getInt(PydevEditorPrefs.CODE_STYLE));
    }

    public TextAttribute getDecoratorTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.DECORATOR_COLOR), null, preferences.getInt(PydevEditorPrefs.DECORATOR_STYLE));
    }

    public TextAttribute getNumberTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.NUMBER_COLOR), null, preferences.getInt(PydevEditorPrefs.NUMBER_STYLE));
    }

    public TextAttribute getClassNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.CLASS_NAME_COLOR), null, preferences.getInt(PydevEditorPrefs.CLASS_NAME_STYLE));
    }

    public TextAttribute getFuncNameTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.FUNC_NAME_COLOR), null, preferences.getInt(PydevEditorPrefs.FUNC_NAME_STYLE));
    }

    public TextAttribute getCommentTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.COMMENT_COLOR), null, preferences.getInt(PydevEditorPrefs.COMMENT_STYLE));
    }

    public TextAttribute getBackquotesTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.BACKQUOTES_COLOR), null, preferences.getInt(PydevEditorPrefs.BACKQUOTES_STYLE));
    }

    public TextAttribute getStringTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.STRING_COLOR), null, preferences.getInt(PydevEditorPrefs.STRING_STYLE));
    }

    public TextAttribute getUnicodeTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.UNICODE_COLOR), null, preferences.getInt(PydevEditorPrefs.UNICODE_STYLE));
    }

    public TextAttribute getKeywordTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.KEYWORD_COLOR), null, preferences.getInt(PydevEditorPrefs.KEYWORD_STYLE));
    }

    public TextAttribute getParensTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.PARENS_COLOR), null, preferences.getInt(PydevEditorPrefs.PARENS_STYLE));
    }

    public TextAttribute getOperatorsTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.OPERATORS_COLOR), null, preferences.getInt(PydevEditorPrefs.OPERATORS_STYLE));
    }

    public TextAttribute getDocstringMarkupTextAttribute() {

        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.DOCSTRING_MARKUP_COLOR), null, preferences.getInt(PydevEditorPrefs.DOCSTRING_MARKUP_STYLE));
    }
    //[[[end]]]

}
