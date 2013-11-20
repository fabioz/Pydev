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
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameAnyLocalProcess extends AbstractRenameRefactorProcess {

    /**
     * No definition (will look for the name)
     */
    public PyRenameAnyLocalProcess() {
        super(null);
    }

    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        String[] tokenAndQual = request.ps.getActivationTokenAndQual(true);
        String completeNameToFind = tokenAndQual[0] + tokenAndQual[1];
        boolean attributeSearch = completeNameToFind.indexOf('.') != -1;

        List<ASTEntry> oc = new ArrayList<ASTEntry>();
        SimpleNode root = request.getAST();

        if (!attributeSearch) {
            List<ASTEntry> occurrencesWithScopeAnalyzer = getOccurrencesWithScopeAnalyzer(request);
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
        addOccurrences(request, oc);
    }

    @Override
    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        status.addWarning(org.python.pydev.shared_core.string.StringUtils.format(
                "Unable to find the definition for the token: %s, so, rename will only happen in the local scope.",
                request.initialName));
        this.findReferencesToRenameOnLocalScope(request, status);
    }
}
