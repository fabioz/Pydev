/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 9, 2006
 * @author Fabio
 */
package com.python.pydev.refactoring.refactorer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.AssignDefinition;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.model.ItemPointer;
import org.python.pydev.editor.refactoring.PyRefactoringFindDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.ui.hierarchy.HierarchyNodeModel;

public class RefactorerFinds {

    public static boolean DEBUG = false;

    private Refactorer refactorer;

    public RefactorerFinds(Refactorer refactorer) {
        this.refactorer = refactorer;
    }

    private void findParentDefinitions(IPythonNature nature, IModule module, List<IDefinition> definitions,
            List<String> withoutAstDefinitions, HierarchyNodeModel model, ICompletionCache completionCache,
            RefactoringRequest request) throws Exception {
        //ok, let's find the parents...
        for (exprType exp : model.ast.bases) {
            String n = NodeUtils.getFullRepresentationString(exp);
            final int line = exp.beginLine;
            final int col = exp.beginColumn + n.length(); //the col must be the last char because it can be a dotted name
            if (module != null) {

                ArrayList<IDefinition> foundDefs = new ArrayList<IDefinition>();
                PyRefactoringFindDefinition.findActualDefinition(request.getMonitor(), module, n, foundDefs, line, col,
                        nature, completionCache);

                if (foundDefs.size() > 0) {
                    definitions.addAll(foundDefs);
                } else {
                    withoutAstDefinitions.add(n);
                }
            } else {
                withoutAstDefinitions.add(n);
            }
        }
    }

    private void findParents(IPythonNature nature, Definition d, HierarchyNodeModel initialModel,
            HashMap<HierarchyNodeModel, HierarchyNodeModel> allFound, RefactoringRequest request) throws Exception {

        request.getMonitor().beginTask("Find parents", IProgressMonitor.UNKNOWN);

        try {
            HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
            foundOnRound.add(initialModel);
            CompletionCache completionCache = new CompletionCache();
            while (foundOnRound.size() > 0) {
                HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
                foundOnRound.clear();

                for (HierarchyNodeModel toFindOnRound : nextRound) {
                    List<IDefinition> definitions = new ArrayList<IDefinition>();
                    List<String> withoutAstDefinitions = new ArrayList<String>();
                    findParentDefinitions(nature, toFindOnRound.module, definitions, withoutAstDefinitions,
                            toFindOnRound, completionCache, request);

                    request.communicateWork(StringUtils.format("Found: %s parents for: %s", definitions.size(), d.value));

                    //and add a parent for each definition found (this will make up what the next search we will do)
                    for (IDefinition def : definitions) {
                        Definition definition = (Definition) def;
                        HierarchyNodeModel model2 = createHierarhyNodeFromClassDef(definition);
                        if (model2 != null) {
                            if (allFound.containsKey(model2) == false) {
                                allFound.put(model2, model2);
                                toFindOnRound.parents.add(model2);
                                foundOnRound.add(model2);
                            } else {
                                model2 = allFound.get(model2);
                                Assert.isNotNull(model2);
                                toFindOnRound.parents.add(model2);
                            }
                        } else {
                            withoutAstDefinitions.add(definition.value);
                        }
                    }

                    for (String def : withoutAstDefinitions) {
                        toFindOnRound.parents.add(new HierarchyNodeModel(def));
                    }
                }
            }
        } finally {
            request.getMonitor().done();
        }
    }

    private void findChildren(RefactoringRequest request, HierarchyNodeModel initialModel,
            HashMap<HierarchyNodeModel, HierarchyNodeModel> allFound) {
        try {
            request.getMonitor().beginTask("Find children", 100);
            //and now the children...
            List<AbstractAdditionalDependencyInfo> infoForProject;
            try {
                infoForProject = AdditionalProjectInterpreterInfo
                        .getAdditionalInfoForProjectAndReferencing(request.nature);
            } catch (MisconfigurationException e) {
                Log.log(e);
                return;
            }
            HashSet<HierarchyNodeModel> foundOnRound = new HashSet<HierarchyNodeModel>();
            foundOnRound.add(initialModel);

            int totalWork = 1000;

            while (foundOnRound.size() > 0) {
                HashSet<HierarchyNodeModel> nextRound = new HashSet<HierarchyNodeModel>(foundOnRound);
                foundOnRound.clear();

                for (HierarchyNodeModel toFindOnRound : nextRound) {

                    HashSet<SourceModule> modulesToAnalyze;

                    int work = totalWork / 250;
                    if (work <= 0) {
                        work = 1;
                        totalWork = 0;
                    }
                    totalWork -= work;

                    try {
                        request.pushMonitor(new SubProgressMonitor(request.getMonitor(), work));
                        modulesToAnalyze = findLikelyModulesWithChildren(request, toFindOnRound, infoForProject);
                    } finally {
                        request.popMonitor().done();
                    }

                    request.communicateWork("Likely modules with matches:" + modulesToAnalyze.size());
                    findChildrenOnModules(request, allFound, foundOnRound, toFindOnRound, modulesToAnalyze);
                }
            }
        } finally {
            request.getMonitor().done();
        }
    }

    private void findChildrenOnModules(RefactoringRequest request,
            HashMap<HierarchyNodeModel, HierarchyNodeModel> allFound, HashSet<HierarchyNodeModel> foundOnRound,
            HierarchyNodeModel toFindOnRound, HashSet<SourceModule> modulesToAnalyze) {
        for (SourceModule module : modulesToAnalyze) {
            SourceModule m = module;
            request.communicateWork("Analyzing:" + m.getName());

            Iterator<ASTEntry> entries = EasyASTIteratorVisitor.createClassIterator(m.getAst());

            while (entries.hasNext()) {
                ASTEntry entry = entries.next();
                //we're checking for those that have model.name as a parent
                ClassDef def = (ClassDef) entry.node;
                List<String> parentNames = NodeUtils.getParentNames(def, true);
                if (parentNames.contains(toFindOnRound.name)) {
                    HierarchyNodeModel newNode = new HierarchyNodeModel(module, def);
                    if (allFound.containsKey(newNode) == false) {
                        toFindOnRound.children.add(newNode);
                        allFound.put(newNode, newNode);
                        foundOnRound.add(newNode);
                    } else {
                        newNode = allFound.get(newNode);
                        Assert.isNotNull(newNode);
                        toFindOnRound.children.add(newNode);
                    }
                }
            }
        }
    }

    private HashSet<SourceModule> findLikelyModulesWithChildren(RefactoringRequest request, HierarchyNodeModel model,
            List<AbstractAdditionalDependencyInfo> infoForProject) {
        //get the modules that are most likely to have that declaration.
        HashSet<SourceModule> modulesToAnalyze = new HashSet<SourceModule>();
        for (AbstractAdditionalDependencyInfo additionalInfo : infoForProject) {

            IProgressMonitor monitor = request.getMonitor();
            if (monitor == null) {
                monitor = new NullProgressMonitor();
            }
            monitor.beginTask("Find likely modules with children", 100);

            try {
                List<ModulesKey> modules;
                try {
                    request.pushMonitor(new SubProgressMonitor(monitor, 90));
                    modules = additionalInfo.getModulesWithToken(request.nature.getProject(), model.name, monitor);
                    monitor.setTaskName("Searching: " + model.name);
                    if (monitor.isCanceled()) {
                        throw new OperationCanceledException();
                    }
                } finally {
                    request.popMonitor().done();
                }

                try {
                    request.pushMonitor(new SubProgressMonitor(monitor, 10));
                    request.getMonitor().beginTask("Find likely modules with children", modules.size());
                    for (ModulesKey declaringModuleName : modules) {
                        if (DEBUG) {
                            System.out.println("findLikelyModulesWithChildren: " + declaringModuleName);
                        }

                        IModule module = null;

                        IPythonNature pythonNature = null;
                        if (additionalInfo instanceof AdditionalProjectInterpreterInfo) {
                            AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) additionalInfo;
                            pythonNature = PythonNature.getPythonNature(projectInterpreterInfo.getProject());

                        }
                        if (pythonNature == null) {
                            pythonNature = request.nature;
                        }
                        module = pythonNature.getAstManager().getModule(declaringModuleName.name, pythonNature, false);
                        if (module == null && pythonNature != request.nature) {
                            module = request.nature.getAstManager().getModule(declaringModuleName.name, request.nature,
                                    false);
                        }

                        if (module instanceof SourceModule) {
                            modulesToAnalyze.add((SourceModule) module);
                        }

                        request.getMonitor().worked(1);
                    }
                } finally {
                    request.popMonitor().done();
                }
            } finally {
                monitor.done();
            }
        }
        return modulesToAnalyze;
    }

    /**
     * @return the hierarchy model, having the returned node as our 'point of interest'.
     */
    public HierarchyNodeModel findClassHierarchy(RefactoringRequest request, boolean findOnlyParents) {
        try {
            request.getMonitor().beginTask("Find class hierarchy", 100);

            ItemPointer[] pointers;
            try {
                request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 5));
                request.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                pointers = this.refactorer.findDefinition(request);
            } finally {
                request.popMonitor().done();
            }

            if (pointers.length == 1) {
                //ok, this is the default one.
                Definition d = pointers[0].definition;
                HierarchyNodeModel model;
                try {
                    request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 5));
                    model = createHierarhyNodeFromClassDef(d);
                } finally {
                    request.popMonitor().done();
                }

                if (model == null) {
                    return null;
                }

                HashMap<HierarchyNodeModel, HierarchyNodeModel> allFound = new HashMap<HierarchyNodeModel, HierarchyNodeModel>();
                allFound.put(model, model);

                try {
                    request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 10));
                    findParents(request.nature, d, model, allFound, request);
                } finally {
                    request.popMonitor().done();
                }

                if (!findOnlyParents) {
                    try {
                        request.pushMonitor(new SubProgressMonitor(request.getMonitor(), 80));
                        findChildren(request, model, allFound);
                    } finally {
                        request.popMonitor().done();
                    }
                }

                return model;
            }

        } catch (OperationCanceledException e) {
            //ignore
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            request.getMonitor().done();
        }
        return null;
    }

    /**
     * @param d
     * @param model
     * @return
     */
    private HierarchyNodeModel createHierarhyNodeFromClassDef(Definition d) {
        HierarchyNodeModel model = null;
        if (d.ast instanceof ClassDef) {
            model = new HierarchyNodeModel(d.module, (ClassDef) d.ast);
        }
        return model;
    }

    public boolean areAllInSameClassHierarchy(List<AssignDefinition> defs) {
        return true;
    }

}
