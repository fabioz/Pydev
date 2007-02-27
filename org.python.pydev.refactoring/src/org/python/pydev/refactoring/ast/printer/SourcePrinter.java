package org.python.pydev.refactoring.ast.printer;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.cmpopType;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.operatorType;
import org.python.pydev.parser.jython.ast.str_typeType;
import org.python.pydev.parser.jython.ast.unaryopType;
import org.python.pydev.refactoring.ast.visitors.NodeHelper;

public class SourcePrinter {

	private boolean disabledIfPrinting;

	private final SyntaxHelper syntaxHelper;

	private boolean ignoreComments;

	private final NodeHelper nodeHelper;

	private final PrintWriter output;

	private final CallDepth callDepth;

	public SourcePrinter(PrintWriter output) {
		this(output, new SyntaxHelper(), new CallDepth(), false);
	}

	public SourcePrinter(PrintWriter output, SyntaxHelper formatHelper, CallDepth callDepth, boolean ignoreComments) {
		this.output = output;
		this.syntaxHelper = formatHelper;
		this.callDepth = callDepth;
		this.ignoreComments = ignoreComments;

		this.disabledIfPrinting = false;
		this.nodeHelper = new NodeHelper();

	}

	protected java.util.List<commentType> extractComments(java.util.List<Object> specials) {
		if (specials == null)
			return null;

		java.util.List<commentType> comments = new ArrayList<commentType>();

		for (Object node : specials) {
			if (node instanceof commentType) {
				commentType comment = (commentType) node;
				comments.add(comment);
			}
		}

		return comments;
	}

	protected java.util.List<commentType> extractComments(java.util.List<Object> specials, SimpleNode firstBodyNode, boolean before) {
		if (specials == null)
			return null;

		java.util.List<commentType> comments = new ArrayList<commentType>();

		for (commentType comment : extractComments(specials)) {
			if (firstBodyNode != null) {

				if (before) {
					if (comment.beginLine < firstBodyNode.beginLine)
						comments.add(comment);
				} else {
					if (comment.beginLine >= firstBodyNode.beginLine)
						comments.add(comment);
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
		if (node == null)
			return false;
		if (node.specialsAfter == null)
			return false;

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
		return (nodeHelper.isFunctionOrClassDef(node) || isPrettyPrintControlStatement(node));
	}

	public boolean isPrettyPrintControlStatement(SimpleNode node) {
		return nodeHelper.isControlStatement(node) && !isDisabledIfPrinting();
	}

	public boolean isSameLine(SimpleNode node1, SimpleNode node2) {
		return (getStartLine(node1) == getStartLine(node2));
	}

	public void outdent() {
		syntaxHelper.outdent();
	}

	public void prettyPrintAfter(SimpleNode node, SimpleNode lastNode) {
		if (!inCall() && isPrettyPrint(node) && isPrettyPrint(lastNode)) {
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
		print(syntaxHelper.afterDict());
	}

	public void printAfterList() {
		print(syntaxHelper.afterList());
	}

	public void printAfterTuple() {
		print(syntaxHelper.afterTuple());
	}

	public void printAssignmentOperator(boolean spaceBefore, boolean spaceAfter) {
		if (spaceBefore)
			printSpace();
		print(syntaxHelper.getOperatorAssignment());
		if (spaceAfter)
			printSpace();
	}

	public void printAttributeSeparator() {
		print(syntaxHelper.getAttributeSeparator());
	}

	public void printBeforeAndAfterCmpOp() {
		printSpace();
	}

	public void printBeforeComment() {
		print(syntaxHelper.afterStatement());
	}

	public void printBeforeDecorator() {
		print(syntaxHelper.getAtSymbol());

	}

	public void printBeforeDict() {
		print(syntaxHelper.beforeDict());
	}

	public void printBeforeKwArg() {
		print(syntaxHelper.getStar(2));
	}

	public void printBeforeList() {
		print(syntaxHelper.beforeList());
	}

	public void printBeforeTuple() {
		print(syntaxHelper.beforeTuple());
	}

	public void printBeforeVarArg() {
		print(syntaxHelper.getStar(1));
	}

	public void printBinOp(int opType, boolean spaceBefore, boolean spaceAfter) {
		String op;

		switch (opType) {
		case operatorType.Add:
			op = syntaxHelper.getOperatorAdd();
			break;
		case operatorType.BitAnd:
			op = syntaxHelper.getOperatorBitAnd();
			break;
		case operatorType.BitOr:
			op = syntaxHelper.getOperatorBitOr();
			break;
		case operatorType.BitXor:
			op = syntaxHelper.getOperatorBitXor();
			break;
		case operatorType.Div:
			op = syntaxHelper.getOperatorDiv();
			break;
		case operatorType.FloorDiv:
			op = syntaxHelper.getOperatorFloorDiv();
			break;
		case operatorType.LShift:
			op = syntaxHelper.getOperatorShiftLeft();
			break;
		case operatorType.Mod:
			op = syntaxHelper.getOperatorMod();
			break;
		case operatorType.Mult:
			op = syntaxHelper.getOperatorMult();
			break;
		case operatorType.Pow:
			op = syntaxHelper.getOperatorPow();
			break;
		case operatorType.RShift:
			op = syntaxHelper.getOperatorShiftRight();
			break;
		case operatorType.Sub:
			op = syntaxHelper.getOperatorSub();
			break;
		default:
			op = "<undef_binop>";
			break;
		}

		if (spaceBefore)
			printSpace();
		print(op);
		if (spaceAfter)
			printSpace();
	}

	public void printBoolOp(int op) {

		printSpace();
		switch (op) {
		case BoolOp.And:
			print(syntaxHelper.getOperatorBoolAnd());
			break;
		case BoolOp.Or:
			print(syntaxHelper.getOperatorBoolOr());
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

	protected void printComment(SimpleNode node, java.util.List<commentType> comments) {

		if (comments == null)
			return;

		boolean wasOnSameLine = false;

		for (Iterator<commentType> iter = comments.iterator(); iter.hasNext();) {
			commentType comment = iter.next();
			String commentText = comment.id.trim();

			if (isSameLine(node, comment)) {
				printBeforeComment();
				wasOnSameLine = true;
			} else {
				printNewlineAndIndentation();
			}

			print(commentText);

			if (isCommentBefore(node, comment) && (!wasOnSameLine)) {
				printNewlineAndIndentation();
			}

		}

	}

	public void printCommentAfter(SimpleNode node) {
		if (!(isIgnoreComments(node))) {
			if (!(nodeHelper.isControlStatement(node)))
				printComment(node, extractComments(node.specialsAfter));
		}
	}

	public void printCommentBeforeBody(SimpleNode node, SimpleNode firstBodyNode) {
		if (!(isIgnoreComments(node)))
			printComment(node, extractComments(node.specialsAfter, firstBodyNode, true));
	}

	public void printCommentAfterBody(SimpleNode node, SimpleNode firstBodyNode) {
		if (!isIgnoreComments(node))
			printComment(node, extractComments(node.specialsAfter, firstBodyNode, false));
	}

	public void printCommentBefore(SimpleNode node) {
		if (!isIgnoreComments(node))
			printComment(node, extractComments(node.specialsBefore));
	}

	public void printCompOp(int opType) {
		switch (opType) {
		case cmpopType.Eq:
			print(syntaxHelper.getOperatorEqual());
			break;
		case cmpopType.Gt:
			print(syntaxHelper.getOperatorGt());
			break;
		case cmpopType.GtE:
			print(syntaxHelper.getOperatorGtEqual());
			break;
		case cmpopType.In:
			print(syntaxHelper.getOperatorIn());
			break;
		case cmpopType.Is:
			print(syntaxHelper.getOperatorIs());
			break;
		case cmpopType.IsNot:
			print(syntaxHelper.getOperatorIsNot());
			break;
		case cmpopType.Lt:
			print(syntaxHelper.getOperatorLt());
			break;
		case cmpopType.LtE:
			print(syntaxHelper.getOperatorLtEqual());
			break;
		case cmpopType.NotEq:
			print(syntaxHelper.getOperatorNotEqual());
			break;
		case cmpopType.NotIn:
			print(syntaxHelper.getOperatorNotIn());
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
		if (withSpaceBefore)
			printSpace();
		print(syntaxHelper.getOperatorDestination());
		if (withSpaceAfter)
			printSpace();
	}

	public void printDictBeforeValue() {
		printFunctionMarker();
		printSpace();
	}

	public void printDoubleDot() {
		print(syntaxHelper.getDoubleDot());

	}

	public void printDoubleDotAndNewline() {
		printDoubleDot();
		printNewlineAndIndentation();
	}

	protected void printDoubleQuote() {
		print(syntaxHelper.getDoubleQuote());
	}

	public void printEllipsis() {
		print(syntaxHelper.getEllipsis());
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
		print(syntaxHelper.getListSeparator());
	}

	public void printNewlineAndIndentation() {
		print(syntaxHelper.getNewLine());
		print(syntaxHelper.getAlignment());
	}

	public void printNum(Num node) {
		print(node.num);
	}

	protected void printSingleQuote() {
		print(syntaxHelper.getSingleQuote());
	}

	protected void printSpace() {
		print(syntaxHelper.getSpace());
	}

	protected void printStatement(String statement) {
		printStatement(statement, false, true);
	}

	protected void printStatement(String statement, boolean spaceBefore, boolean spaceAfter) {
		if (spaceBefore)
			print(syntaxHelper.beforeStatement());
		print(statement);
		if (spaceAfter)
			print(syntaxHelper.afterStatement());
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

	public void printStatementIf(boolean spaceBefore, boolean spaceAfter) {
		if (!isDisabledIfPrinting())
			printStatement("if", spaceBefore, spaceAfter);
		else
			setDisabledIfPrinting(false);
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
		if (node.unicode)
			print("u");
		// u and r don't exclude themself (but u before r)
		if (node.raw)
			print("r");
		printStrQuote(node);
		print(node.s);
		printStrQuote(node);
	}

	public void printStrQuote(Str node) {
		switch (node.type) {
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
		}
	}

	public void printTripeDoubleQuote() {
		for (int i = 0; i < 3; i++)
			printDoubleQuote();
	}

	public void printTripeSingleQuote() {
		for (int i = 0; i < 3; i++)
			printSingleQuote();
	}

	public void printUnaryOp(int opType) {
		String op;

		switch (opType) {
		case unaryopType.Invert:
			op = syntaxHelper.getOperatorInvert();
			break;
		case unaryopType.Not:
			op = syntaxHelper.getOperatorNot();
			break;
		case unaryopType.UAdd:
			op = syntaxHelper.getOperatorUAdd();
			break;
		case unaryopType.USub:
			op = syntaxHelper.getOperatorUSub();
			break;
		default:
			op = "<undef_unaryop>";
			break;
		}

		print(op);
		if (opType == unaryopType.Not)
			printSpace();

	}

	public void setDisabledIfPrinting(boolean disabledIfPrinting) {
		this.disabledIfPrinting = disabledIfPrinting;
	}

	public void setIgnoreComments(boolean ignoreComments) {
		this.ignoreComments = ignoreComments;
	}

	public void printReprQuote() {
		print(syntaxHelper.getReprQuote());
	}

	public NodeHelper getNodeHelper() {
		return this.nodeHelper;
	}

	public boolean hasSpecialBefore(SimpleNode n, String match) {
		if (n == null)
			return false;
		return checkSpecialStr(n.getSpecialsBefore(), match);
	}

	public boolean hasSpecialAfter(SimpleNode n, String match) {
		if (n == null)
			return false;
		return checkSpecialStr(n.getSpecialsAfter(), match);
	}

	public boolean checkSpecialStr(List<Object> specials, String pattern) {
		for (Object object : specials) {
			if (object instanceof SpecialStr) {
				SpecialStr str = (SpecialStr) object;
				return (str.str.compareTo(pattern) == 0);

			} else if (object instanceof String) {
				String text = (String) object;
				return (text.compareTo(pattern) == 0);
			}
		}
		return false;
	}

	public void openParentheses(SimpleNode n) {
		if (needsParentheses(n)) {
			printBeforeTuple();
		}
	}

	public void closeParentheses(SimpleNode n) {
		if (needsParentheses(n)) {
			printAfterTuple();
		}
	}

	protected boolean needsParentheses(SimpleNode n) {
		return (inCall() || hasParentheses(n));
	}

	protected boolean needsBracket(SimpleNode n) {
		return (hasBracket(n) || nodeHelper.isList(n));
	}

	protected boolean hasParentheses(SimpleNode n) {
		return hasSpecialBefore(n, getSyntaxhelper().beforeTuple());
	}

	boolean hasBracket(SimpleNode n) {
		return hasSpecialBefore(n, getSyntaxhelper().beforeList());
	}

	public void closeBracket(SimpleNode n) {
		if (needsBracket(n)) {
			printAfterList();
		}
	}

	public void openBracket(SimpleNode n) {
		if (needsBracket(n)) {
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