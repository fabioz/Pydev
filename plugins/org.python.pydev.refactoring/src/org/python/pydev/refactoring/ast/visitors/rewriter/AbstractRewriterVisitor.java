/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.visitors.rewriter;


/**
 * Abstract Rewriter
 * 
 * @author Ueli Kistler
 * 
 */
public abstract class AbstractRewriterVisitor {
//
//	// node printer helper class
//	protected final SourcePrinter printer;
//
//	// Tracking last node
//	private SimpleNode previousNode;
//
//	public AbstractRewriterVisitor(OutputStream out, String newLineDelim) {
//		this(new SourcePrinter(new PrintWriter(out, true), newLineDelim));
//	}
//
//	public AbstractRewriterVisitor(SourcePrinter printer) {
//		this.printer = printer;
//	}
//
//	public AbstractRewriterVisitor(Writer out, String newLineDelim) {
//		this(new SourcePrinter(new PrintWriter(out), newLineDelim));
//	}
//
//	protected void enterCall() {
//		printer.enterCall();
//	}
//
//	public void flush() {
//		printer.flushStream();
//	}
//
//	protected SimpleNode getPreviousNode() {
//		return previousNode;
//	}
//
//	protected void handleAfterNode(SimpleNode node) {
//		printer.prettyPrintAfter(node, getPreviousNode());
//	}
//
//	protected void handleBeforeNode(SimpleNode node) {
//	}
//
//	protected boolean inCall() {
//		return printer.inCall();
//	}
//
//	protected void leaveCall() {
//		printer.leaveCall();
//	}
//
//	public void setIgnoreComments(boolean ignoreComments) {
//		this.printer.setIgnoreComments(ignoreComments);
//	}
//
//	protected void setPreviousNode(SimpleNode lastNode) {
//		this.previousNode = lastNode;
//	}
//
//	@Override
//	public void traverse(SimpleNode node) throws Exception {
//		throw new Exception("Use visit() method");
//	}
//
//	@Override
//	protected Object unhandled_node(SimpleNode node) throws Exception {
//		printer.print("[unhandled :" + node.toString());
//		traverse(node);
//		return null;
//	}
//
//	public SimpleNode visit(SimpleNode node) throws Exception {
//		handleBeforeNode(node);
//		node = visitNode(node);
//		handleAfterNode(node);
//
//		return node;
//	}
//
//	/**
//	 * Additional nodes supported by this visitor
//	 */
//	public abstract Object visitAliasType(aliasType node) throws Exception;
//
//	public abstract Object visitArgumentsType(argumentsType node) throws Exception;
//
//	public abstract Object visitDecoratorsType(decoratorsType node) throws Exception;
//
//	public abstract Object visitExceptHandlerType(excepthandlerType node) throws Exception;
//
//	public abstract Object visitKeywordType(keywordType node) throws Exception;
//
//	public abstract Object visitListCompType(ListComp node) throws Exception;
//
//	protected SimpleNode visitNode(SimpleNode node) throws Exception {
//		if (node == null) {
//			return null;
//		}
//
//		if (node instanceof suiteType) {
//			node = (SimpleNode) visitSuiteType((suiteType) node);
//		} else if (node instanceof decoratorsType) {
//			visitDecoratorsType((decoratorsType) node);
//		} else if (node instanceof keywordType) {
//			node = (SimpleNode) visitKeywordType((keywordType) node);
//		} else if (node instanceof argumentsType) {
//			node = (SimpleNode) visitArgumentsType((argumentsType) node);
//		} else if (node instanceof aliasType) {
//			node = (SimpleNode) visitAliasType((aliasType) node);
//		} else {
//			node.accept(this);
//		}
//
//		setPreviousNode(node);
//
//		return node;
//	}
//
//	public abstract Object visitSuiteType(suiteType suite) throws Exception;

}
