/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.text.edits.ReplaceEdit;
import org.eclipse.text.edits.TextEdit;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public abstract class AbstractRefactorProcess implements IRefactorProcess{

    protected RefactoringRequest request;

    protected List<Tuple<TextEdit, String>> getAllRenameEdits(List<ASTEntry> ocurrences) {
        List<Tuple<TextEdit, String>> ret = new ArrayList<Tuple<TextEdit, String>>();
        StringBuffer buf = new StringBuffer();
        buf.append("Change: ");
        buf.append(request.duringProcessInfo.initialName);
        buf.append(" >> ");
        buf.append(request.duringProcessInfo.name);
        buf.append(" (line:");
        for(ASTEntry entry : ocurrences){
            StringBuffer entryBuf = new StringBuffer(buf.toString());
            entryBuf.append(entry.node.beginLine);
            entryBuf.append(")");
            int offset = request.ps.getAbsoluteCursorOffset(entry.node.beginLine-1, entry.node.beginColumn-1);
            ret.add(new Tuple<TextEdit, String>(createRenameEdit(offset), entryBuf.toString()));
        }
        return ret;
    }

    protected TextEdit createRenameEdit(int offset) {
        return new ReplaceEdit(offset, request.duringProcessInfo.initialName.length(), request.duringProcessInfo.name);
    }

}
