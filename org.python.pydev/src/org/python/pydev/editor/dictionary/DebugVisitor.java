/*
 * Author: atotic
 * Created on Mar 29, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.editor.dictionary;

import org.python.parser.SimpleNode;
import org.python.parser.ast.*;

/**
 * For debugging only. Prints out all the results of a visitor pattern.
 * This is a org.python.parser visitor.
 */
class DebugVisitor extends VisitorBase {

		
	DebugVisitor() {
	}
		
	protected Object unhandled_node(SimpleNode node) throws Exception {
		System.out.println("unhandled_node");
		return null;
	}

	public void traverse(SimpleNode node) throws Exception {
		System.out.println("traverse");
	}
		
	public Object visitAttribute(Attribute node) throws Exception {
		System.out.println("visitAttribute " + node.attr);
		return null;
	}

	public Object visitClassDef(ClassDef node) throws Exception {
		System.out.println("visitClassDef:" + node.name);
		return null;
	}

	public Object visitExpr(Expr node) throws Exception {
		System.out.println("visitExpr:" + node.toString(""));
		return null;
	}

	public Object visitExpression(Expression node) throws Exception {
		System.out.println("visitExpression:" + node.toString(""));
		return null;
	}

	public Object visitFunctionDef(FunctionDef node) throws Exception {
		System.out.println("visitFunctionDef:" + node.toString(""));
		return null;
	}

	public Object visitGlobal(Global node) throws Exception {
		System.out.println("visitGlobal:" + node.toString(""));
		return null;
	}

	public Object visitName(Name node) throws Exception {
		System.out.println("visitName:" + node.toString(""));
		return null;
	}

	public Object visitAssert(Assert node) throws Exception {
		System.out.println("visitAssert:" + node.toString(""));
		return null;
	}

	public Object visitAssign(Assign node) throws Exception {
		System.out.println("visitAssign:" + node.toString(""));
		return null;
	}

	public Object visitAugAssign(AugAssign node) throws Exception {
		System.out.println("visitAugAssign:" + node.toString(""));
		return null;
	}

	public Object visitBinOp(BinOp node) throws Exception {
		System.out.println("visitBinOp:" + node.toString(""));
		return null;
	}

	public Object visitBoolOp(BoolOp node) throws Exception {
		System.out.println("visitBoolOp:" + node.toString(""));
		return null;
	}

	public Object visitBreak(Break node) throws Exception {
		System.out.println("visitBreak:" + node.toString(""));
		return null;
	}

	public Object visitCall(Call node) throws Exception {
		System.out.println("visitCall:" + node.toString(""));
		return null;
	}

	public Object visitCompare(Compare node) throws Exception {
		System.out.println("visitCompare:" + node.toString(""));
		return null;
	}

	public Object visitContinue(Continue node) throws Exception {
		System.out.println("visitContinue:" + node.toString(""));
		return null;
	}

	public Object visitDelete(Delete node) throws Exception {
		System.out.println("visitDelete:" + node.toString(""));
		return null;
	}

	public Object visitDict(Dict node) throws Exception {
		System.out.println("visitDict:" + node.toString(""));
		return null;
	}

	public Object visitEllipsis(Ellipsis node) throws Exception {
		System.out.println("visitEllipsis:" + node.toString(""));
		return null;
	}

	public Object visitExec(Exec node) throws Exception {
		System.out.println("visitExec:" + node.toString(""));
		return null;
	}

	public Object visitExtSlice(ExtSlice node) throws Exception {
		System.out.println("visitExtSlice:" + node.toString(""));
		return null;
	}

	public Object visitFor(For node) throws Exception {
		System.out.println("visitFor:" + node.toString(""));
		return null;
	}

	public Object visitIf(If node) throws Exception {
		System.out.println("visitIf:" + node.toString(""));
		return null;
	}

	public Object visitImport(Import node) throws Exception {
		System.out.println("visitImport:" + node.toString(""));
		return null;
	}

	public Object visitImportFrom(ImportFrom node) throws Exception {
		System.out.println("visitImportFrom:" + node.toString(""));
		return null;
	}

	public Object visitIndex(Index node) throws Exception {
		System.out.println("visitIndex:" + node.toString(""));
		return null;
	}

	public Object visitInteractive(Interactive node) throws Exception {
		System.out.println("visitInteractive:" + node.toString(""));
		return null;
	}

	public Object visitLambda(Lambda node) throws Exception {
		System.out.println("visitLambda:" + node.toString(""));
		return null;
	}

	public Object visitList(List node) throws Exception {
		System.out.println("visitList:" + node.toString(""));
		return null;
	}

	public Object visitListComp(ListComp node) throws Exception {
		System.out.println("visitListComp:" + node.toString(""));
		return null;
	}

	public Object visitModule(Module node) throws Exception {
		System.out.println("visitModule:" + node.toString(""));
		return null;
	}

	public Object visitNum(Num node) throws Exception {
		System.out.println("visitNum:" + node.toString(""));
		return null;
	}

	public Object visitPass(Pass node) throws Exception {
		System.out.println("visitPass:" + node.toString(""));
		return null;
	}

	public Object visitPrint(Print node) throws Exception {
		System.out.println("visitPrint:" + node.toString(""));
		return null;
	}

	public Object visitRaise(Raise node) throws Exception {
		System.out.println("visitRaise:" + node.toString(""));
		return null;
	}

	public Object visitRepr(Repr node) throws Exception {
		System.out.println("visitRepr:" + node.toString(""));
		return null;
	}

	public Object visitReturn(Return node) throws Exception {
		System.out.println("visitReturn:" + node.toString(""));
		return null;
	}

	public Object visitSlice(Slice node) throws Exception {
		System.out.println("visitSlice:" + node.toString(""));
		return null;
	}

	public Object visitStr(Str node) throws Exception {
		System.out.println("visitStr:" + node.toString(""));
		return null;
	}

	public Object visitSubscript(Subscript node) throws Exception {
		System.out.println("visitSubscript:" + node.toString(""));
		return null;
	}

	public Object visitSuite(Suite node) throws Exception {
		System.out.println("visitSuite:" + node.toString(""));
	return null;
	}

	public Object visitTryExcept(TryExcept node) throws Exception {
		System.out.println("visitTryExcept:" + node.toString(""));
		return null;
	}

	public Object visitTryFinally(TryFinally node) throws Exception {
		System.out.println("visitTryFinally:" + node.toString(""));
		return null;
	}

	public Object visitTuple(Tuple node) throws Exception {
		System.out.println("visitTuple:" + node.toString(""));
		return null;
	}

	public Object visitUnaryOp(UnaryOp node) throws Exception {
		System.out.println("visitUnaryOp:" + node.toString(""));
		return null;
	}

	public Object visitWhile(While node) throws Exception {
		System.out.println("visitWhile:" + node.toString(""));
		return null;
	}

	public Object visitYield(Yield node) throws Exception {
		System.out.println("visitYield:" + node.toString(""));
		return null;
	}

}
