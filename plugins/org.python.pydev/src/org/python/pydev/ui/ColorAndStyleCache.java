/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ui;

import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.preferences.IPydevPreferencesProvider;

public class ColorAndStyleCache extends ColorCache {

    public ColorAndStyleCache(IPreferenceStore prefs) {
        super(prefs);
    }

    @SuppressWarnings("unchecked")
    public static boolean isColorOrStyleProperty(String property) {
        if (property.endsWith("_COLOR") || property.endsWith("_STYLE")) {
            return true;
        }
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            if (iPydevPreferencesProvider.isColorOrStyleProperty(property)) {
                return true;
            }
        }

        return false;
    }

    //Note that to update the code below, the install.py of this plugin should be run.

    /*[[[cog
    import cog
    
    template = '''
    @SuppressWarnings("unchecked")
    public TextAttribute get%sTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.get%sTextAttribute();
            if(textAttribute != null){
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(
                PydevEditorPrefs.%s_COLOR), null, preferences.getInt(PydevEditorPrefs.%s_STYLE));
    }'''
    
    for s in ('self', 'code', 'decorator', 'number', 'class_name', 'func_name', 'comment', 'backquotes', 'string', 'keyword', 'parens', 'operators'):
        
        cog.outl(template % (s.title().replace('_', ''), s.title().replace('_', ''), s.upper(), s.upper()))

    ]]]*/

    @SuppressWarnings("unchecked")
    public TextAttribute getSelfTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getSelfTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.SELF_COLOR), null,
                preferences.getInt(PydevEditorPrefs.SELF_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getCodeTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getCodeTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.CODE_COLOR), null,
                preferences.getInt(PydevEditorPrefs.CODE_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getDecoratorTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getDecoratorTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.DECORATOR_COLOR), null,
                preferences.getInt(PydevEditorPrefs.DECORATOR_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getNumberTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getNumberTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.NUMBER_COLOR), null,
                preferences.getInt(PydevEditorPrefs.NUMBER_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getClassNameTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getClassNameTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.CLASS_NAME_COLOR), null,
                preferences.getInt(PydevEditorPrefs.CLASS_NAME_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getFuncNameTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getFuncNameTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.FUNC_NAME_COLOR), null,
                preferences.getInt(PydevEditorPrefs.FUNC_NAME_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getCommentTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getCommentTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.COMMENT_COLOR), null,
                preferences.getInt(PydevEditorPrefs.COMMENT_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getBackquotesTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getBackquotesTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.BACKQUOTES_COLOR), null,
                preferences.getInt(PydevEditorPrefs.BACKQUOTES_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getStringTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getStringTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.STRING_COLOR), null,
                preferences.getInt(PydevEditorPrefs.STRING_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getKeywordTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getKeywordTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.KEYWORD_COLOR), null,
                preferences.getInt(PydevEditorPrefs.KEYWORD_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getParensTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getParensTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.PARENS_COLOR), null,
                preferences.getInt(PydevEditorPrefs.PARENS_STYLE));
    }

    @SuppressWarnings("unchecked")
    public TextAttribute getOperatorsTextAttribute() {
        List<IPydevPreferencesProvider> participants = ExtensionHelper
                .getParticipants(ExtensionHelper.PYDEV_PREFERENCES_PROVIDER);
        for (IPydevPreferencesProvider iPydevPreferencesProvider : participants) {
            TextAttribute textAttribute = iPydevPreferencesProvider.getOperatorsTextAttribute();
            if (textAttribute != null) {
                return textAttribute;
            }
        }
        return new TextAttribute(getNamedColor(PydevEditorPrefs.OPERATORS_COLOR), null,
                preferences.getInt(PydevEditorPrefs.OPERATORS_STYLE));
    }
    //[[[end]]]

}
