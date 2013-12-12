/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameGlobalProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * @param definition
     */
    public PyRenameGlobalProcess(Definition definition) {
        super(definition);
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        SimpleNode searchStringsAt = module.getAst();

        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(initialName, module.getAst());
        if (ret.size() > 0 && searchStringsAt != null) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, searchStringsAt));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, searchStringsAt));
        }

        return ret;
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode ast = request.getAST();
        //it was found in another module, but we want to keep things local
        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(request.initialName, ast);
        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, ast));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, ast));
        }
        addOccurrences(request, ret);
    }

}
