/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.javacodecompletion;

import java.util.ArrayList;
import java.util.List;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Expr;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.parser.ast.VisitorBase;
import org.python.parser.ast.stmtType;
import org.python.pydev.utils.REF;

/**
 * This class visits only the global context. Other visitors should visit contexts inside of this one.
 * 
 * @author Fabio Zadrozny
 */
public class GlobalModelVisitor extends VisitorBase {

    public static final int GLOBAL_TOKENS = 1;

    public static final int WILD_MODULES = 2;

    public static final int ALIAS_MODULES = 3;

    private int visitWhat;

    public List tokens = new ArrayList();


    /**
     * @param global_tokens2
     */
    public GlobalModelVisitor(int visitWhat) {
        this.visitWhat = visitWhat;
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
    }

    
    /**
     * @param node
     * @param t
     */
    public static String getNodeDocString(SimpleNode node) {
        //and check if it has a docstring.
        if (REF.hasAttr(node, "body")) {

            Object obj = REF.getAttr(node, "body");
            if (obj instanceof stmtType[]) {
                stmtType body[] = (stmtType[]) obj;
                if (body.length > 0) {
                    if (body[0] instanceof Expr) {
                        Expr e = (Expr) body[0];
                        if (e.value instanceof Str) {
                            Str s = (Str) e.value;
                            return s.s;
                        }
                    }
                }
            }
        }
        return "";
    }
    
    
    /**
     * Adds a token with a docstring.
     * 
     * @param node
     */
    private void addToken(SimpleNode node) {
        //add the token
        Token t = new Token(node);
        t.docStr = getNodeDocString(node);
        this.tokens.add(t);
    }

    public Object visitClassDef(ClassDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (this.visitWhat == GLOBAL_TOKENS) {
            addToken(node);
        } 
        return null;
    }

    public Object visitFunctionDef(FunctionDef node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (this.visitWhat == GLOBAL_TOKENS) {
            addToken(node);
        }
        return null;
    }

    /**
     * Name should be whithin assign.
     * 
     * @see org.python.parser.ast.VisitorIF#visitAssign(org.python.parser.ast.Assign)
     */
    public Object visitAssign(Assign node) throws Exception {
        node.traverse(this);
        return null;
    }

    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (this.visitWhat == GLOBAL_TOKENS) {
            if (node.ctx == Name.Store) {
                this.tokens.add(new Token(node));
            }
        }
        return null;
    }

    /**
     * @see org.python.parser.ast.VisitorBase#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if (this.visitWhat == WILD_MODULES) {
            if (node.names.length == 0) {
                this.tokens.add(new Token(node));
            }
        } else if (this.visitWhat == ALIAS_MODULES) {
            if (node.names.length > 0) {
                this.tokens.add(new Token(node));
            }
        }
        return null;
    }
    
    /**
     * @see org.python.parser.ast.VisitorBase#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        if (this.visitWhat == ALIAS_MODULES) {
            if (node.names.length > 0) {
                this.tokens.add(new Token(node));
            }
        }
        return null;
    }
}