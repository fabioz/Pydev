/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameAnyLocalProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * No definition (will look for the name)
     */
    public PyRenameAnyLocalProcess() {
        super(null);
    }

    private Boolean attributeSearch;

    public boolean getAttributeSearch() {
        if (attributeSearch == null) {
            String[] tokenAndQual = request.ps.getActivationTokenAndQual(true);
            String completeNameToFind = tokenAndQual[0] + tokenAndQual[1];
            attributeSearch = completeNameToFind.indexOf('.') != -1;
        }
        return attributeSearch;
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        List<ASTEntry> oc = getOccurrences(request, request.initialName, (SourceModule) request.getModule());
        addOccurrences(request, oc);
    }

    private List<ASTEntry> getOccurrences(RefactoringRequest request, String completeNameToFind, SourceModule module) {

        List<ASTEntry> oc = new ArrayList<ASTEntry>();
        SimpleNode root = module.getAst();

        if (!getAttributeSearch()) {
            List<ASTEntry> occurrencesWithScopeAnalyzer = getOccurrencesWithScopeAnalyzer(request, module);
            oc.addAll(occurrencesWithScopeAnalyzer);

            if (occurrencesWithScopeAnalyzer.size() == 0) {
                oc.addAll(ScopeAnalysis.getLocalOccurrences(request.initialName, root, false));
            }

        } else {
            //attribute search
            oc.addAll(ScopeAnalysis.getAttributeReferences(request.initialName, root));
        }
        if (oc.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            oc.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, root));
            oc.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, root));
        }
        return oc;
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        return getOccurrences(request, initialName, module);
    }
}
