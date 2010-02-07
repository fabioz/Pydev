/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.Tuple;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameLocalProcess extends AbstractRenameRefactorProcess{


    public PyRenameLocalProcess(Definition definition) {
        super(definition);
    }


    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        Tuple<SimpleNode, List<ASTEntry>> tup = ScopeAnalysis.getLocalOccurrences(request.initialName, definition.module, definition.scope);
        List<ASTEntry> ret = tup.o2;
        SimpleNode searchStringsAt = tup.o1;
        if(ret.size() > 0 && searchStringsAt != null){
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, searchStringsAt));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, searchStringsAt));
        }
        addOccurrences(request, ret);
    }

    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        if(!definition.module.getName().equals(request.moduleName)){
            SimpleNode ast = request.getAST();
            //it was found in another module, but we want to keep things local
            List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(request.initialName, ast);
            if(ret.size() > 0){
                //only add comments and strings if there's at least some other occurrence
                ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, ast));
                ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, ast));
            }            
            addOccurrences(request, ret);
        }else{
            findReferencesToRenameOnWorkspace(request, status);
        }
    }


}
