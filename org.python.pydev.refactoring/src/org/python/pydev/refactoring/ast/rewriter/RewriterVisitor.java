/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.rewriter;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Assert;
import org.python.pydev.parser.jython.ast.Assign;
import org.python.pydev.parser.jython.ast.Attribute;
import org.python.pydev.parser.jython.ast.AugAssign;
import org.python.pydev.parser.jython.ast.BinOp;
import org.python.pydev.parser.jython.ast.BoolOp;
import org.python.pydev.parser.jython.ast.Break;
import org.python.pydev.parser.jython.ast.Call;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Compare;
import org.python.pydev.parser.jython.ast.Comprehension;
import org.python.pydev.parser.jython.ast.Continue;
import org.python.pydev.parser.jython.ast.Delete;
import org.python.pydev.parser.jython.ast.Dict;
import org.python.pydev.parser.jython.ast.Ellipsis;
import org.python.pydev.parser.jython.ast.Exec;
import org.python.pydev.parser.jython.ast.Expr;
import org.python.pydev.parser.jython.ast.Expression;
import org.python.pydev.parser.jython.ast.ExtSlice;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.GeneratorExp;
import org.python.pydev.parser.jython.ast.Global;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.IfExp;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.Index;
import org.python.pydev.parser.jython.ast.Interactive;
import org.python.pydev.parser.jython.ast.Lambda;
import org.python.pydev.parser.jython.ast.List;
import org.python.pydev.parser.jython.ast.ListComp;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Num;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.Print;
import org.python.pydev.parser.jython.ast.Raise;
import org.python.pydev.parser.jython.ast.Repr;
import org.python.pydev.parser.jython.ast.Return;
import org.python.pydev.parser.jython.ast.Slice;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.StrJoin;
import org.python.pydev.parser.jython.ast.Subscript;
import org.python.pydev.parser.jython.ast.Suite;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.Tuple;
import org.python.pydev.parser.jython.ast.UnaryOp;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.Yield;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.argumentsType;
import org.python.pydev.parser.jython.ast.decoratorsType;
import org.python.pydev.parser.jython.ast.excepthandlerType;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.keywordType;
import org.python.pydev.parser.jython.ast.listcompType;
import org.python.pydev.parser.jython.ast.modType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.suiteType;
import org.python.pydev.refactoring.ast.printer.SourcePrinter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

/**
 * GoF Visitor Pattern for AST traversal (see AbstractRewriterVisitor for implemented Jython Visitor class). It will traverse an Abstract
 * Syntax Tree, in order to reprint the source code.
 * 
 * @author Ueli Kistler (ukistler[at]hsr.ch)
 * 
 */
public class RewriterVisitor extends AbstractRewriterVisitor {
    
    //------------------------------------------------------------------------------------------------- public interface

    //REWRITER INTERFACE FOR CURRENT VERSION ---------------------------------------------------------------------------
    public static String reparsed(String source, String string) {
        StringWriter out = new StringWriter();
        createRewriterVisitor(out, source, "\n");
        return out.getBuffer().toString();
    }

    public static String createSourceFromAST(SimpleNode root, boolean ignoreComments, String newLineDelim) {
        RewriterVisitor visitor = null;
        StringWriter writer = new StringWriter();
        try {
            visitor = new RewriterVisitor(VisitorFactory.createPrinter(writer, newLineDelim));
            visitor.setIgnoreComments(ignoreComments);
            visitor.visit(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        visitor.flush();
        return writer.getBuffer().toString();
    }

    public static String createSourceFromAST(SimpleNode root, String newLineDelim) {
        return createSourceFromAST(root, false, newLineDelim);
    }

    private static RewriterVisitor createRewriterVisitor(Writer out, String source, String newLineDelim) {
        RewriterVisitor visitor = new RewriterVisitor(VisitorFactory.createPrinter(out, newLineDelim));;
                
        try {
            SimpleNode root = VisitorFactory.getRootNodeFromString(source);
			root.accept(visitor);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

        visitor.flush();
        return visitor;
    }
    
    //END REWRITER INTERFACE FOR CURRENT VERSION -----------------------------------------------------------------------


//    public static String reparsed(String source, String delimiter) {
//        try {
//            SimpleNode root = VisitorFactory.getRootNodeFromString(source);
//            return createSourceFromAST(root, delimiter);
//        } catch (Throwable e) {
//            throw new RuntimeException(e);
//        }
//        
//    }
//    
//    public static String createSourceFromAST(SimpleNode root, String newLineDelim) {
//        return createSourceFromAST(root, false, newLineDelim);
//    }
//
//
//    public static String createSourceFromAST(SimpleNode root, boolean ignoreComments, String newLineDelim) {
//        final WriterEraser stringWriter = new WriterEraser();
//        PrettyPrinterPrefs prettyPrinterPrefs = new PrettyPrinterPrefs(newLineDelim);
//        prettyPrinterPrefs.setSpacesAfterComma(1);
//        prettyPrinterPrefs.setSpacesBeforeComment(1);
//        PrettyPrinter printer = new PrettyPrinter(prettyPrinterPrefs, stringWriter);
//        try {
//            root.accept(printer);
//            return stringWriter.getBuffer().toString();
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        
//    }


    
    //--------------------------------------------------------------------------------------------- end public interface


	private RewriterVisitor(SourcePrinter printer) {
		super(printer);
	}

	private SimpleNode handleCallArguments(Call node) throws Exception {
		SimpleNode lastNode = null;
		lastNode = visitWithSeparator(node, node.args);
		if (printer.getNodeHelper().isFilledList(node.keywords)) {
			// comma before already handeld in visitWithSeparator
			lastNode = visitWithSeparator(node, node.keywords);
		}
		if (node.starargs != null) {
			// comma before already handeld in visitWithSeparator
			lastNode = visit(node.starargs);
		}
		if (node.kwargs != null) {
			if (node.starargs != null)
				printer.printListSeparator();
			lastNode = visit(node.kwargs);
		}
		return lastNode;
	}

	private boolean handleCommaOptional(SimpleNode node) {
		return (printer.hasSpecialAfter(node, printer.getSyntaxhelper().getComma()));
	}

	protected void handleCommentAfter(SimpleNode node) {
		printer.printCommentAfter(node);
	}

	private void handleCommentAfterBody(SimpleNode node, SimpleNode firstBodyNode) {
		printer.printCommentAfterBody(node, firstBodyNode);
	}

	protected void handleCommentBefore(SimpleNode node) {
		printer.printCommentBefore(node);
	}

	private void handleCommentBeforeBody(SimpleNode node, SimpleNode firstBodyNode) {
		printer.printCommentBeforeBody(node, firstBodyNode);
	}

	private SimpleNode handleDecoratorArgs(decoratorsType node) throws Exception {
		if (node == null)
			return null;
		SimpleNode lastNode = null;

		if (node.args != null) {
			if (isFilledList(node.args))
				printer.printBeforeTuple();

			lastNode = visitWithSeparator(node, reverseNodeArray(node.args));

			if (isFilledList(node.keywords)) {
				printer.printListSeparator();
				lastNode = visitWithSeparator(node, reverseNodeArray(node.keywords));
			}
			if (node.starargs != null) {
				// comma before already handeld in visitWithSeparator
				printer.printBeforeVarArg();
				super.visit(node.starargs);
				lastNode = node.starargs;
			}
			if (node.kwargs != null) {
				if (lastNode != null)
					printer.printListSeparator();
				printer.printBeforeKwArg();
				super.visit(node.kwargs);
				lastNode = node.kwargs;
			}

			if (isFilledList(node.args))
				printer.printAfterTuple();

		}
		return lastNode;
	}

	private SimpleNode handleFunctionArgs(SimpleNode node, argumentsType arguments) throws Exception {
		SimpleNode lastNode = null;

		if (arguments != null) {
			lastNode = handlePositionalAndDefaultArgs(node, arguments);
			lastNode = handleVarArgs(arguments, lastNode);
			lastNode = handleKwArgs(arguments, lastNode);
		}
		return lastNode;
	}

	private void handleIfElse(If node) throws Exception {
		boolean passedElsif = false;
		if (node.orelse != null) {
			for (stmtType statement : node.orelse) {
				passedElsif = handleIfElseSuite(node, passedElsif, statement);

			}
			if (passedElsif)
				printer.outdent();

		}
	}

	private boolean handleIfElseSuite(If node, boolean passedElsif, stmtType statement) throws Exception {
		printer.setDisabledIfPrinting(true);

		printer.printNewlineAndIndentation();

		if (printer.getNodeHelper().isIfStatement(statement)) {
			if (!passedElsif) {
				printer.printStatementElif();
			} else
				printer.setDisabledIfPrinting(false);
		} else if (!passedElsif) {
			passedElsif = true;
			printer.printStatementElse();
			printer.printFunctionMarker();
			printer.indent();
			printer.printNewlineAndIndentation();
		}

		visit(statement);
		return passedElsif;
	}

	private exprType handleKeyValueArgs(SimpleNode parent, exprType[] keys, exprType[] values) throws Exception {
		exprType lastNode = null;
		if (keys == null)
			return lastNode;

		int startOffset = 0;

		if (values != null)
			startOffset = (keys.length - values.length);

		for (int i = 0; i < keys.length; i++) {
			super.visit(keys[i]);
			if ((values != null) && (i >= startOffset)) {
				if (values[i - startOffset] != null) {
					if (printer.getNodeHelper().isDict(parent)) {
						printer.printDictBeforeValue();
					} else {
						printer.printAssignmentOperator(false, false);
					}
					super.visit(values[i - startOffset]);
				}

			}
			if (i < keys.length - 1)
				printer.printListSeparator();
			else
				lastNode = keys[i];

		}
		return lastNode;
	}

	private SimpleNode handleKwArgs(argumentsType arguments, SimpleNode lastNode) throws Exception {
		if (arguments.kwarg != null) {
			if (lastNode != null)
				printer.printListSeparator();
			printer.printBeforeKwArg();
			super.visit(arguments.kwarg);
			lastNode = arguments.kwarg;

			if (handleCommaOptional(lastNode))
				printer.printListSeparator();
		}
		return lastNode;
	}

	private SimpleNode handlePositionalAndDefaultArgs(SimpleNode node, argumentsType arguments) throws Exception {
		SimpleNode lastNode;
		lastNode = visitKeyValue(node, arguments.args, arguments.defaults);
		if (handleCommaOptional(lastNode) && arguments.vararg == null && arguments.kwarg == null)
			printer.printListSeparator();
		return lastNode;
	}

	private void handlePostNode(SimpleNode parent, SimpleNode lastNode, Iterator<SimpleNode> iter, boolean outdent, boolean separator, String separatorStr) {
		if (separator)
			handleSeparator(parent, lastNode, iter, separatorStr);
		handleCommentAfter(lastNode);

		if (outdent) {
			if (!iter.hasNext()) {
				printer.outdent();
			}
		}
		if (iter.hasNext() && !separator) {
			if (!(printer.getNodeHelper().isComprehension(parent))) {
				printer.printNewlineAndIndentation();
			}
		}
	}

	private void handlePreNode(SimpleNode parent, SimpleNode lastNode, java.util.List<SimpleNode> nodes) {
		handleCommentBefore(lastNode);
		if (printer.getNodeHelper().isComprehension(parent)) {
			printer.printStatementIf(lastNode, true, true);
		}
	}

	private void handleRootNode(modType node, stmtType[] body) throws Exception {
		handleCommentBefore(node);
		handleCommentAfter(visit(node, body, false, false));
		handleCommentAfter(node);
	}

	private void handleSeparator(SimpleNode parent, SimpleNode lastNode, Iterator<SimpleNode> iter, String separatorStr) {
		if (iter.hasNext()) {
			if (printer.getNodeHelper().isBoolOp(parent)) {
				BoolOp boolParent = (BoolOp) parent;
				printer.printBoolOp(boolParent.op);
			} else {
				printer.printListSeparator(separatorStr);
			}
		} else {
			if (handleCommaOptional(lastNode)) {
				printer.printListSeparator(separatorStr);
			}

		}

	}

	private void handleTryBody(stmtType node, stmtType[] body) throws Exception {
		if (!printer.getNodeHelper().isTryStatement(node))
			return;

		printer.printStatementTry();
		printer.printFunctionMarker();
		printer.indent();
		if (isFilledList(body))
			handleCommentBeforeBody(node, body[0]);

		printer.printNewlineAndIndentation();

		if (isFilledList(body)) {
			handleCommentAfterBody(node, visit(node, body));
		}
	}

	private SimpleNode handleVarArgs(argumentsType arguments, SimpleNode lastNode) throws Exception {
		if (arguments.vararg != null) {
			if (lastNode != null)
				printer.printListSeparator();
			printer.printBeforeVarArg();
			super.visit(arguments.vararg);
			lastNode = arguments.vararg;

			if (handleCommaOptional(lastNode) && arguments.kwarg == null)
				printer.printListSeparator();
		}
		return lastNode;
	}

	public boolean isEmptyList(SimpleNode[] list) {
		return printer.getNodeHelper().isEmptyList(list);
	}

	public boolean isFilledList(SimpleNode[] list) {
		return printer.getNodeHelper().isFilledList(list);
	}

	private SimpleNode[] reverseNodeArray(SimpleNode[] expressions) {
		java.util.List<SimpleNode> ifs = Arrays.asList(expressions);
		Collections.reverse(ifs);
		SimpleNode[] ifsInOrder = ifs.toArray(new SimpleNode[0]);
		return ifsInOrder;
	}

	protected void visit(decoratorsType[] decs) throws Exception {
		if (decs == null)
			return;
		java.util.List<decoratorsType> stmts = Arrays.asList(decs);

		for (Iterator<decoratorsType> iter = stmts.iterator(); iter.hasNext();) {
			decoratorsType node = iter.next();

			visitDecoratorsType(node);

		}
	}

	@Override
	public SimpleNode visit(SimpleNode node) throws Exception {
		SimpleNode lastNode = null;

		handleBeforeNode(node);
		handleCommentBefore(node);
		lastNode = super.visitNode(node);
		handleCommentAfter(node);
		handleAfterNode(node);

		return lastNode;
	}

	private SimpleNode visit(SimpleNode node, SimpleNode[] body) throws Exception {
		return visit(node, body, true, false);
	}

	private SimpleNode visit(SimpleNode parent, SimpleNode[] list, boolean outdent, boolean separator) throws Exception {
	    return visit(parent, list, outdent, separator, null);
	}
	
	private SimpleNode visit(SimpleNode parent, SimpleNode[] list, boolean outdent, boolean separator, String separatorStr) throws Exception {
		if (list == null)
			return null;
		SimpleNode lastNode = null;
		java.util.List<SimpleNode> nodes = Arrays.asList(list);
		for (Iterator<SimpleNode> iter = nodes.iterator(); iter.hasNext();) {
			lastNode = iter.next();

			handlePreNode(parent, lastNode, nodes);

			super.visit(lastNode);

			handlePostNode(parent, lastNode, iter, outdent, separator, separatorStr);

		}
		return lastNode;
	}

	public Object visitAliasType(aliasType node) throws Exception {
		SimpleNode lastNode = super.visit(node.name);
		if (node.asname != null) {
			printer.printStatementAs();
			super.visit(node.asname);
			lastNode = node.asname;
		}
		handleCommentAfter(lastNode);

		return lastNode;
	}

	public Object visitArgumentsType(argumentsType node) throws Exception {
		SimpleNode lastNode = handleFunctionArgs(getPreviousNode(), node);
		return lastNode;
	}

	public Object visitAssert(Assert node) throws Exception {
		handleCommentBefore(node.test);
		printer.printStatementAssert();
		super.visit(node.test);
		if (node.msg != null) {
			printer.printListSeparator();
			visit(node.msg);
			handleCommentAfter(node.msg);
		} else
			handleCommentAfter(node.test);

		return null;
	}

	public Object visitAssign(Assign node) throws Exception {
		visitWithSeparator(node, node.targets, " = ");
		printer.printAssignmentOperator(!inCall(), !inCall());
		visit(node.value);
		return null;
	}

	public Object visitAttribute(Attribute node) throws Exception {
		visit(node.value);
		printer.printAttributeSeparator();
		visit(node.attr);
		return null;
	}

	public Object visitAugAssign(AugAssign node) throws Exception {
		visit(node.target);
		printer.printBinOp(node.op, true, false);
		printer.printAssignmentOperator(false, true);
		visit(node.value);

		return null;

	}

	public Object visitBinOp(BinOp node) throws Exception {
		if ((node.left != null) && (node.right != null)) {
			visit(node.left);
			printer.printBinOp(node.op, true, true);
			visit(node.right);
		}
		return null;
	}

	public Object visitBoolOp(BoolOp node) throws Exception {
		visitWithSeparator(node, node.values);
		return null;
	}

	public Object visitBreak(Break node) throws Exception {
		printer.printStatementBreak();
		return null;
	}

	public Object visitCall(Call node) throws Exception {
		visit(node.func);
		enterCall();
		printer.openParentheses(node);
		handleCallArguments(node);
		printer.closeParentheses(node);
		leaveCall();

		return null;

	}

	public Object visitClassDef(ClassDef node) throws Exception {
		SimpleNode lastNode = null;

		printer.printClassDef();
		super.visit(node.name);

		printer.setIgnoreComments(true);
		if (isFilledList(node.bases)) {
			printer.printBeforeTuple();
			lastNode = visitWithSeparator(node, node.bases);
			printer.printAfterTuple();
		}
		printer.setIgnoreComments(false);

		printer.indent();

		printer.printFunctionMarker();

		if (lastNode == null) {
			handleCommentAfter(node.name);
		} else {
			handleCommentAfter(lastNode);
		}
		printer.printNewlineAndIndentation();

		visit(node, node.body);

		return null;
	}

	public Object visitCompare(Compare node) throws Exception {
		visit(node.left);
		for (int i = 0; i < node.ops.length; i++) {
			printer.printBeforeAndAfterCmpOp();
			printer.printCompOp(node.ops[i]);
			printer.printBeforeAndAfterCmpOp();
			visit(node.comparators[i]);

		}
		return null;
	}

	public Object visitComprehension(Comprehension node) throws Exception {
		printer.printStatementFor(true, true);
		super.visit(node.target);
		printer.printStatementIn();
		super.visit(node.iter);
		if (isFilledList(node.ifs)) {
			printer.indent();
			visit(node, reverseNodeArray(node.ifs));
		}

		return null;
	}

	public Object visitContinue(Continue node) throws Exception {
		printer.printContinue();
		return null;
	}

	public Object visitDecoratorsType(decoratorsType node) throws Exception {
		if (node == null)
			return null;

		printer.printBeforeDecorator();
		super.visit(node.func);

		SimpleNode lastNode = handleDecoratorArgs(node);

		if (lastNode == null) {
			handleCommentAfter(node.func);
		} else {
			handleCommentAfter(lastNode);
		}
		printer.printNewlineAndIndentation();

		return lastNode;
	}

	public Object visitDelete(Delete node) throws Exception {
		printer.printStatementDel();
		visitWithSeparator(node, node.targets);
		return null;
	}

	public Object visitDict(Dict node) throws Exception {
		printer.printBeforeDict();
		SimpleNode lastNode = visitKeyValue(node, node.keys, node.values);
		printer.printAfterDict();
		handleCommentAfter(lastNode);

		return null;
	}

	public Object visitEllipsis(Ellipsis node) throws Exception {
		printer.printEllipsis();
		return null;
	}

	public Object visitExceptHandlerType(excepthandlerType node) throws Exception {
		printer.printStatementExcept(node.type != null);
		super.visit(node.type);
		if (node.name != null) {
			printer.printListSeparator();
			super.visit(node.name);
		}
		printer.printFunctionMarker();
		printer.indent();

		if (isFilledList(node.body))
			handleCommentBeforeBody(node.type, node.body[0]);

		printer.printNewlineAndIndentation();
		SimpleNode lastNode = visit(node, node.body);

		if (isFilledList(node.body))
			handleCommentAfterBody(node, lastNode);
		return null;

	}

	private void visitExceptionHandlers(TryExcept node) throws Exception {
		if (node.handlers != null) {
			for (excepthandlerType exceptHandler : node.handlers) {
				printer.printNewlineAndIndentation();
				visitExceptHandlerType(exceptHandler);
			}

		}
	}

	public Object visitExec(Exec node) throws Exception {
		printer.printStatementExec();
		visit(node.body);
		if (node.globals != null) {
			printer.printStatementIn();
			visit(node.globals);
			if (node.locals != null) {
				printer.printListSeparator();
				visit(node.locals);
			}
		}

		return null;
	}

	public Object visitExpr(Expr node) throws Exception {
		visit(node.value);
		return null;
	}

	public Object visitExpression(Expression node) throws Exception {
		visit(node.body);
		return null;
	}

	public Object visitExtSlice(ExtSlice node) throws Exception {
		visitWithSeparator(node, node.dims);
		return null;
	}

	public Object visitFor(For node) throws Exception {
		handleCommentBefore(node.target);
		printer.printStatementFor(false, true);
		printer.openParentheses(node);
		super.visit(node.target);
		printer.closeParentheses(node);
		printer.printStatementIn();
		super.visit(node.iter);
		printer.printFunctionMarker();
		handleCommentAfter(node.iter);

		printer.indent();
		printer.printNewlineAndIndentation();
		visit(node, node.body);

		visitSuiteType(node.orelse);

		return null;

	}

	public Object visitFunctionDef(FunctionDef node) throws Exception {

		SimpleNode lastNode = null;

		visit(node.decs);

		handleCommentBefore(node.name);
		printer.printFunctionDef();
		super.visit(node.name);

		enterCall();
		printer.printBeforeTuple();
		this.setPreviousNode(node);
		lastNode = super.visit(node.args);
		this.setPreviousNode(node.name);
		printer.printAfterTuple();

		printer.indent();

		printer.printFunctionMarker();
		leaveCall();

		if (lastNode == null)
			handleCommentAfter(node.name);
		else
			handleCommentAfter(lastNode);

		printer.printNewlineAndIndentation();

		visit(node, node.body);

		return null;
	}

	public Object visitGeneratorExp(GeneratorExp node) throws Exception {
		printer.openParentheses(node);
		visit(node.elt);
		visit(node, node.generators, false, false);
		return null;
	}

	public Object visitGlobal(Global node) throws Exception {
		printer.printStatementGlobal();
		visitWithSeparator(node, node.names);
		return null;
	}

	public Object visitIf(If node) throws Exception {

		printer.printStatementIf(node, false, true);

		printer.openParentheses(node);
		super.visit(node.test);
		printer.closeParentheses(node);
		printer.printFunctionMarker();
		printer.indent();
		handleCommentAfter(node.test);

		printer.printNewlineAndIndentation();
		if (isFilledList(node.body))
			handleCommentBeforeBody(node, node.body[0]);

		if (isFilledList(node.body))
			handleCommentAfterBody(node, visit(node, node.body));

		handleIfElse(node);
		
		return null;
	}

	public Object visitIfExp(IfExp node) throws Exception {
		super.visit(node.body);

		printer.printStatementIf(node, true, true);
		printer.openParentheses(node.test);
		super.visit(node.test);
		printer.closeParentheses(node.test);

		if (node.orelse != null) {
			printer.printStatementElseWithSpace();
			super.visit(node.orelse);
		}

		return null;

	}

	public Object visitImport(Import node) throws Exception {
		printer.printStatementImport();
		visitWithSeparator(node, node.names);
		return null;
	}

	public Object visitImportFrom(ImportFrom node) throws Exception {

		handleCommentBefore(node.module);
		printer.printStatementFrom();
		super.visit(node.module);
		printer.printStatementFromImport();

		if (isFilledList(node.names))
			visitWithSeparator(node, node.names);
		else
			printer.printBinOp(BinOp.Mult, false, false);

		return null;
	}

	public Object visitIndex(Index node) throws Exception {
		visit(node.value);
		return null;
	}

	public Object visitInteractive(Interactive node) throws Exception {
		handleRootNode(node, node.body);
		return null;
	}

	public exprType visitKeyValue(SimpleNode parent, exprType[] keys, exprType[] values) throws Exception {
		return handleKeyValueArgs(parent, keys, values);
	}

	@Override
	public Object visitKeywordType(keywordType node) throws Exception {
		SimpleNode lastNode = visit(node.arg);
		printer.printAssignmentOperator(false, false);
		if (node.value != null)
			lastNode = visit(node.value);
		return lastNode;
	}

	public Object visitLambda(Lambda node) throws Exception {
		printer.printstatementLambda();
		setPreviousNode(node);
		visit(node.args);
		setPreviousNode(node);
		printer.printFunctionMarkerWithSpace();
		visit(node.body);
		return null;

	}

	public Object visitList(List node) throws Exception {
		if (isFilledList(node.elts)) {
			handleCommentBefore(node.elts[0]);
		}
		printer.openBracket(node);
		printer.setIgnoreComments(true);
		SimpleNode lastNode = visitWithSeparator(node, node.elts);
		printer.setIgnoreComments(false);
		printer.closeBracket(node);

		handleCommentAfter(lastNode);
		return null;
	}

	public Object visitListComp(ListComp node) throws Exception {
		if (!inCall())
			printer.openParentheses(node);
		printer.openBracket(node);
		visit(node.elt);
		visit(node, node.generators, false, false);
		printer.closeBracket(node);
		if (!inCall())
			printer.closeParentheses(node);
		return null;
	}

	public Object visitListCompType(listcompType node) throws Exception {
		SimpleNode lastNode = visit(node.target);
		if (node.iter != null)
			lastNode = visit(node.iter);
		if (isFilledList(node.ifs)) {
			lastNode = visit(node, node.ifs, false, false);
		}
		return lastNode;
	}

	public Object visitModule(Module node) throws Exception {
		handleRootNode(node, node.body);
		return null;
	}

	public Object visitName(Name node) throws Exception {
		printer.print(node.id);
		return null;
	}

	public Object visitNameTok(NameTok node) throws Exception {
		printer.print(node.id);
		return null;
	}

	public Object visitNum(Num node) throws Exception {
		printer.printNum(node);
		return null;
	}

	public Object visitPass(Pass node) throws Exception {
		printer.printStatementPass();
		return null;
	}

	public Object visitPrint(Print node) throws Exception {
		printer.printStatementPrint();
		if (node.dest != null) {
			printer.printDestinationOperator(false, true);
			visit(node.dest);
			if (isFilledList(node.values)) {
				printer.printListSeparator();
			}
		}
		visitWithSeparator(node, node.values);

		return null;
	}

	public Object visitRaise(Raise node) throws Exception {
		printer.printStatementRaise(node.type != null);
		visit(node.type);
		if (node.inst != null) {
			printer.printListSeparator();
			visit(node.inst);
		}
		if (node.tback != null) {
			printer.printListSeparator();
			visit(node.tback);
		}

		return null;
	}

	public Object visitRepr(Repr node) throws Exception {
		handleCommentBefore(node);
		printer.printReprQuote();
		super.visit(node.value);
		printer.printReprQuote();
		handleCommentAfter(node);
		return null;
	}

	public Object visitReturn(Return node) throws Exception {
		printer.printStatementReturn();
		visit(node.value);
		return null;
	}

	public Object visitSlice(Slice node) throws Exception {
		visit(node.lower);
		printer.printFunctionMarker();
		visit(node.upper);
		if (node.step != null) {
			printer.printFunctionMarker();
			visit(node.step);
		}
		return null;

	}

	public Object visitStr(Str node) throws Exception {
		printer.printStr(node);
		return null;
	}

	public Object visitStrJoin(StrJoin node) throws Exception {
		visitWithSeparator(node, node.strs);
		return null;
	}

	public Object visitSubscript(Subscript node) throws Exception {
		visit(node.value);
		printer.openBracket(node);
		visit(node.slice);
		printer.closeBracket(node);
		return null;
	}

	public Object visitSuite(Suite node) throws Exception {
		handleRootNode(node, node.body);
		return null;
	}

	public Object visitSuiteType(suiteType node) throws Exception {
		if (node != null) {
			if (!(printer.getNodeHelper().isTryFinallyStatement(getPreviousNode()))) {
				printer.printNewlineAndIndentation();
				printer.printStatementElse();

				printer.printFunctionMarker();

			}
			if (isFilledList(node.body))
				handleCommentBeforeBody(node, node.body[0]);

			printer.indent();
			printer.printNewlineAndIndentation();
			SimpleNode lastNode = visit(node, node.body);
			if (isFilledList(node.body))
				handleCommentAfterBody(node, lastNode);
		}
		return null;
	}

	public Object visitTryExcept(TryExcept node) throws Exception {
		handleTryBody(node, node.body);

		visitExceptionHandlers(node);

		visitSuiteType(node.orelse);

		return null;
	}

	public Object visitTryFinally(TryFinally node) throws Exception {
		handleTryBody(node, node.body);

		if (node.finalbody != null) {
			printer.printNewlineAndIndentation();
			handleCommentBefore(node.finalbody);

			printer.printStatementFinally();
			printer.printFunctionMarker();

			setPreviousNode(node);
			visitSuiteType(node.finalbody);
		}

		return null;
	}

	public Object visitTuple(Tuple node) throws Exception {
		if (isFilledList(node.elts)) {
			handleCommentBefore(node.elts[0]);
		}
		printer.openParentheses(node);
		printer.setIgnoreComments(true);
		SimpleNode lastNode = visitWithSeparator(node, node.elts);
		printer.setIgnoreComments(false);
		printer.closeParentheses(node);

		handleCommentAfter(lastNode);
		return null;
	}

	public Object visitUnaryOp(UnaryOp node) throws Exception {
		if (node.operand != null) {
			printer.printUnaryOp(node.op);
			visit(node.operand);
		}
		return null;

	}

	public Object visitWhile(While node) throws Exception {
		printer.printStatementWhile();
		printer.openParentheses(node);
		super.visit(node.test);
		printer.closeParentheses(node);
		printer.printFunctionMarker();
		if (isFilledList(node.body))
			handleCommentBeforeBody(node.test, node.body[0]);

		printer.indent();
		printer.printNewlineAndIndentation();
		visit(node, node.body);

		visitSuiteType(node.orelse);

		return null;
	}

	public Object visitWith(With node) throws Exception {
		SimpleNode lastNode = node.context_expr;

		printer.printStatementWith();
		printer.openParentheses(node.context_expr);
		super.visit(node.context_expr);
		printer.closeParentheses(node.context_expr);

		if (node.optional_vars != null) {
			printer.printStatementAs();
			super.visit(node.optional_vars);
			lastNode = node.optional_vars;
		}
		printer.printFunctionMarker();
		printer.indent();
		handleCommentAfter(lastNode);

		printer.printNewlineAndIndentation();

		visit(node, node.body.body);

		return null;
	}

	private SimpleNode visitWithSeparator(SimpleNode parent, SimpleNode[] body, String separator) throws Exception {
	    return visit(parent, body, false, true, separator);
	    
	}
	private SimpleNode visitWithSeparator(SimpleNode parent, SimpleNode[] body) throws Exception {
		return visit(parent, body, false, true);
	}

	public Object visitYield(Yield node) throws Exception {
		handleCommentBefore(node.value);
		printer.printStatementYield();
		super.visit(node.value);
		handleCommentAfter(node.value);
		return null;
	}





}
