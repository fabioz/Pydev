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

public class PyRenameSelfAttributeProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * Target is 'self.attr'
     */
    private String target;

    public PyRenameSelfAttributeProcess(Definition definition, String target) {
        super(definition);
        this.target = target;
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode root = request.getAST();
        List<ASTEntry> oc = ScopeAnalysis.getAttributeReferences(request.initialName, root);
        if (oc.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            oc.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
            oc.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        }
        addOccurrences(request, oc);
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        SimpleNode root = module.getAst();
        List<ASTEntry> oc = ScopeAnalysis.getAttributeReferences(initialName, root);
        if (oc.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            oc.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
            oc.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        }
        return oc; //will get the self.xxx occurrences
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

}
