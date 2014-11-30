/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * @author: ptoofani
 * Created: June 2004
 */

package org.python.pydev.editor.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.EditorUtils;

/**
 * Converts tab-width spacing to tab characters in selection or entire document,
 * if nothing selected.
 * 
 * @author Parhaum Toofanian
 */
public class PyConvertSpaceToTab extends PyAction {

    /* Selection element */
    private PySelection ps;

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
            ITextEditor textEditor = getTextEditor();
            ps = new PySelection(textEditor);
            ps.selectAll(false);
            // Perform the action
            perform(ps, textEditor);

            // Put cursor at the first area of the selection
            textEditor.selectAndReveal(ps.getLineOffset(), 0);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps
     *            Given PySelection
     * @param textEditor 
     * @return boolean The success or failure of the action
     */
    public static boolean perform(PySelection ps, ITextEditor textEditor) {
        // What we'll be replacing the selected text with
        FastStringBuffer strbuf = new FastStringBuffer();

        // If they selected a partial line, count it as a full one
        ps.selectCompleteLine();

        int i;

        try {
            // For each line, strip their whitespace
            String tabSpace = getTabSpace(textEditor);
            if (tabSpace == null) {
                return false; //could not get it
            }
            IDocument doc = ps.getDoc();
            int endLineIndex = ps.getEndLineIndex();
            String endLineDelim = ps.getEndLineDelim();

            for (i = ps.getStartLineIndex(); i <= endLineIndex; i++) {
                IRegion lineInformation = doc.getLineInformation(i);
                String line = doc.get(lineInformation.getOffset(), lineInformation.getLength());
                strbuf.append(line.replaceAll(tabSpace, "\t")).append((i < endLineIndex ? endLineDelim : ""));
            }

            // If all goes well, replace the text with the modified information
            doc.replace(ps.getStartLine().getOffset(), ps.getSelLength(), strbuf.toString());
            return true;
        } catch (Exception e) {
            beep(e);
        }

        // In event of problems, return false
        return false;
    }

    /**
     * Currently returns an int of the Preferences' Tab Width.
     * @param textEditor 
     * 
     * @return Tab width in preferences
     */
    protected static String getTabSpace(ITextEditor textEditor) {
        class NumberValidator implements IInputValidator {

            /*
             * @see IInputValidator#isValid(String)
             */
            public String isValid(String input) {

                if (input == null || input.length() == 0) {
                    return " ";
                }

                try {
                    int i = Integer.parseInt(input);
                    if (i <= 0) {
                        return "Must be more than 0.";
                    }

                } catch (NumberFormatException x) {
                    return x.getMessage();
                }

                return null;
            }
        }

        InputDialog inputDialog = new InputDialog(EditorUtils.getShell(), "Tab length",
                "How many spaces should be considered for each tab?", ""
                        + DefaultIndentPrefs.get(textEditor).getTabWidth(),
                new NumberValidator());

        if (inputDialog.open() != InputDialog.OK) {
            return null;
        }

        int tabWidth = Integer.parseInt(inputDialog.getValue());
        return new FastStringBuffer(tabWidth).appendN(' ', tabWidth).toString();
    }
}
