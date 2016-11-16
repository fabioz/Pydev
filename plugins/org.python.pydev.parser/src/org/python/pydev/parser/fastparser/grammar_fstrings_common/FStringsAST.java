package org.python.pydev.parser.fastparser.grammar_fstrings_common;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammarTreeConstants;
import org.python.pydev.shared_core.string.TextSelectionUtils;

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

    public static class FStringExpressionContent {

        public final String string;
        //0-based
        public final int startOffset;
        //0-based
        public final int endOffset;

        // 1-based
        public final int beginLine;
        // 1-based
        public final int beginColumn;
        // 1-based
        public final int endLine;
        // 1-based
        public final int endColumn;

        public FStringExpressionContent(String string, int startOffset, int endOffset, int beginLine, int beginColumn,
                int endLine, int endColumn) {
            this.string = string;
            this.startOffset = startOffset;
            this.endOffset = endOffset;
            this.beginLine = beginLine;
            this.beginColumn = beginColumn;
            this.endLine = endLine;
            this.endColumn = endColumn;
        }

    }

    public Iterable<FStringExpressionContent> getFStringExpressionsContent(IDocument doc) {
        Iterable<SimpleNode> fStringExpressions = this.getBalancedExpressions();
        ArrayList<FStringExpressionContent> lst = new ArrayList<>(this.rootNode.jjtGetNumChildren());
        for (SimpleNode simpleNode : fStringExpressions) {
            int startOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc, simpleNode.beginLine - 1,
                    simpleNode.beginColumn - 1);
            int endOffset = TextSelectionUtils.getAbsoluteCursorOffset(doc, simpleNode.endLine - 1,
                    simpleNode.endColumn);
            try {
                lst.add(new FStringExpressionContent(doc.get(startOffset, endOffset - startOffset), startOffset,
                        endOffset, simpleNode.beginLine, simpleNode.beginColumn, simpleNode.endLine,
                        simpleNode.endColumn));
            } catch (BadLocationException e) {
                Log.log(e);
            }
        }
        return lst;
    }

}
