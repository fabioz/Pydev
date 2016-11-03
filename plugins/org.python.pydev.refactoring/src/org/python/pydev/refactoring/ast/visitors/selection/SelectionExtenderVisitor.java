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
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.selection;

import java.util.List;

import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.parser.jython.ISpecialStr;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.shared_core.structure.FastStack;

public class SelectionExtenderVisitor extends VisitorBase {

    private ModuleAdapter module;

    private ITextSelection selection;

    private FastStack<SimpleNode> stmtExprStack;

    private SimpleNode extendNodeInSelection;

    public SelectionExtenderVisitor(ModuleAdapter module, ITextSelection selection) {
        this.module = module;
        this.selection = selection;
        this.stmtExprStack = new FastStack<SimpleNode>(20);
        this.extendNodeInSelection = null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        if (node != null) {
            updateSelection(node);
            node.traverse(this);
        }
    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    private void updateSelection(SimpleNode node) {
        extendSelection(node);
        if (node.beginLine <= selection.getEndLine() + 1) {
            updateStack(node);
            checkAndExtend(node, node);
        } else if (node.beginLine >= selection.getEndLine() + 1) {
            if (!stmtExprStack.isEmpty()) {
                this.extendNodeInSelection = stmtExprStack.peek();
            }
        }
    }

    protected SimpleNode visit(SimpleNode node) throws Exception {
        if (node == null) {
            return null;
        }

        if (!(node instanceof suiteType)) {
            updateSelection(node);
        }

        if (node instanceof suiteType) {
            visitSuiteType((suiteType) node);
        } else if (node instanceof excepthandlerType) {
            visitExceptionHandler((excepthandlerType) node);
        } else if (node instanceof decoratorsType) {
            visitDecoratorsType((decoratorsType) node);
        } else if (node instanceof keywordType) {
            visitKeywordType((keywordType) node);
        } else if (node instanceof argumentsType) {
            visitArgumentsType((argumentsType) node);
        } else if (node instanceof aliasType) {
            visitAliasType((aliasType) node);
        } else {
            node.accept(this);
        }

        if (isAnyInSelection(node.getSpecialsBefore()) || isAnyInSelection(node.getSpecialsAfter())) {
            this.extendNodeInSelection = node;
        }

        return node;
    }

    private boolean isAnyInSelection(List<Object> specials) {
        for (Object o : specials) {
            Str strNode = convertSpecialToStr(o);
            if (strNode != null) {
                if (module.isNodeInSelection(selection, strNode)) {
                    return true;
                }
            }

        }
        return false;
    }

    private Str convertSpecialToStr(Object o) {
        Str stringNode = null;
        if (o instanceof ISpecialStr) {
            ISpecialStr special = (ISpecialStr) o;
            stringNode = new Str(special.toString(), Str.SingleDouble, false, false, false, false);
            stringNode.beginLine = special.getBeginLine();
            stringNode.beginColumn = special.getBeginCol();
        }
        return stringNode;
    }

    private void extendSelection(SimpleNode node) {
        if (extendNodeInSelection != null && isExtendable(node) && node != extendNodeInSelection) {

            node = resolveExtendNode(node);

            this.selection = this.module.extendSelection(selection, extendNodeInSelection, node);
            this.extendNodeInSelection = null;
            this.stmtExprStack.clear();
        }
    }

    private boolean isExtendable(SimpleNode node) {
        return (node instanceof stmtType || node instanceof excepthandlerType || node instanceof suiteType);
    }

    private SimpleNode resolveExtendNode(SimpleNode node) {
        if (extendNodeInSelection instanceof exprType) {
            while (!(stmtExprStack.isEmpty()) && !(isExtendable(stmtExprStack.peek()))) {
                stmtExprStack.pop();
            }
            if (!(stmtExprStack.isEmpty())) {
                SimpleNode stmtBefore = stmtExprStack.peek();
                node = checkSpecials(stmtBefore, stmtBefore.getSpecialsAfter());
            }
        }
        node = checkSpecials(node, node.getSpecialsBefore());

        return node;
    }

    private SimpleNode checkSpecials(SimpleNode node, List<Object> specials) {
        if (specials.size() > 0) {
            for (Object o : specials) {
                if (o instanceof ISpecialStr) {
                    ISpecialStr str = (ISpecialStr) o;
                    if (str.getBeginLine() >= extendNodeInSelection.beginLine) {
                        return convertSpecialToStr(o);
                    }
                }
            }
        }
        return node;
    }

    private void checkAndExtend(SimpleNode extendNode, SimpleNode checkNode) {
        if (module.isNodeInSelection(selection, checkNode)) {
            extendNodeInSelection = extendNode;
        }
    }

    @Override
    public Object visitCall(Call node) throws Exception {
        traverse(node);
        checkAndExtend(node, node.func);

        return null;

    }

    @Override
    public Object visitAttribute(Attribute node) throws Exception {
        visit(node.value);
        visit(node.attr);
        checkAndExtend(node, node.value);
        checkAndExtend(node, node.attr);
        return null;
    }

    @Override
    public Object visitFor(For node) throws Exception {
        visit(node.target);
        visit(node.iter);
        visit(node.body);
        visit(node.orelse);
        return null;
    }

    @Override
    public Object visitWhile(While node) throws Exception {
        visit(node.test);
        visit(node.body);
        visit(node.orelse);
        return null;
    }

    @Override
    public Object visitIf(If node) throws Exception {
        visit(node.test);
        visit(node.body);
        if (node.orelse != null) {
            if (this.extendNodeInSelection instanceof exprType) {
                extendSelection(node);
            }
            visit(node.orelse);
        }

        return null;
    }

    @Override
    public Object visitTryExcept(TryExcept node) throws Exception {
        visit(node.body);
        visit(node.handlers);
        visit(node.orelse);
        if (node.orelse != null) {
            if (this.extendNodeInSelection instanceof exprType) {
                extendSelection(node);
            }
            visit(node.orelse);
        }
        return null;
    }

    private Object visitSuiteType(suiteType node) throws Exception {
        visit(node.body);
        return null;
    }

    private Object visitExceptionHandler(excepthandlerType node) throws Exception {
        visit(node.type);
        visit(node.name);
        visit(node.body);
        return null;
    }

    private Object visitDecoratorsType(decoratorsType node) throws Exception {
        visit(node.func);
        visit(node.args);
        visit(node.keywords);
        visit(node.starargs);
        visit(node.kwargs);
        return null;
    }

    private Object visitKeywordType(keywordType node) throws Exception {
        visit(node.arg);
        visit(node.value);
        return null;
    }

    private Object visitArgumentsType(argumentsType node) throws Exception {
        visit(node.args);
        visit(node.defaults);
        visit(node.vararg);
        visit(node.kwarg);
        return null;
    }

    private Object visitAliasType(aliasType node) throws Exception {
        visit(node.name);
        visit(node.asname);
        return null;
    }

    @Override
    public Object visitFunctionDef(FunctionDef node) throws Exception {
        visit(node.name);
        visit(node.args);
        visit(node.body);
        extendLast(node);

        return null;
    }

    @Override
    public Object visitClassDef(ClassDef node) throws Exception {
        visit(node.name);
        visit(node.bases);
        visit(node.body);
        extendLast(node);

        return null;
    }

    @Override
    public Object visitModule(Module node) throws Exception {
        visit(node.body);
        extendLast(node);

        return null;
    }

    private void extendLast(SimpleNode node) {
        if (extendNodeInSelection != null) {
            selection = module.extendSelectionToEnd(selection, node);
        }
        if (!stmtExprStack.isEmpty()) {
            selection = module.extendSelection(selection, stmtExprStack.peek());
        }
    }

    private void visit(SimpleNode[] body) throws Exception {
        for (SimpleNode node : body) {
            visit(node);
        }
    }

    private void updateStack(SimpleNode node) {
        if (!stmtExprStack.isEmpty()) {
            if (node == stmtExprStack.peek()) {
                return;
            }
        }
        if (node instanceof stmtType || node instanceof exprType) {
            stmtExprStack.push(node);
        }
    }

    public ITextSelection getSelection() {
        return selection;
    }

}
