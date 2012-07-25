/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 5, 2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.autoedit;

import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.cache.PyPreferencesCache;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.plugin.PydevPlugin;

/**
 * Provides indentation preferences from the preferences set in the preferences pages within eclipse.
 */
public class DefaultIndentPrefs extends AbstractIndentPrefs {

    /** 
     * Cache for indentation string 
     */
    private String indentString = null;

    private boolean useSpaces;

    private int tabWidth;

    private static PyPreferencesCache cache;

    /**
     * Singleton instance for the preferences
     */
    private static IIndentPrefs indentPrefs;

    /**
     * @return the indentation preferences to be used
     */
    public synchronized static IIndentPrefs get() {
        if (indentPrefs == null) {
            if (PydevPlugin.getDefault() == null) {
                return new TestIndentPrefs(true, 4);
            }
            indentPrefs = new DefaultIndentPrefs();
        }
        return indentPrefs;
    }

    /**
     * @return a cache for the preferences.
     */
    private PyPreferencesCache getCache() {
        if (cache == null) {
            cache = new PyPreferencesCache(PydevPlugin.getDefault().getPreferenceStore());
        }
        return cache;
    }

    /**
     * Not singleton (each pyedit may force to use tabs or not).
     */
    public DefaultIndentPrefs() {
        PyPreferencesCache c = getCache();
        useSpaces = c.getBoolean(PydevEditorPrefs.SUBSTITUTE_TABS);
        tabWidth = c.getInt(PydevEditorPrefs.TAB_WIDTH, 4);
    }

    public boolean getUseSpaces(boolean considerForceTabs) {
        PyPreferencesCache c = getCache();
        if (useSpaces != c.getBoolean(PydevEditorPrefs.SUBSTITUTE_TABS)) {
            useSpaces = c.getBoolean(PydevEditorPrefs.SUBSTITUTE_TABS);
            regenerateIndentString();
        }
        if (considerForceTabs && getForceTabs()) {
            return false; //forcing tabs.
        }
        return useSpaces;
    }

    public void setForceTabs(boolean forceTabs) {
        super.setForceTabs(forceTabs);
        regenerateIndentString(); //When forcing tabs, we must update the cache.
    }

    public static int getStaticTabWidth() {
        PydevPlugin default1 = PydevPlugin.getDefault();
        if (default1 == null) {
            return 4;
        }
        int w = default1.getPluginPreferences().getInt(PydevEditorPrefs.TAB_WIDTH);
        if (w <= 0) { //tab width should never be 0 or less (in this case, let's make the default 4)
            w = 4;
        }
        return w;
    }

    public int getTabWidth() {
        PyPreferencesCache c = getCache();
        if (tabWidth != c.getInt(PydevEditorPrefs.TAB_WIDTH, 4)) {
            tabWidth = c.getInt(PydevEditorPrefs.TAB_WIDTH, 4);
            regenerateIndentString();
        }
        return tabWidth;
    }

    public void regenerateIndentString() {
        PyPreferencesCache c = getCache();
        c.clear(PydevEditorPrefs.TAB_WIDTH);
        c.clear(PydevEditorPrefs.SUBSTITUTE_TABS);
        indentString = super.getIndentationString();
    }

    /**
     * This class also puts the indentation string in a cache and redoes it 
     * if the preferences are changed.
     * 
     * @return the indentation string. 
     */
    public String getIndentationString() {
        if (indentString == null) {
            regenerateIndentString();
        }

        return indentString;
    }

    /** 
     * @see org.python.pydev.core.IIndentPrefs#getAutoParentesis()
     */
    public boolean getAutoParentesis() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_PAR);
    }

    public boolean getAutoLink() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_LINK);
    }

    public boolean getIndentToParLevel() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_INDENT_TO_PAR_LEVEL);
    }

    public boolean getAutoColon() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_COLON);
    }

    public boolean getAutoBraces() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_BRACES);
    }

    public boolean getAutoWriteImport() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_WRITE_IMPORT_STR);
    }

    public boolean getSmartIndentPar() {
        return getCache().getBoolean(PydevEditorPrefs.SMART_INDENT_PAR);
    }

    public boolean getAutoAddSelf() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_ADD_SELF);
    }

    public boolean getAutoDedentElse() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_DEDENT_ELSE);
    }

    public int getIndentAfterParWidth() {
        return getCache().getInt(PydevEditorPrefs.AUTO_INDENT_AFTER_PAR_WIDTH, 1);
    }

    public boolean getSmartLineMove() {
        return getCache().getBoolean(PydevEditorPrefs.SMART_LINE_MOVE);
    }

    public boolean getAutoLiterals() {
        return getCache().getBoolean(PydevEditorPrefs.AUTO_LITERALS);
    }

}