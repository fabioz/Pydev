package org.python.pydev.refactoring.ast.visitors;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.printer.SourcePrinter;
import org.python.pydev.refactoring.ast.rewriter.RewriterVisitor;
import org.python.pydev.refactoring.ast.visitors.context.AbstractContextVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionException;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionExtenderVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionValidationVisitor;
import org.python.pydev.refactoring.core.PythonModuleManager;

public class VisitorFactory {

	public static String createSourceFromAST(SimpleNode root,
			boolean ignoreComments) {
		RewriterVisitor visitor = null;
		StringWriter writer = new StringWriter();
		try {
			visitor = new RewriterVisitor(createPrinter(writer));
			visitor.setIgnoreComments(ignoreComments);
			visitor.visit(root);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		visitor.flush();
		return writer.getBuffer().toString();
	}

	public static String createSourceFromAST(SimpleNode root) {
		return createSourceFromAST(root, false);
	}

	public static RewriterVisitor createRewriterVisitor(Writer out,
			String source) {
		RewriterVisitor visitor = null;
		try {
			SimpleNode root = getRootNodeFromString(source);
			visitor = new RewriterVisitor(createPrinter(out));
			root.accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		visitor.flush();
		return visitor;
	}

	public static ITextSelection createSelectionExtension(
			AbstractScopeNode<?> scope, ITextSelection selection) {
		SelectionExtenderVisitor visitor = null;
		try {
			visitor = new SelectionExtenderVisitor(scope.getModule(), selection);
			scope.getASTNode().accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor.getSelection();
	}

	public static void validateSelection(ModuleAdapter scope)
			throws SelectionException {
		SelectionValidationVisitor visitor = null;
		try {
			visitor = new SelectionValidationVisitor();
			scope.getASTNode().accept(visitor);
		} catch (SelectionException e) {
			throw e;
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
	}

	public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass,
			String source) throws Throwable {
		return createVisitor(visitorClass, getRootNodeFromString(source));
	}

	public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass,
			SimpleNode root) {
		T visitor = null;
		try {
			visitor = visitorClass.newInstance();
			root.accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor;
	}

	public static <T extends AbstractContextVisitor> T createContextVisitor(
			Class<T> visitorClass, SimpleNode root, ModuleAdapter module,
			AbstractNodeAdapter parent) {
		T visitor = null;
		try {
			visitor = visitorClass.cast(visitorClass.getConstructors()[0]
					.newInstance(new Object[] { module, parent }));
			root.accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor;
	}

	public static ModuleAdapter createModuleAdapter(
			PythonModuleManager moduleManager, File file, IDocument doc)
			throws Throwable {
		return new ModuleAdapter(moduleManager, file, doc, getRootNode(doc));
	}

	private static SourcePrinter createPrinter(Writer out) {
		return new SourcePrinter(new PrintWriter(out));
	}

	private static SimpleNode getRootNodeFromString(String source)
			throws Throwable {
		return getRootNode(getDocumentFromString(source));
	}

	private static IDocument getDocumentFromString(String source) {
		return new Document(source);
	}

	private static Module getRootNode(IDocument doc) throws Throwable {
		Tuple<SimpleNode, Throwable> objects = PyParser
				.reparseDocument(new PyParser.ParserInfo(doc, false,
						IPythonNature.LATEST_GRAMMAR_VERSION));
		if (objects.o2 != null)
			throw objects.o2;
		return (Module) objects.o1;
	}

}