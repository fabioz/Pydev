/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.analysis.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.ast.codecompletion.revisited.CompletionState;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.OrderedSet;

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
        ICompletionState completionCache = new CompletionState();
        completionCache.setAcceptTypeshed(request.acceptTypeshed);

        Set<ASTEntry> ret = new OrderedSet<ASTEntry>();

        List<ASTEntry> localOccurrences = ScopeAnalysis.getLocalOccurrences(initialName, module.getAst(), false);
        localOccurrences.addAll(ScopeAnalysis.getFullAttributeReferencesFromPartialName(initialName, module.getAst()));
        if (localOccurrences.size() > 0) {
            try {
                List<IDefinition> actualDefinitions = request.findActualDefinitions(completionCache);
                for (ASTEntry occurrence : localOccurrences) {
                    String fullRepresentationString = NodeUtils.getFullRepresentationString(occurrence.node);
                    List<IDefinition> foundDefs = PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(),
                            request.acceptTypeshed, module, fullRepresentationString, null, occurrence.node.beginLine,
                            occurrence.node.beginColumn, module.getNature(), completionCache, false);

                    for (IDefinition def : foundDefs) {
                        IModule defModule = def.getModule();
                        if (defModule == null || (def instanceof Definition && ((Definition) def).ast == null)) {
                            // If we're unsure, just add it.
                            ret.add(occurrence);
                            continue;
                        }
                        for (IDefinition actualDef : actualDefinitions) {
                            IModule actualDefModule = actualDef.getModule();
                            if (defModule.getName().equals(actualDefModule.getName())) {
                                ret.add(occurrence);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return new ArrayList<ASTEntry>(ret);

    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode ast = request.getAST();
        //it was found in another module, but we want to keep things local
        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(request.qualifier, ast,
                request.activationToken.isEmpty());
        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.qualifier, ast));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.qualifier, ast));
        }
        addOccurrences(request, ret);
    }

}
