/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.CompletionCache;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.editor.codecompletion.revisited.visitors.KeywordParameterDefinition;
import org.python.pydev.editor.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;
import com.python.pydev.refactoring.wizards.rename.visitors.FindCallVisitor;

/**
 * The rename parameter is based on the rename function, because it will basically:
 * 1- get the  function definition 
 * 2- get all the references 
 * 
 * 3- change the parameter in the function definition (as well as any references to the
 * parameter inside the function
 * 4- change the parameter in all function calls
 * 
 * 
 * This process will only be available if we can find the function definition
 * (otherwise, we'd have a standard rename any local here)
 * 
 * @author fabioz
 */
public class PyRenameParameterProcess extends PyRenameFunctionProcess {

    private String functionName;

    /**
     * Used if we're in a call and cannot find the definition for the method of that call when we've a KeywordParameterDefinition.
     * If that's not the case, it's null.
     */
    private ASTEntry singleEntry;

    public PyRenameParameterProcess(KeywordParameterDefinition definition, IPythonNature nature) {
        Assert.isNotNull(definition.scope, "The scope for a rename parameter must always be provided.");

        String tok = NodeUtils.getFullRepresentationString(definition.call);
        int line = definition.call.beginLine;
        int col = definition.call.beginColumn;

        IDefinition[] definitions;
        try {
            definitions = definition.module.findDefinition(CompletionStateFactory.getEmptyCompletionState(tok, nature,
                    line - 1, col - 1, new CompletionCache()), line, col, nature);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (definitions != null && definitions.length > 0) {
            init((Definition) definitions[0]);
        } else {
            singleEntry = new ASTEntry(null);
            singleEntry.node = definition.ast;
        }
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        if (singleEntry == null) {
            super.findReferencesToRenameOnLocalScope(request, status);
        } else {
            docOccurrences.add(singleEntry);
        }
    }

    @Override
    protected void findReferencesToRenameOnWorkspace(RefactoringRequest request, RefactoringStatus status) {
        if (singleEntry == null) {
            super.findReferencesToRenameOnWorkspace(request, status);
        } else {
            docOccurrences.add(singleEntry);
        }
    }

    public PyRenameParameterProcess(Definition definition) {
        //empty, because we'll actually supply a different definition for the superclass (the method 
        //definition, and not the parameter, which we receive here).

        init(definition);
    }

    private void init(Definition definition) {
        Assert.isNotNull(definition.scope, "The scope for a rename parameter must always be provided.");

        FunctionDef node;
        if (definition.ast instanceof FunctionDef) {
            node = (FunctionDef) definition.ast;
        } else {
            node = (FunctionDef) definition.scope.getScopeStack().peek();
        }
        super.definition = new Definition(node.beginLine, node.beginColumn, ((NameTok) node.name).id, node,
                definition.scope, definition.module);
        this.functionName = ((NameTok) node.name).id;
    }

    /**
     * These are the methods that we need to override to change the function occurrences for parameter occurrences
     */
    @Override
    protected List<ASTEntry> getEntryOccurrencesInSameModule(RefactoringStatus status, String initialName,
            SimpleNode root) {
        List<ASTEntry> occurrences = super.getEntryOccurrencesInSameModule(status, this.functionName, root);
        return getParameterOccurences(occurrences, root);
    }

    @Override
    protected List<ASTEntry> getOccurrencesInOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName,
            SourceModule module, PythonNature nature) {
        List<ASTEntry> occurrences = super.getOccurrencesInOtherModule(status, request, this.functionName, module,
                nature);
        return getParameterOccurences(occurrences, module.getAst());

    }

    /**
     * This method changes function occurrences for parameter occurrences
     */
    private List<ASTEntry> getParameterOccurences(List<ASTEntry> occurrences, SimpleNode root) {
        List<ASTEntry> ret = new ArrayList<ASTEntry>();
        List<Tuple<Integer, Integer>> acceptedCommentRanges = new ArrayList<Tuple<Integer, Integer>>();
        for (ASTEntry entry : occurrences) {

            if (entry.node instanceof Name) {
                Name name = (Name) entry.node;
                if (name.ctx == Name.Artificial) {
                    continue;
                }
            }
            if (entry.parent != null && entry.parent.node instanceof FunctionDef && entry.node instanceof NameTok
                    && ((NameTok) entry.node).ctx == NameTok.FunctionName) {
                //process a function definition (get the parameters with the given name and
                //references inside that function)
                processFunctionDef(ret, entry);
                ASTEntry parent = entry.parent;
                acceptedCommentRanges.add(new Tuple<Integer, Integer>(parent.node.beginLine, parent.endLine));

            } else if (entry.node instanceof Name) {
                processFoundName(root, ret, (Name) entry.node);

            } else if (entry.node instanceof NameTok) {
                processFoundNameTok(root, ret, (NameTok) entry.node);

            }
        }
        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            List<ASTEntry> commentOccurrences = ScopeAnalysis.getCommentOccurrences(request.initialName, root);
            for (ASTEntry commentOccurrence : commentOccurrences) {
                for (Tuple<Integer, Integer> range : acceptedCommentRanges) {
                    if (commentOccurrence.node.beginLine >= range.o1 && commentOccurrence.node.beginLine <= range.o2) {
                        ret.add(commentOccurrence);
                    }
                }
            }
        }
        return ret;
    }

    private void processFunctionDef(List<ASTEntry> ret, ASTEntry entry) {
        //this is the actual function definition, so, let's take a look at its arguments... 

        FunctionDef node = (FunctionDef) entry.parent.node;
        List<ASTEntry> found = ScopeAnalysis.getLocalOccurrences(request.initialName, node);
        ret.addAll(ScopeAnalysis.getStringOccurrences(request.initialName, node));
        ret.addAll(found);
    }

    private void processFoundNameTok(SimpleNode root, List<ASTEntry> ret, NameTok name) {
        if (name.ctx == NameTok.Attrib) {
            Call call = FindCallVisitor.findCall(name, root);
            processCall(ret, call);
        }
    }

    private void processFoundName(SimpleNode root, List<ASTEntry> ret, Name name) {
        if (name.ctx == Name.Load) {
            Call call = FindCallVisitor.findCall(name, root);
            processCall(ret, call);
        }
    }

    private void processCall(List<ASTEntry> ret, Call call) {
        if (call == null) {
            return;
        }
        List<ASTEntry> found = ScopeAnalysis.getLocalOccurrences(request.initialName, call);
        for (ASTEntry entry2 : found) {
            if (entry2.node instanceof NameTok) {
                NameTok name2 = (NameTok) entry2.node;
                if (name2.ctx == NameTok.KeywordName) {
                    ret.add(entry2);
                }
            }
        }
    }

}
