/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial implementation
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.coderefactoring.extractlocal.edit;

import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.expr_contextType;
import org.python.pydev.parser.visitors.scope.GetNodeForExtractLocalVisitor;
import org.python.pydev.refactoring.coderefactoring.extractlocal.request.ExtractLocalRequest;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.core.edit.AbstractInsertEdit;
import org.python.pydev.shared_core.structure.FastStack;
import org.python.pydev.shared_core.structure.Tuple;

public class CreateLocalVariableEdit extends AbstractInsertEdit {

    private RefactoringInfo info;

    private String variableName;

    private exprType expression;

    private int lineForLocal = -1;

    private boolean replaceDuplicates;

    private List<Tuple<ITextSelection, SimpleNode>> duplicates;

    public CreateLocalVariableEdit(ExtractLocalRequest req) {
        super(req);
        this.info = req.info;
        this.variableName = req.variableName;
        this.expression = (exprType) req.expression.createCopy();
        replaceDuplicates = req.replaceDuplicates;
        duplicates = req.duplicates;
    }

    @Override
    protected SimpleNode getEditNode() {
        exprType variable = new Name(variableName, expr_contextType.Store, false);
        exprType[] target = { variable };

        return new Assign(target, expression, null);
    }

    private int calculateLineForLocal() {
        if (lineForLocal == -1) {
            ITextSelection userSelection = info.getUserSelection();
            if (replaceDuplicates) {
                //When replacing duplicates, we have to consider the selection the first
                //replace (so that the local created works for all the replaces).
                for (Tuple<ITextSelection, SimpleNode> dup : duplicates) {
                    if (dup.o1.getStartLine() < userSelection.getStartLine()) {
                        userSelection = dup.o1;
                    }
                }
            }
            PySelection selection = new PySelection(info.getDocument(), userSelection);
            int startLineIndexIndocCoords = selection.getStartLineIndex();
            int startLineIndexInASTCoords = startLineIndexIndocCoords + 1; //from doc to ast
            Module module = info.getModuleAdapter().getASTNode();
            SimpleNode currentScope = module;

            try {
                FindScopeVisitor scopeVisitor = new FindScopeVisitor(startLineIndexInASTCoords,
                        selection.getCursorColumn() + 1, info.getNature());
                module.accept(scopeVisitor);
                ILocalScope scope = scopeVisitor.scope;
                FastStack scopeStack = scope.getScopeStack();
                currentScope = (SimpleNode) scopeStack.peek(); //at least the module should be there if we don't have anything.
            } catch (Exception e1) {
                Log.log(e1);
            }

            GetNodeForExtractLocalVisitor visitor = new GetNodeForExtractLocalVisitor(startLineIndexInASTCoords);
            try {
                currentScope.accept(visitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            SimpleNode lastNodeBeforePassedLine = visitor.getLastInContextBeforePassedLine();
            if (lastNodeBeforePassedLine != null) {
                final int[] line = new int[] { Integer.MAX_VALUE };
                try {
                    Visitor v = new Visitor() {

                        @Override
                        protected Object unhandled_node(SimpleNode node) throws Exception {
                            if (node.beginLine > 0) {
                                line[0] = Math.min(line[0], node.beginLine - 1);
                            } else {
                                Log.log("Found node with beginLine <= 0:" + node + " line: " + node.beginLine);
                            }
                            return this;
                        }
                    };
                    lastNodeBeforePassedLine.accept(v);
                } catch (Exception e) {
                    Log.log(e);
                }
                if (line[0] != Integer.MAX_VALUE) {
                    lineForLocal = line[0];
                } else {
                    lineForLocal = lastNodeBeforePassedLine.beginLine - 1;
                }
            } else {
                lineForLocal = startLineIndexIndocCoords;
            }

            //The above should give us the proper location, but let's make sure it's NEVER after the current
            //location!
            if (lineForLocal > startLineIndexIndocCoords) {
                lineForLocal = startLineIndexIndocCoords;
            }
        }
        return lineForLocal;
    }

    @Override
    public int getOffset() {
        PySelection selection = new PySelection(info.getDocument(), calculateLineForLocal(), 0);
        return selection.getStartLineOffset();
    }

    @Override
    public int getOffsetStrategy() {
        return 0;
    }

    @Override
    public String getIndent() {
        PySelection selection = new PySelection(info.getDocument(), calculateLineForLocal(), 0);
        return selection.getIndentationFromLine();
    }

}
