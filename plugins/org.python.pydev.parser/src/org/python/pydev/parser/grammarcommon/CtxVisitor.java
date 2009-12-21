package org.python.pydev.parser.grammarcommon;

import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Visitor;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.Starred;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.expr_contextType;

public final class CtxVisitor extends Visitor {
    
    private int ctx;

    public CtxVisitor() { }

    public void setParam(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Param;
        visit(node);
    }
    
    public void setKwOnlyParam(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.KwOnlyParam;
        visit(node);
    }

    public void setStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Store;
        visit(node);
    }

    public void setStore(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setStore(nodes[i]);
    }

    public void setDelete(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.Del;
        visit(node);
    }

    public void setDelete(SimpleNode[] nodes) throws Exception {
        for (int i = 0; i < nodes.length; i++) 
            setDelete(nodes[i]);
    }

    public void setAugStore(SimpleNode node) throws Exception {
        this.ctx = expr_contextType.AugStore;
        visit(node);
    }

    public Object visitName(Name node) throws Exception {
        if(ctx == expr_contextType.Store){
            if(node.reserved){
                throw new ParseException(StringUtils.format("Cannot assign value to %s (because it's a keyword)", node.id), node);
            }
        }
        node.ctx = ctx;
        return null;
    }

    public Object visitStarred(Starred node) throws Exception {
        node.ctx = ctx;
        traverse(node);
        return null;
    }
    
    public Object visitAttribute(Attribute node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitSubscript(Subscript node) throws Exception {
        node.ctx = ctx;
        return null;
    }

    public Object visitList(List node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to list not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitTuple(Tuple node) throws Exception {
        if (ctx == expr_contextType.AugStore) {
            throw new ParseException(
                    "augmented assign to tuple not possible", node);
        }
        node.ctx = ctx;
        traverse(node);
        return null;
    }

    public Object visitCall(Call node) throws Exception {
        throw new ParseException("can't assign to function call", node);
    }

    public Object visitListComp(Call node) throws Exception {
        throw new ParseException("can't assign to list comprehension call",
                                 node);
    }

    public Object unhandled_node(SimpleNode node) throws Exception {
        throw new ParseException("can't assign to operator:"+node, node);
    }
}
