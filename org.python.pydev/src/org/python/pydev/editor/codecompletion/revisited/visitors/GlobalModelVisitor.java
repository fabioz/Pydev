/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited.visitors;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Assign;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

/**
 * This class visits only the global context. Other visitors should visit contexts inside of this one.
 * 
 * @author Fabio Zadrozny
 */
public class GlobalModelVisitor extends AbstractVisitor {

    private int visitWhat;

    /**
     * @param moduleName
     * @param global_tokens2
     */
    GlobalModelVisitor(int visitWhat, String moduleName) {
        this.visitWhat = visitWhat;
        this.moduleName = moduleName;
    }

    protected Object unhandled_node(SimpleNode node) throws Exception {
        return null;
    }

    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
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

    /**
     * Visiting some name
     * 
     * @see org.python.parser.ast.VisitorIF#visitName(org.python.parser.ast.Name)
     */
    public Object visitName(Name node) throws Exception {
        //when visiting the global namespace, we don't go into any inner scope.
        if (this.visitWhat == GLOBAL_TOKENS) {
            if (node.ctx == Name.Store) {
                addToken(node);
            }
        }
        return null;
    }

    /**
     * Visiting some import from
     * @see org.python.parser.ast.VisitorBase#visitImportFrom(org.python.parser.ast.ImportFrom)
     */
    public Object visitImportFrom(ImportFrom node) throws Exception {
        if (this.visitWhat == WILD_MODULES) {
            makeWildImportToken(node, this.tokens, moduleName);
        } else if (this.visitWhat == ALIAS_MODULES) {
            makeImportToken(node, this.tokens, moduleName);
        }
        return null;
    }



    /**
     * Visiting some import
     * @see org.python.parser.ast.VisitorBase#visitImport(org.python.parser.ast.Import)
     */
    public Object visitImport(Import node) throws Exception {
        if (this.visitWhat == ALIAS_MODULES) {
            makeImportToken(node, this.tokens, moduleName);
        }
        return null;
    }

    
    /**
     * @see org.python.parser.ast.VisitorBase#visitStr(org.python.parser.ast.Str)
     */
    public Object visitStr(Str node) throws Exception {
        if(this.visitWhat == MODULE_DOCSTRING){
            this.tokens.add(new SourceToken(node, node.s, "", "", moduleName));
        }
        return null;
    }
}