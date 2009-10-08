/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.printer;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.ISpecialStrOrToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.cmpopType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.operatorType;
import org.python.pydev.parser.jython.ast.str_typeType;
import org.python.pydev.parser.jython.ast.unaryopType;
import org.python.pydev.refactoring.ast.adapters.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

public class SourcePrinter {
    private boolean disabledIfPrinting;
    private final SyntaxHelper syntaxHelper;
    private boolean ignoreComments;
    private final NodeHelper nodeHelper;
    private final PrintWriter output;
    private final CallDepth callDepth;

    public SourcePrinter(PrintWriter output, AdapterPrefs adapterPrefs) {
        this.output = output;
        this.syntaxHelper = new SyntaxHelper(adapterPrefs.endLineDelim);
        this.callDepth = new CallDepth();
        this.ignoreComments = false;

        this.disabledIfPrinting = false;
        this.nodeHelper = new NodeHelper(adapterPrefs);
    }

    public java.util.List<commentType> extractComments(java.util.List<Object> specials) {
        if(specials == null){
            return null;
        }

        java.util.List<commentType> comments = new ArrayList<commentType>();

        for(Object node:specials){
            if(node instanceof commentType){
                commentType comment = (commentType) node;
                comments.add(comment);
            }
        }

        return comments;
    }

    protected java.util.List<commentType> extractComments(java.util.List<Object> specials, SimpleNode firstBodyNode, boolean before) {
        if(specials == null){
            return null;
        }

        java.util.List<commentType> comments = new ArrayList<commentType>();

        for(commentType comment:extractComments(specials)){
            if(firstBodyNode != null){

                if(before){
                    if(comment.beginLine < firstBodyNode.beginLine){
                        comments.add(comment);
                    }
                }else{
                    if(comment.beginLine >= firstBodyNode.beginLine){
                        comments.add(comment);
                    }
                }

            }
        }

        return comments;
    }

    public void flushStream() {
        output.flush();
    }

    public int getStartLine(SimpleNode n) {
        return nodeHelper.getStartLine(n);
    }

    public int getStartOffset(SimpleNode n) {
        return nodeHelper.getStartOffset(n);
    }

    public SyntaxHelper getSyntaxhelper() {
        return syntaxHelper;
    }

    public boolean hasCommentsAfter(SimpleNode node) {
        if(node == null){
            return false;
        }
        if(node.specialsAfter == null){
            return false;
        }

        java.util.List<commentType> comments = extractComments(node.specialsAfter);
        return (comments != null) && (comments.size() > 0);

    }

    public void indent() {
        syntaxHelper.indent();
    }

    public boolean isCommentBefore(SimpleNode node, commentType comment) {
        return getStartLine(comment) < getStartLine(node);
    }

    public boolean isDisabledIfPrinting() {
        return disabledIfPrinting;
    }

    private boolean isIgnoreComments(SimpleNode node) {
        return ignoreComments || (node == null);
    }

    public boolean isPrettyPrint(SimpleNode node) {
        return(nodeHelper.isFunctionOrClassDef(node) || isPrettyPrintControlStatement(node));
    }

    public boolean isPrettyPrintControlStatement(SimpleNode node) {
        return nodeHelper.isControlStatement(node) && !isDisabledIfPrinting();
    }

    public boolean isSameLine(SimpleNode node1, SimpleNode node2) {
        return(getStartLine(node1) == getStartLine(node2));
    }

    public void outdent() {
        syntaxHelper.outdent();
    }

    public void prettyPrintAfter(SimpleNode node, SimpleNode lastNode) {
        if(!inCall() && isPrettyPrint(node) && isPrettyPrint(lastNode)){
            printNewlineAndIndentation();
        }
    }

    public void print(BigInteger i) {
        output.print(i);
    }

    public void print(char c) {
        output.print(c);
    }

    public void print(double d) {
        output.print(d);
    }

    public void print(int i) {
        output.print(i);
    }

    public void print(long l) {
        output.print(l);
    }

    public void print(Object o) {
        output.print(String.valueOf(o));
    }

    public void print(String s) {
        output.print(s);
    }

    public void printAfterDict() {
        print(SyntaxHelper.DICT_CLOSE);
    }

    public void printAfterList() {
        print(SyntaxHelper.LIST_CLOSE);
    }

    public void printAfterTuple() {
        print(SyntaxHelper.PARENTHESE_CLOSE);
    }

    public void printAssignmentOperator(boolean spaceBefore, boolean spaceAfter) {
        if(spaceBefore){
            printSpace();
        }
        print(SyntaxHelper.EQUAL);
        if(spaceAfter){
            printSpace();
        }
    }

    public void printAttributeSeparator() {
        print(SyntaxHelper.DOT);
    }

    public void printBeforeAndAfterCmpOp() {
        printSpace();
    }

    public void printBeforeComment() {
        print(SyntaxHelper.ONE_SPACE);
    }

    public void printBeforeDecorator() {
        print(SyntaxHelper.AT_SYMBOL);

    }

    public void printBeforeDict() {
        print(SyntaxHelper.DICT_OPEN);
    }

    public void printBeforeKwArg() {
        print(syntaxHelper.getStar(2));
    }

    public void printBeforeList() {
        print(SyntaxHelper.LIST_OPEN);
    }

    public void printBeforeTuple() {
        print(SyntaxHelper.PARENTHESE_OPEN);
    }

    public void printBeforeVarArg() {
        print(syntaxHelper.getStar(1));
    }

    public void printBinOp(int opType, boolean spaceBefore, boolean spaceAfter) {
        String op;

        switch(opType){
        case operatorType.Add:
            op = SyntaxHelper.OP_ADD;
            break;
        case operatorType.BitAnd:
            op = SyntaxHelper.OP_BITWISE_AND;
            break;
        case operatorType.BitOr:
            op = SyntaxHelper.OP_BITWISE_OR;
            break;
        case operatorType.BitXor:
            op = SyntaxHelper.OP_BITWISE_XOR;
            break;
        case operatorType.Div:
            op = SyntaxHelper.OP_DIV;
            break;
        case operatorType.FloorDiv:
            op = SyntaxHelper.OP_FLOORDIV;
            break;
        case operatorType.LShift:
            op = SyntaxHelper.OP_LSHIFT;
            break;
        case operatorType.Mod:
            op = SyntaxHelper.OP_MOD;
            break;
        case operatorType.Mult:
            op = SyntaxHelper.STAR;
            break;
        case operatorType.Pow:
            op = SyntaxHelper.OP_POWER;
            break;
        case operatorType.RShift:
            op = SyntaxHelper.OP_RSHIFT;
            break;
        case operatorType.Sub:
            op = SyntaxHelper.OP_SUB;
            break;
        default:
            op = "<undef_binop>";
            break;
        }

        if(spaceBefore){
            printSpace();
        }
        print(op);
        if(spaceAfter){
            printSpace();
        }
    }

    public void printBoolOp(int op) {

        printSpace();
        switch(op){
        case BoolOp.And:
            print(SyntaxHelper.OP_BOOL_AND);
            break;
        case BoolOp.Or:
            print(SyntaxHelper.OP_BOOL_OR);
            break;
        default:
            print("<undef_boolop>");
            break;
        }
        printSpace();

    }

    public void printClassDef() {
        printStatement("class");
    }

    public void printComment(SimpleNode node, java.util.List<commentType> comments) {

        if(comments == null){
            return;
        }

        boolean wasOnSameLine = false;

        for(Iterator<commentType> iter = comments.iterator(); iter.hasNext();){
            commentType comment = iter.next();
            String commentText = comment.id.trim();

            if(isSameLine(node, comment)){
                printBeforeComment();
                wasOnSameLine = true;
            }else{
                printNewlineAndIndentation();
            }

            print(commentText);

            if(isCommentBefore(node, comment) && (!wasOnSameLine)){
                printNewlineAndIndentation();
            }

        }

    }

    public void printCommentAfter(SimpleNode node) {
        if(!(isIgnoreComments(node))){
            if(!(nodeHelper.isControlStatement(node))){
                printComment(node, extractComments(node.specialsAfter));
            }
        }
    }

    public void printCommentBeforeBody(SimpleNode node, SimpleNode firstBodyNode) {
        if(!(isIgnoreComments(node))){
            printComment(node, extractComments(node.specialsAfter, firstBodyNode, true));
        }
    }

    public void printCommentAfterBody(SimpleNode node, SimpleNode firstBodyNode) {
        if(!isIgnoreComments(node)){
            printComment(node, extractComments(node.specialsAfter, firstBodyNode, false));
        }
    }

    public void printCommentBefore(SimpleNode node) {
        if(!isIgnoreComments(node)){
            printComment(node, extractComments(node.specialsBefore));
        }
    }

    public void printCompOp(int opType) {
        switch(opType){
        case cmpopType.Eq:
            print(SyntaxHelper.OP_EQUAL_VALUE);
            break;
        case cmpopType.Gt:
            print(SyntaxHelper.OP_GT);
            break;
        case cmpopType.GtE:
            print(SyntaxHelper.OP_GT_EQUAL);
            break;
        case cmpopType.In:
            print(SyntaxHelper.OP_IN);
            break;
        case cmpopType.Is:
            print(SyntaxHelper.OP_IS);
            break;
        case cmpopType.IsNot:
            print(SyntaxHelper.OP_IS_NOT);
            break;
        case cmpopType.Lt:
            print(SyntaxHelper.OP_LT);
            break;
        case cmpopType.LtE:
            print(SyntaxHelper.OP_LT_EQUAL);
            break;
        case cmpopType.NotEq:
            print(SyntaxHelper.OP_NOT_EQUAL);
            break;
        case cmpopType.NotIn:
            print(SyntaxHelper.OP_NOT_IN);
            break;
        default:
            print("<undef_compop>");
            break;
        }

    }

    public void printContinue() {
        printStatement("continue", false, false);
    }

    public void printDestinationOperator(boolean withSpaceBefore, boolean withSpaceAfter) {
        if(withSpaceBefore){
            printSpace();
        }
        print(SyntaxHelper.OP_RSHIFT);
        if(withSpaceAfter){
            printSpace();
        }
    }

    public void printDictBeforeValue() {
        printFunctionMarker();
        printSpace();
    }

    public void printDoubleDot() {
        print(SyntaxHelper.DOUBLEDOT);

    }

    public void printDoubleDotAndNewline() {
        printDoubleDot();
        printNewlineAndIndentation();
    }

    protected void printDoubleQuote() {
        print(SyntaxHelper.QUOTE_DOUBLE);
    }

    public void printEllipsis() {
        print(SyntaxHelper.ELLIPSIS);
    }

    public void printFunctionDef() {
        printStatement("def");
    }

    public void printFunctionMarker() {
        printDoubleDot();
    }

    public void printFunctionMarkerWithSpace() {
        printFunctionMarker();
        printSpace();
    }

    public void printListSeparator() {
        print(SyntaxHelper.LIST_SEPERATOR);
    }

    public void printNewlineAndIndentation() {
        print(syntaxHelper.defaultLineDelimiter);
        print(syntaxHelper.getAlignment());
    }

    public void printNum(Num node) {
        print(node.num);
    }

    protected void printSingleQuote() {
        print(SyntaxHelper.QUOTE_SINGLE);
    }

    protected void printSpace() {
        print(SyntaxHelper.ONE_SPACE);
    }

    protected void printStatement(String statement) {
        printStatement(statement, false, true);
    }

    protected void printStatement(String statement, boolean spaceBefore, boolean spaceAfter) {
        if(spaceBefore){
            print(SyntaxHelper.ONE_SPACE);
        }
        print(statement);
        if(spaceAfter){
            print(SyntaxHelper.ONE_SPACE);
        }
    }

    public void printStatementAs() {
        printStatement("as", true, true);
    }

    public void printStatementAssert() {
        printStatement("assert");

    }

    public void printStatementBreak() {
        print("break");
    }

    public void printStatementDel() {
        printStatement("del");
    }

    public void printStatementElif() {
        printStatement("elif");

    }

    public void printStatementElse() {
        print("else");
    }

    public void printStatementElseWithSpace() {
        printSpace();
        printStatementElse();
        printSpace();
    }

    public void printStatementExcept(boolean spaceAfter) {
        printStatement("except", false, spaceAfter);
    }

    public void printStatementExec() {
        printStatement("exec");
    }

    public void printStatementFinally() {
        print("finally");
    }

    public void printStatementFor(boolean spaceBefore, boolean spaceAfter) {
        printStatement("for", spaceBefore, spaceAfter);
    }

    public void printStatementFrom() {
        printStatement("from", false, true);
    }

    public void printStatementFromImport() {
        printStatement("import", true, true);
    }

    public void printStatementGlobal() {
        printStatement("global");
    }

    public void printStatementIf(SimpleNode node, boolean spaceBefore, boolean spaceAfter) {
        if(!isDisabledIfPrinting()){
            printStatement("if", spaceBefore, spaceAfter);
        }else{

            if(node != null){
                for(Object o:node.specialsBefore){
                    if(o instanceof ISpecialStrOrToken){
                        ISpecialStrOrToken specialStr = (ISpecialStrOrToken) o;
                        if(specialStr.toString().trim().equals("if")){
                            printStatement("if", spaceBefore, spaceAfter);
                            return;
                        }
                    }
                }
            }

            setDisabledIfPrinting(false);
        }
    }

    public void printStatementImport() {
        printStatement("import");
    }

    public void printStatementIn() {
        printStatement("in", true, true);
    }

    public void printstatementLambda() {
        printStatement("lambda");
    }

    public void printStatementPass() {
        printStatement("pass", false, false);
    }

    public void printStatementPrint() {
        printStatement("print");
    }

    public void printStatementRaise(boolean spaceAfter) {
        printStatement("raise", false, spaceAfter);
    }

    public void printStatementReturn() {
        printStatement("return");
    }

    public void printStatementTry() {
        print("try");
    }

    public void printStatementWhile() {
        printStatement("while");
    }

    public void printStatementWith() {
        printStatement("with");
    }

    public void printStatementYield() {
        printStatement("yield");
    }

    public void printStr(Str node) {
        if(node.unicode){
            print("u");
        }
        // u and r don't exclude themself (but u before r)
        if(node.raw){
            print("r");
        }
        printStrQuote(node);
        print(node.s);
        printStrQuote(node);
    }

    public void printStrQuote(Str node) {
        switch(node.type){
        case (str_typeType.SingleDouble):
            printDoubleQuote();
            break;
        case (str_typeType.SingleSingle):
            printSingleQuote();
            break;
        case (str_typeType.TripleDouble):
            printTripeDoubleQuote();
            break;
        case (str_typeType.TripleSingle):
            printTripeSingleQuote();
            break;
        default:
            throw new RuntimeException("Unknown node");
        }
    }

    public void printTripeDoubleQuote() {
        for(int i = 0; i < 3; i++){
            printDoubleQuote();
        }
    }

    public void printTripeSingleQuote() {
        for(int i = 0; i < 3; i++){
            printSingleQuote();
        }
    }

    public void printUnaryOp(int opType) {
        String op;

        switch(opType){
        case unaryopType.Invert:
            op = SyntaxHelper.OP_UINVERT;
            break;
        case unaryopType.Not:
            op = SyntaxHelper.OP_UNOT;
            break;
        case unaryopType.UAdd:
            op = SyntaxHelper.OP_UADD;
            break;
        case unaryopType.USub:
            op = SyntaxHelper.OP_USUB;
            break;
        default:
            op = "<undef_unaryop>";
            break;
        }

        print(op);
        if(opType == unaryopType.Not){
            printSpace();
        }

    }

    public void setDisabledIfPrinting(boolean disabledIfPrinting) {
        this.disabledIfPrinting = disabledIfPrinting;
    }

    public void setIgnoreComments(boolean ignoreComments) {
        this.ignoreComments = ignoreComments;
    }

    public void printReprQuote() {
        print(SyntaxHelper.REPR_QUOTE);
    }

    public NodeHelper getNodeHelper() {
        return this.nodeHelper;
    }

    public boolean hasSpecialBefore(SimpleNode n, String match) {
        if(n == null){
            return false;
        }
        return checkSpecialStr(n.getSpecialsBefore(), match);
    }

    public boolean hasSpecialAfter(SimpleNode n, String match) {
        if(n == null){
            return false;
        }
        return checkSpecialStr(n.getSpecialsAfter(), match);
    }

    public boolean checkSpecialStr(List<Object> specials, String pattern) {
        for(Object object:specials){
            if(object instanceof ISpecialStrOrToken){
                ISpecialStrOrToken str = (ISpecialStrOrToken) object;
                return(str.toString().compareTo(pattern) == 0);

            }else if(object instanceof String){
                String text = (String) object;
                return(text.compareTo(pattern) == 0);
            }
        }
        return false;
    }

    public void openParentheses(SimpleNode n) {
        if(needsParentheses(n)){
            printBeforeTuple();
        }
    }

    public void closeParentheses(SimpleNode n) {
        if(needsParentheses(n)){
            printAfterTuple();
        }
    }

    protected boolean needsParentheses(SimpleNode n) {
        return(inCall() || hasParentheses(n));
    }

    protected boolean needsBracket(SimpleNode n) {
        return(hasBracket(n) || nodeHelper.isList(n));
    }

    protected boolean hasParentheses(SimpleNode n) {
        return hasSpecialBefore(n, SyntaxHelper.PARENTHESE_OPEN);
    }

    boolean hasBracket(SimpleNode n) {
        return hasSpecialBefore(n, SyntaxHelper.LIST_OPEN);
    }

    public void closeBracket(SimpleNode n) {
        if(needsBracket(n)){
            printAfterList();
        }
    }

    public void openBracket(SimpleNode n) {
        if(needsBracket(n)){
            printBeforeList();
        }
    }

    public CallDepth getCallDepth() {
        return callDepth;
    }

    public void enterCall() {
        callDepth.enterCall();
    }

    public void leaveCall() {
        callDepth.leaveCall();
    }

    public boolean inCall() {
        return callDepth.inCall();
    }
}
