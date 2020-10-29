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
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;

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
                List<Tuple<Integer, Integer>> possiblePositions = new ArrayList<Tuple<Integer, Integer>>();
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
                            } else {
                                addPossiblePositions(def, module, completionCache, actualDef, possiblePositions);
                            }
                        }
                    }

                }
                for (ASTEntry occurrence : localOccurrences) {
                    if (initialName.equals(occurrence.getName()) && !ret.contains(occurrence)) {
                        for (Tuple<Integer, Integer> possiblePosition : possiblePositions) {
                            if (possiblePosition.o1 == occurrence.endLine && possiblePosition.o2 == occurrence.endCol) {
                                ret.add(occurrence);
                                break;
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

    private void addPossiblePositions(IDefinition iDef, SourceModule module, ICompletionCache completionCache,
            IDefinition iActualDef, List<Tuple<Integer, Integer>> possiblePositions)
            throws CompletionRecursionException, Exception {
        if (iDef instanceof Definition) {
            List<IDefinition> foundDefs = new ArrayList<IDefinition>();
            if (iDef instanceof AssignDefinition) {
                AssignDefinition assignDef = (AssignDefinition) iDef;
                if (assignDef.target != null && !assignDef.target.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            assignDef.target,
                            foundDefs,
                            assignDef.line, assignDef.col, module.getNature(), completionCache,
                            false);
                    if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                        possiblePositions.add(new Tuple<Integer, Integer>(assignDef.line, assignDef.col));
                    }
                }
                if (assignDef.nodeValue != null && assignDef.value != null) {
                    if (assignDef.nodeValue instanceof BinOp || assignDef.nodeValue instanceof BoolOp
                            || assignDef.nodeValue instanceof Compare) {
                        searchDefsInValue(assignDef.nodeValue, module, completionCache, possiblePositions,
                                iDef, iActualDef);
                    } else if (!assignDef.value.isEmpty()) {
                        PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                                assignDef.value,
                                foundDefs,
                                assignDef.nodeValue.beginLine, assignDef.nodeValue.beginColumn, module.getNature(),
                                completionCache,
                                false);
                        if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                            possiblePositions.add(new Tuple<Integer, Integer>(assignDef.nodeValue.beginLine,
                                    assignDef.nodeValue.beginColumn));
                        }
                    }
                }
                if (assignDef.nodeType != null && assignDef.type != null && !assignDef.type.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            assignDef.type,
                            foundDefs,
                            assignDef.nodeType.beginLine, assignDef.nodeType.beginColumn, module.getNature(),
                            completionCache,
                            false);
                    if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                        possiblePositions.add(new Tuple<Integer, Integer>(assignDef.nodeType.beginLine,
                                assignDef.nodeType.beginColumn));
                    }
                }
            } else {
                Definition def = (Definition) iDef;
                if (def.value != null && !def.value.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            def.value,
                            foundDefs,
                            def.line, def.col, module.getNature(), completionCache,
                            false);
                    if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                        possiblePositions.add(new Tuple<Integer, Integer>(def.line, def.col));
                    }
                }
                if (def.nodeType != null && def.type != null && !def.type.isEmpty()) {
                    PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                            def.type,
                            foundDefs,
                            def.nodeType.beginLine, def.nodeType.beginColumn, module.getNature(), completionCache,
                            false);
                    if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                        possiblePositions
                                .add(new Tuple<Integer, Integer>(def.nodeType.beginLine, def.nodeType.beginColumn));
                    }
                }
            }
        }
    }

    private void searchDefsInValue(exprType value, IModule module,
            ICompletionCache completionCache, List<Tuple<Integer, Integer>> possiblePositions, IDefinition iDef,
            IDefinition iActualDef) throws CompletionRecursionException, Exception {
        if (value == null) {
            return;
        }
        if (value instanceof BinOp) {
            BinOp binOp = (BinOp) value;
            searchDefsInValue(binOp.left, module, completionCache, possiblePositions, iDef, iActualDef);
            searchDefsInValue(binOp.right, module, completionCache, possiblePositions, iDef, iActualDef);

        } else if (value instanceof BoolOp) {
            exprType[] values = ((BoolOp) value).values;
            for (exprType v : values) {
                searchDefsInValue(v, module, completionCache, possiblePositions, iDef, iActualDef);
            }
        } else if (value instanceof Compare) {
            Compare compare = (Compare) value;
            searchDefsInValue(compare.left, module, completionCache, possiblePositions, iDef, iActualDef);
            for (exprType comparator : compare.comparators) {
                searchDefsInValue(comparator, module, completionCache, possiblePositions, iDef, iActualDef);
            }
        } else if (!(value instanceof Str)) {
            String rep = NodeUtils.getFullRepresentationString(value);
            if (rep != null && !rep.isEmpty()) {
                List<IDefinition> foundDefs = new ArrayList<IDefinition>();
                PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module,
                        rep,
                        foundDefs,
                        value.beginLine, value.beginColumn, module.getNature(),
                        completionCache,
                        false);
                if (checkFoundDefs(foundDefs, iDef, iActualDef)) {
                    possiblePositions.add(new Tuple<Integer, Integer>(value.beginLine, value.beginColumn));
                }
            }
        }
    }

    private static boolean checkFoundDefs(List<IDefinition> foundDefs, IDefinition iDef, IDefinition iActualDef) {
        for (IDefinition foundDef : foundDefs) {
            if (foundDef == null || iDef.equals(foundDef)) {
                continue;
            }
            if (foundDef instanceof Definition) {
                if (((Definition) foundDef).module == null || ((Definition) foundDef).ast == null) {
                    continue;
                }
                String foundDefModuleName = ((Definition) foundDef).module.getName();
                if (iActualDef.getModule().getName().equals(foundDefModuleName)
                        && foundDef.getLine() == iActualDef.getLine()) {
                    foundDefs.clear();
                    return true;
                }
            }
        }
        foundDefs.clear();
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
