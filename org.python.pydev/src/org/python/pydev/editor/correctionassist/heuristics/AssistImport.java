/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.correctionassist.FixCompletionProposal;
import org.python.pydev.editor.model.AbstractNode;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Fabio Zadrozny
 */
public class AssistImport implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.editor.actions.PySelection, org.python.pydev.ui.ImageCache)
     */
    public List getProps(PySelection ps, ImageCache imageCache, File f, PythonNature nature, AbstractNode root) throws BadLocationException {
        ArrayList l = new ArrayList();
        String sel = PyAction.getLineWithoutComments(ps).trim();

        int i = sel.indexOf("import");
        if(ps.startLineIndex != ps.endLineIndex)
            return l;
        
        
        String delimiter = PyAction.getDelimiter(ps.doc);
        
        int lineToMoveImport = 0;
        int lines = ps.doc.getNumberOfLines();
        for (int line = 0; line < lines; line++) {
            String str = ps.getLine(line);
            if(str.startsWith("import ") || str.startsWith("from ")){
                lineToMoveImport = line;
                break;
            }
        }
        
        int offset = ps.doc.getLineOffset(lineToMoveImport);
        
        
        if(i >= 0){
            l.add(new FixCompletionProposal(sel+delimiter, offset, 0, ps.startLine.getOffset(), imageCache.get(UIConstants.ASSIST_MOVE_IMPORT),
                    "Move import to global scope", null, null, ps.startLineIndex+1));
        }
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.editor.actions.PySelection)
     */
    public boolean isValid(PySelection ps, String sel) {
        return sel.indexOf("import ") != -1;
    }

}
