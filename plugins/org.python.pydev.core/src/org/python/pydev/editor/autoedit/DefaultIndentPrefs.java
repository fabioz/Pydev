/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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

import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.ITabChangedListener;
import org.python.pydev.plugin.preferences.PyDevEditorPreferences;
import org.python.pydev.plugin.preferences.PyDevTypingPreferences;
import org.python.pydev.plugin.preferences.PyScopedPreferences;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.callbacks.ListenerList;

/**
 * Provides indentation preferences from the preferences set in the preferences pages within eclipse.
 */
public class DefaultIndentPrefs extends AbstractIndentPrefs {

    /**
     * Cache for indentation string
     */
    private String indentString = null;

    private String lastIndentString = null;

    private boolean lastUseSpaces;

    private int lastTabWidth;

    private final IAdaptable projectAdaptable;

    /**
     * Singleton instance for the preferences
     */
    private static IIndentPrefs indentPrefs;

    private ListenerList<ITabChangedListener> listenerList = new ListenerList<>(ITabChangedListener.class);

    @Override
    public void addTabChangedListener(ITabChangedListener listener) {
        listenerList.add(listener);
    }

    /**
     * Should only be used on tests (and on a finally it should be set to null again in the test).
     */
    public synchronized static void set(IIndentPrefs indentPrefs) {
        DefaultIndentPrefs.indentPrefs = indentPrefs;
    }

    /**
     * @param an IAdaptable which must adapt to IProject.
     * @return the indentation preferences to be used
     */
    public static IIndentPrefs get(IAdaptable projectAdaptable) {
        if (indentPrefs != null) {
            return indentPrefs;
        }
        if (SharedCorePlugin.inTestMode()) {
            return new TestIndentPrefs(true, 4);
        }
        return new DefaultIndentPrefs(projectAdaptable);
    }

    /**
     * Not singleton (each pyedit may force to use tabs or not).
     */
    public DefaultIndentPrefs(IAdaptable projectAdaptable) {
        this.projectAdaptable = projectAdaptable;
        lastUseSpaces = getBoolFromPreferences(PyDevEditorPreferences.SUBSTITUTE_TABS);
        regenerateIndentString();
    }

    @Override
    public boolean getUseSpaces(boolean considerForceTabs) {
        boolean boolFromPreferences = getBoolFromPreferences(PyDevEditorPreferences.SUBSTITUTE_TABS);
        if (lastUseSpaces != boolFromPreferences) {
            lastUseSpaces = boolFromPreferences;
            regenerateIndentString();
        }
        if (considerForceTabs && getForceTabs()) {
            return false; //forcing tabs.
        }
        return lastUseSpaces;
    }

    @Override
    public void setForceTabs(boolean forceTabs) {
        super.setForceTabs(forceTabs);
        regenerateIndentString(); //When forcing tabs, we must update the cache.
    }

    @Override
    public int getTabWidth() {
        if (lastTabWidth != getIntFromPreferences(PyDevEditorPreferences.TAB_WIDTH, 1)) {
            lastTabWidth = getIntFromPreferences(PyDevEditorPreferences.TAB_WIDTH, 1);
            regenerateIndentString();
        }
        return lastTabWidth;
    }

    Boolean lastGuessTabSubstitution = null;

    @Override
    public boolean getGuessTabSubstitution() {
        boolean curr = getBoolFromPreferences(PyDevEditorPreferences.GUESS_TAB_SUBSTITUTION);
        if (lastGuessTabSubstitution != null && lastGuessTabSubstitution != curr) {
            regenerateIndentString();
        }
        lastGuessTabSubstitution = curr;
        return curr;
    }

    @Override
    public void regenerateIndentString() {
        indentString = super.getIndentationString();
        if (lastIndentString == null || !lastIndentString.equals(indentString)) {
            lastIndentString = indentString;
            ITabChangedListener[] listeners = this.listenerList.getListeners();
            for (ITabChangedListener iTabChangedListener : listeners) {
                iTabChangedListener.onTabSettingsChanged(this);
            }
        }
    }

    /**
     * This class also puts the indentation string in a cache and redoes it
     * if the preferences are changed.
     *
     * @return the indentation string.
     */
    @Override
    public String getIndentationString() {
        return indentString;
    }

    /**
     * @see org.python.pydev.core.IIndentPrefs#getAutoParentesis()
     */
    @Override
    public boolean getAutoParentesis() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_PAR);
    }

    @Override
    public boolean getAutoLink() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_LINK);
    }

    @Override
    public boolean getIndentToParLevel() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_INDENT_TO_PAR_LEVEL);
    }

    @Override
    public boolean getAutoColon() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_COLON);
    }

    @Override
    public boolean getAutoBraces() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_BRACES);
    }

    @Override
    public boolean getAutoWriteImport() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_WRITE_IMPORT_STR);
    }

    @Override
    public boolean getSmartIndentPar() {
        return getBoolFromPreferences(PyDevTypingPreferences.SMART_INDENT_PAR);
    }

    @Override
    public boolean getIndentToParAsPep8() {
        return getBoolFromPreferences(PyDevTypingPreferences.INDENT_AFTER_PAR_AS_PEP8);
    }

    @Override
    public boolean getAutoAddSelf() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_ADD_SELF);
    }

    @Override
    public boolean getAutoDedentElse() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_DEDENT_ELSE);
    }

    @Override
    public int getIndentAfterParWidth() {
        return getIntFromPreferences(PyDevTypingPreferences.AUTO_INDENT_AFTER_PAR_WIDTH, 1);
    }

    @Override
    public boolean getSmartLineMove() {
        return getBoolFromPreferences(PyDevTypingPreferences.SMART_LINE_MOVE);
    }

    @Override
    public boolean getAutoLiterals() {
        return getBoolFromPreferences(PyDevTypingPreferences.AUTO_LITERALS);
    }

    @Override
    public boolean getTabStopInComment() {
        return getBoolFromPreferences(PyDevEditorPreferences.TAB_STOP_IN_COMMENT);
    }

    private boolean getBoolFromPreferences(String pref) {
        return PyScopedPreferences.getBoolean(pref, projectAdaptable);
    }

    private int getIntFromPreferences(String pref, int minVal) {
        return PyScopedPreferences.getInt(pref, projectAdaptable, minVal);
    }

}