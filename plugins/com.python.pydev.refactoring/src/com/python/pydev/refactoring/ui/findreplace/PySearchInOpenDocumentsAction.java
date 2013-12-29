/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.ui.findreplace;

import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.ui.internal.texteditor.TextEditorPlugin;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.IOfflineActionWithParameters;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Action to search in open documents.
 * 
 * Uses the information that the FindReplace dialog obtained on whether the search is case-sensitive,
 * whole word, regexp.
 * 
 * To know what to search, it can use the parameters passed to the action or get the currently
 * selected text in the editor.
 */
public class PySearchInOpenDocumentsAction extends Action implements IOfflineActionWithParameters {

    private List<String> parameters;
    private PyEdit edit;

    public PySearchInOpenDocumentsAction(PyEdit edit) {
        this.edit = edit;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public void run() {
        IDialogSettings settings = TextEditorPlugin.getDefault().getDialogSettings();
        IDialogSettings s = settings.getSection("org.eclipse.ui.texteditor.FindReplaceDialog");
        boolean caseSensitive = false;
        boolean wholeWord = false;
        boolean isRegEx = false;
        if (s != null) {
            caseSensitive = s.getBoolean("casesensitive"); //$NON-NLS-1$
            wholeWord = s.getBoolean("wholeword"); //$NON-NLS-1$
            isRegEx = s.getBoolean("isRegEx"); //$NON-NLS-1$
        }

        String searchText = "";
        if (parameters != null) {
            searchText = StringUtils.join(" ", parameters);
        }
        if (searchText.length() == 0) {
            PySelection ps = new PySelection(edit);
            searchText = ps.getSelectedText();
        }
        IStatusLineManager statusLineManager = edit.getStatusLineManager();
        if (searchText.length() == 0) {
            InputDialog d = new InputDialog(EditorUtils.getShell(), "Text to search", "Enter text to search.", "", null);

            int retCode = d.open();
            if (retCode == InputDialog.OK) {
                searchText = d.getValue();
            }
        }

        if (searchText.length() >= 0) {
            if (wholeWord && !isRegEx && isWord(searchText)) {
                isRegEx = true;
                searchText = "\\b" + searchText + "\\b";
            }

            FindInOpenDocuments.findInOpenDocuments(searchText, caseSensitive, wholeWord, isRegEx, statusLineManager);
        }
    }

    /**
     * Tests whether each character in the given string is a letter.
     *
     * @param str the string to check
     * @return <code>true</code> if the given string is a word
     */
    private boolean isWord(String str) {
        if (str == null || str.length() == 0) {
            return false;
        }

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isJavaIdentifierPart(str.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
