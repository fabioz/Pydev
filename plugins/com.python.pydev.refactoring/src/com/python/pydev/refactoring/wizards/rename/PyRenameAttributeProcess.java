/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 20, 2006
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameAttributeProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * Target is the full name. E.g.: foo.bar (and the initialName would be just 'bar')
     */
    private String target;

    public PyRenameAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode ast = request.getAST();

        List<ASTEntry> attributeOccurrences = new ArrayList<ASTEntry>();
        attributeOccurrences.addAll(ScopeAnalysis.getAttributeReferences(request.initialName, ast));
        if (attributeOccurrences.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            attributeOccurrences.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, ast));
            attributeOccurrences.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, ast));
        }
        addOccurrences(request, attributeOccurrences);
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        return ScopeAnalysis.getAttributeReferences(initialName, module.getAst()); //will get the self.xxx occurrences
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

}
