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

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.ast.codecompletion.revisited.CompletionCache;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.StringUtils;

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
        ArrayList<IDefinition> foundDefs = new ArrayList<IDefinition>();
        CompletionCache completionCache = new CompletionCache();
        SimpleNode searchStringsAt = module.getAst();

        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        List<ASTEntry> localOccurrences = ScopeAnalysis.getLocalOccurrences(initialName, module.getAst(), false, true);
        if (localOccurrences.size() > 0 && searchStringsAt != null) {
            try {
                for (ASTEntry occurrence : localOccurrences) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module, occurrence.getName(),
                            foundDefs,
                            occurrence.endLine, occurrence.endCol, module.getNature(), completionCache);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }

        for (IDefinition def : foundDefs) {
            if (def != null && module.equals(def.getModule())) {
                if (def instanceof Definition) {
                    Definition d = (Definition) def;
                    if (d.ast == null || d.ast instanceof ClassDef || d.ast instanceof FunctionDef) {
                        continue;
                    }
                    String value = d.value;
                    if (value != null && !value.isEmpty()) {
                        List<String> s = StringUtils.split(value, '.');
                        if (!s.contains("self") && s.contains(initialName)) {
                            addInitialNameMatchOccurrences(ret, localOccurrences, initialName);
                            break;
                        }
                    }
                    if (def instanceof AssignDefinition) {
                        String target = ((AssignDefinition) def).target;
                        if (target != null && !target.isEmpty()) {
                            List<String> s = StringUtils.split(target, '.');
                            if (!s.contains("self") && s.contains(initialName)) {
                                addInitialNameMatchOccurrences(ret, localOccurrences, initialName);
                                break;
                            }
                        }
                    }
                }
            }
        }
        return ret;
    }

    private void addInitialNameMatchOccurrences(List<ASTEntry> ret, List<ASTEntry> localOccurrences,
            String initialName) {
        for (ASTEntry occurrence : localOccurrences) {
            if (occurrence.getName().equals(initialName)) {
                ret.add(occurrence);
            }
        }
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
