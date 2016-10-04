/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.plugin.preferences.PydevPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.structure.Tuple;

public abstract class AbstractBlockCommentAction extends PyAction {

    protected boolean alignRight = true;
    protected int defaultCols = 80;

    public AbstractBlockCommentAction() {
        //default
    }

    /**
     * For tests: assigns the default values
     */
    protected AbstractBlockCommentAction(int defaultCols, boolean alignLeft) {
        this.defaultCols = defaultCols;
        this.alignRight = alignLeft;
    }

    /**
     * Grabs the selection information and performs the action.
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }
            // Select from text editor
            PySelection ps = new PySelection(getTextEditor());
            // Perform the action
            Tuple<Integer, Integer> toSelect = perform(ps);
            if (toSelect != null) {
                getTextEditor().selectAndReveal(toSelect.o1, toSelect.o2);
            } else {
                // Put cursor at the first area of the selection
                revealSelEndLine(ps);
            }
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Actually performs the action 
     */
    public abstract Tuple<Integer, Integer> perform(PySelection ps);

    /**
     * @return the number of columns to be used (and the char too)
     */
    public Tuple<Integer, Character> getColsAndChar() {
        int cols = this.defaultCols;
        char c = '-';

        if (SharedCorePlugin.inTestMode()) {
            // use defaults
        } else {
            IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
            cols = chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);

            IPreferenceStore prefs = PydevPrefs.getPreferenceStore();
            c = prefs.getString(getPreferencesNameForChar()).charAt(0);
        }
        return new Tuple<Integer, Character>(cols, c);
    }

    /**
     * @return the editor tab width.
     */
    public int getEditorTabWidth() {
        if (SharedCorePlugin.inTestMode()) {
            return 4; //if not available, default is 4
        }

        IPreferenceStore chainedPrefStore = PydevPrefs.getChainedPrefStore();
        return chainedPrefStore.getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    }

    protected abstract String getPreferencesNameForChar();

    /**
     * @return the length of the string considering the size of the tab for the editor
     */
    protected int getLenOfStrConsideringTabEditorLen(String str) {
        int ret = 0;
        int tabWidth = this.getEditorTabWidth();
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == '\t') {
                ret += tabWidth;
            } else {
                ret += 1;
            }
        }
        return ret;
    }

}
