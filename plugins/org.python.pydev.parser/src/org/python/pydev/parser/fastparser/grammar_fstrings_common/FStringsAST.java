package org.python.pydev.parser.fastparser.grammar_fstrings_common;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.grammar_fstrings.FStringsGrammarTreeConstants;

public class FStringsAST {

    private SimpleNode rootNode;

    public FStringsAST(SimpleNode rootNode) {
        this.rootNode = rootNode;
    }

    public void dump() {
        this.rootNode.dump("");
    }

    public boolean hasChildren() {
        return this.rootNode != null && this.rootNode.jjtGetNumChildren() > 0;
    }

    public Iterable<SimpleNode> childNodesOfId(int id) {
        SimpleNode node = this.rootNode;
        return childNodesOfId(node, id);
    }

    public static Iterable<SimpleNode> childNodesOfId(SimpleNode node, int id) {
        int nChildren = node.jjtGetNumChildren();
        List<SimpleNode> arrayList = new ArrayList<>(nChildren);
        for (int i = 0; i < nChildren; i++) {
            SimpleNode n = (SimpleNode) node.jjtGetChild(i);
            if (n.id == id) {
                arrayList.add(n);
            }
        }
        return arrayList;
    }

    public Iterable<SimpleNode> getFStringExpressions() {
        return childNodesOfId(FStringsGrammarTreeConstants.JJTF_STRING_EXPR);
    }

    public Iterable<SimpleNode> getBalancedExpressions() {
        List<SimpleNode> ret = new ArrayList<>(this.rootNode.jjtGetNumChildren());
        for (SimpleNode n : childNodesOfId(FStringsGrammarTreeConstants.JJTF_STRING_EXPR)) {
            for (SimpleNode n2 : childNodesOfId(n, FStringsGrammarTreeConstants.JJTBALANCED_EXPRESSION_TEXT)) {
                ret.add(n2);
            }
        }
        return ret;
    }

}
