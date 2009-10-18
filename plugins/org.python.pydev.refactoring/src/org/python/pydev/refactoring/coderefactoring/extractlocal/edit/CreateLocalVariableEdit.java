/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.edit;

import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.visitors.scope.GetNodeForExtractLocalVisitor;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;

public class CreateLocalVariableEdit extends AbstractInsertEdit {

    private RefactoringInfo info;

    private String variableName;

    private exprType expression;
    
    private int lineForLocal = -1;

    public CreateLocalVariableEdit(ExtractLocalRequest req) {
        super(req);
        this.info = req.info;
        this.variableName = req.variableName;
        this.expression = (exprType) req.expression.createCopy();
    }

    @Override
    protected SimpleNode getEditNode() {
        exprType variable = new Name(variableName, expr_contextType.Store, false);
        exprType[] target = { variable };

        return new Assign(target, expression);
    }

    
    private int calculateLineForLocal() {
        if(lineForLocal == -1){
            PySelection selection = new PySelection(info.getDocument(), info.getExtendedSelection());
            int startLineIndex = selection.getStartLineIndex();
            startLineIndex += 1; //from doc to ast
            Module module = info.getModuleAdapter().getASTNode();
            
            GetNodeForExtractLocalVisitor visitor = new GetNodeForExtractLocalVisitor(startLineIndex);
            try{
                module.accept(visitor);
            }catch(Exception e){
                throw new RuntimeException(e);
            }
            SimpleNode lastNodeBeforePassedLine = visitor.getLastInContextBeforePassedLine();
            if(lastNodeBeforePassedLine != null){
                lineForLocal = lastNodeBeforePassedLine.beginLine-1;
            }else{
                lineForLocal = selection.getStartLineIndex();
            }
        }
        return lineForLocal;
    }

    @Override
    public int getOffset() {
        PySelection selection = new PySelection(info.getDocument(), calculateLineForLocal(), 0);
        return selection.getStartLineOffset();
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    protected String getIndentation(int indent) {
        PySelection selection = new PySelection(info.getDocument(), calculateLineForLocal(), 0);
        return selection.getIndentationFromLine();
    }

}
