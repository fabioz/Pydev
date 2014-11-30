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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Converts tab-width spacing to tab characters in selection or entire document, if nothing
 * selected.
 * 
 * @author Parhaum Toofanian
 */
public class PyConvertTabToSpace extends PyConvertSpaceToTab {
    /* Selection element */
    private static PySelection ps;

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
            perform(textEditor);

            // Put cursor at the first area of the selection
            textEditor.selectAndReveal(ps.getLineOffset(), 0);
        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * Performs the action with the class' PySelection.
     * 
     * @return boolean The success or failure of the action
     */
    public static boolean perform(ITextEditor textEditor) {
        return perform(ps, textEditor);
    }

    /**
     * Performs the action with a given PySelection
     * 
     * @param ps Given PySelection
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
            IDocument doc = ps.getDoc();
            String tabSpace = getTabSpace(textEditor);
            int endLineIndex = ps.getEndLineIndex();
            String endLineDelim = ps.getEndLineDelim();
            for (i = ps.getStartLineIndex(); i <= endLineIndex; i++) {
                IRegion lineInformation = doc.getLineInformation(i);
                String line = doc.get(lineInformation.getOffset(), lineInformation.getLength());
                strbuf.append(line.replaceAll("\t", tabSpace) + (i < endLineIndex ? endLineDelim : ""));
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
}
