/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

/*
 * @author Robin Stocker, Fabio Zadrozny
 */
package org.python.pydev.refactoring.codegenerator.generatedocstring;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.correctionassist.docstrings.DocstringsPrefPage;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.plugin.StatusInfo;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.core.RefactoringInfo;

// FIXME: Merge with code in AssistDocString.

/**
 * Generates a docstring for a function or a class. The caret can be anywhere
 * within the body of the function or class or on the signature. If there's
 * already a docstring, it makes the caret jump to the end of the docstring.
 */
public class GenerateDocstringOperation implements IWorkspaceRunnable {

    private PyEdit edit;
    
    public GenerateDocstringOperation(PyEdit edit) {
        this.edit = edit;
    }
    
    public void run(IProgressMonitor monitor) throws CoreException {
        RefactoringInfo info;
        try {
            info = new RefactoringInfo(edit, edit.getPythonNature());
        } catch (Throwable e) {
            IStatus status = new StatusInfo(IStatus.ERROR, e.getMessage());
            throw new CoreException(status);
        }
        
        PySelection selection = new PySelection(edit);
        
        selectDefinition(selection, info);
        
        boolean successful = selectEndOfDocstring(selection, info);
        if (successful) {
            return;
        }
        
        Tuple<List<String>, Integer> tuple = selection.getInsideParentesisToks(false);
        if (tuple == null) {
            return;
        }
        List<String> params = tuple.o1;
        
        // Needed because the parameter list can be on multiple lines.
        int lineOfLastParam = selection.getLineOfOffset(tuple.o2);

        String inAndIndent = getInAndIndent(selection);

        FastStringBuffer buf = new FastStringBuffer();
        String docStringMarker = DocstringsPrefPage.getDocstringMarker();
        
        buf.append(inAndIndent + docStringMarker);
        buf.append(inAndIndent);

        int relativeSelectionOffset = buf.length();
        
        if (selection.isInFunctionLine()) {
            for (String paramName : params) {
                if(!PySelection.isIdentifier(paramName)){
                    continue;
                }
                buf.append(inAndIndent).append("@param ").append(paramName).append(":");
                if (DocstringsPrefPage.getTypeTagShouldBeGenerated(paramName)) {
                    buf.append(inAndIndent).append("@type ").append(paramName).append(":");
                }
            }
        }
        
        buf.append(inAndIndent).append(docStringMarker);

        try {
            int insertOffset = selection.getEndLineOffset(lineOfLastParam);
            IDocument document = edit.getDocument();
            document.replace(insertOffset, 0, buf.toString());
            edit.setSelection(insertOffset + relativeSelectionOffset, 0);
        } catch (BadLocationException e) {
            return;
        }
    }
    
    /*
     * Jump to the line where the "def" or "class" is.
     */
    private void selectDefinition(PySelection selection, RefactoringInfo info) {
        AbstractScopeNode<?> scope = info.getScopeAdapter();
        int firstLineInScope = scope.getNodeFirstLine();
        int definitionLine = firstLineInScope - 1;
        
        int offset = selection.getLineOffset(definitionLine);
        selection.setSelection(offset, offset);
    }
    
    private boolean selectEndOfDocstring(PySelection selection, RefactoringInfo info) {
        Str docstring = getDocstring(selection, info);
        
        if (docstring != null) {
            int line = docstring.beginLine - 1;
            int col  = docstring.beginColumn;
            int offset = selection.getAbsoluteCursorOffset(line, col);
            
            // For intelligent selection placement. Go to the end of the last
            // line of the docstring.
            Pattern p = Pattern.compile("\n?(\\t| )*\\z");
            Matcher m = p.matcher(docstring.s);
            m.find();
            offset += m.start();
            
            if (docstring.type == Str.TripleSingle || docstring.type == Str.TripleDouble) {
                offset += 2;
            }
            
            edit.setSelection(offset, 0);
            return true;
        }
        
        return false;
    }
    
    private Str getDocstring(PySelection selection, RefactoringInfo info) {
        Str docstring = null;
        
        AbstractScopeNode<?> scope = info.getScopeAdapter();
        Object node = scope.getASTNode();
        if (node instanceof FunctionDef) {
            FunctionDef def = (FunctionDef) node;
            docstring = extractDocstring(def.body[0]);
        } else if (node instanceof ClassDef) {
            ClassDef def = (ClassDef) node;
            docstring = extractDocstring(def.body[0]);
        }
        return docstring;
    }
    
    private Str extractDocstring(stmtType statement) {
        if (statement instanceof Expr) {
            Expr expr = (Expr) statement;
            if (expr.value instanceof Str) {
                return (Str) expr.value;
            }
        }
        return null;
    }
    
    private String getInAndIndent(PySelection selection) {
        String initial = PySelection.getIndentationFromLine(selection.getCursorLineContents());
        String delimiter = PyAction.getDelimiter(selection.getDoc());
        String indentation = PyAction.getStaticIndentationString(edit);
        String inAndIndent = delimiter + initial + indentation;
        return inAndIndent;
    }
}
