/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
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
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

/**
 * Only local renaming (i.e.: local variable)
 * 
 * @author fabioz
 */
public class PyRenameLocalProcess extends AbstractRenameRefactorProcess {

    public PyRenameLocalProcess(Definition definition) {
        super(definition);
    }

    @Override
    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        //Only search in local scope
        findReferencesToRenameOnLocalScope(request, status);
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        //Only search in local scope if the place where the definition was found is the same place of the request.
        if (definition.module.getName().equals(request.moduleName)) {
            Tuple<SimpleNode, List<ASTEntry>> tup = ScopeAnalysis.getLocalOccurrences(request.initialName,
                    definition.module, definition.scope);
            List<ASTEntry> ret = tup.o2;
            SimpleNode searchStringsAt = tup.o1;
            if (ret.size() > 0 && searchStringsAt != null) {
                //only add comments and strings if there's at least some other occurrence
                ret.addAll(ScopeAnalysis.getCommentOccurrences(request.initialName, searchStringsAt));
                ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, searchStringsAt));
            }
            addOccurrences(request, ret);
        }
    }

}
