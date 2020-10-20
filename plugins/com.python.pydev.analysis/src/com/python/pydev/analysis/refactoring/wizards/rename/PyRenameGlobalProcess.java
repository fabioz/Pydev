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
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
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
        CompletionCache completionCache = new CompletionCache();

        List<ASTEntry> ret = new ArrayList<ASTEntry>();

        List<ASTEntry> localOccurrences = ScopeAnalysis.getLocalOccurrences(initialName, module.getAst(), false, true);
        if (localOccurrences.size() > 0) {
            try {
                List<IDefinition> actualDefinitions = request.findActualDefinitions(completionCache);
                for (ASTEntry occurrence : localOccurrences) {
                    List<IDefinition> foundDefs = new ArrayList<IDefinition>();
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module, occurrence.getName(),
                            foundDefs,
                            occurrence.endLine, occurrence.endCol, module.getNature(), completionCache, false);

                    for (IDefinition def : foundDefs) {
                        IModule defModule = def.getModule();
                        if (defModule == null || (def instanceof Definition && ((Definition) def).ast == null)) {
                            continue;
                        }
                        for (IDefinition actualDef : actualDefinitions) {
                            IModule actualDefModule = actualDef.getModule();
                            if (defModule.getName().equals(actualDefModule.getName())
                                    && def.getLine() == actualDef.getLine()) {
                                ret.add(occurrence);
                            } else if (checkDef(def, module, completionCache, actualDef, false)) {
                                ret.add(occurrence);
                            }
                        }
                    }

                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
        return ret;
    }

    private boolean checkDef(IDefinition iDef, SourceModule module, ICompletionCache completionCache,
            IDefinition iActualDef, boolean checkModule)
            throws CompletionRecursionException, Exception {
        if (iDef == null) {
            return false;
        }
        if (iDef instanceof Definition) {
            if (checkModule) {
                if (((Definition) iDef).module == null || ((Definition) iDef).ast == null) {
                    return false;
                }
                String defModuleName = ((Definition) iDef).module.getName();
                if (iActualDef.getModule().getName().equals(defModuleName)
                        && iDef.getLine() == iActualDef.getLine()) {
                    return true;
                }
            }
            List<IDefinition> foundDefs = new ArrayList<IDefinition>();
            if (iDef instanceof AssignDefinition) {
                AssignDefinition assignDef = (AssignDefinition) iDef;
                if (assignDef.target != null && !assignDef.target.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            assignDef.target,
                            foundDefs,
                            assignDef.line, assignDef.col, module.getNature(), completionCache,
                            false);
                }
                if (assignDef.nodeValue != null && assignDef.value != null && !assignDef.value.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            assignDef.value,
                            foundDefs,
                            assignDef.nodeValue.beginLine, assignDef.nodeValue.beginColumn, module.getNature(),
                            completionCache,
                            false);
                }
                if (assignDef.nodeType != null && assignDef.type != null && !assignDef.type.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            assignDef.type,
                            foundDefs,
                            assignDef.nodeType.beginLine, assignDef.nodeType.beginColumn, module.getNature(),
                            completionCache,
                            false);
                }
            } else {
                Definition def = (Definition) iDef;
                if (def.value != null && !def.value.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            def.value,
                            foundDefs,
                            def.line, def.col, module.getNature(), completionCache,
                            false);
                }
                if (def.nodeType != null && def.type != null && !def.type.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            def.type,
                            foundDefs,
                            def.nodeType.beginLine, def.nodeType.beginColumn, module.getNature(), completionCache,
                            false);
                }
            }
            for (IDefinition foundDef : foundDefs) {
                if (iDef.equals(foundDef)) {
                    continue;
                }
                if (checkDef(foundDef, module, completionCache, iActualDef, true)) {
                    return true;
                }
            }
        }
        return false;
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
