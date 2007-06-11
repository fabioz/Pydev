package org.python.pydev.refactoring.ast.visitors;

import java.io.File;
import java.io.PrintWriter;
import java.io.Writer;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISourceModule;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.VisitorIF;
import org.python.pydev.refactoring.ast.adapters.AbstractNodeAdapter;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.printer.SourcePrinter;
import org.python.pydev.refactoring.ast.visitors.context.AbstractContextVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionException;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionExtenderVisitor;
import org.python.pydev.refactoring.ast.visitors.selection.SelectionValidationVisitor;
import org.python.pydev.refactoring.core.PythonModuleManager;

public class VisitorFactory {

	public static ITextSelection createSelectionExtension(AbstractScopeNode<?> scope, ITextSelection selection) {
		SelectionExtenderVisitor visitor = null;
		try {
			visitor = new SelectionExtenderVisitor(scope.getModule(), selection);
			scope.getASTNode().accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor.getSelection();
	}

	public static void validateSelection(ModuleAdapter scope) throws SelectionException {
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

	public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass, String source) throws Throwable {
		return createVisitor(visitorClass, getRootNodeFromString(source));
	}

	public static <T extends VisitorIF> T createVisitor(Class<T> visitorClass, SimpleNode root) {
		T visitor = null;
		try {
			visitor = visitorClass.newInstance();
			root.accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor;
	}

	/**
	 * Unchecked (because if doing Class.cast, it does not work in java 1.4)
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractContextVisitor> T createContextVisitor(Class<T> visitorClass, SimpleNode root, ModuleAdapter module,
			AbstractNodeAdapter parent) {
		T visitor = null;
		try {
			visitor = (T) visitorClass.getConstructors()[0].newInstance(new Object[] { module, parent });
			root.accept(visitor);
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}
		return visitor;
	}

	public static ModuleAdapter createModuleAdapter(PythonModuleManager moduleManager, File file, IDocument doc, IPythonNature nature) throws Throwable {
		if (file != null && file.exists()) {
			if(moduleManager != null){
				IModulesManager m = moduleManager.getIModuleManager();
				if(m != null){
					String modName = m.resolveModule(REF.getFileAbsolutePath(file));
					IModule module = m.getModule(modName, nature, true);
					if (module instanceof ISourceModule) {
						return createModuleAdapter(moduleManager, (ISourceModule) module, nature);
					}
				}
			}
		}
		return new ModuleAdapter(moduleManager, file, doc, getRootNode(doc), nature);
	}

	/**
	 * Preferred way of creating a module adapter
	 */
	public static ModuleAdapter createModuleAdapter(PythonModuleManager moduleManager, ISourceModule module, IPythonNature nature) {
		return new ModuleAdapter(moduleManager, module, nature);
	}


	public static SourcePrinter createPrinter(Writer out, String newLineDelim) {
		return new SourcePrinter(new PrintWriter(out), newLineDelim);
	}

	public static SimpleNode getRootNodeFromString(String source) throws Throwable {
		return getRootNode(getDocumentFromString(source));
	}

	private static IDocument getDocumentFromString(String source) {
		return new Document(source);
	}

	public static Module getRootNode(IDocument doc) throws Throwable {
		Tuple<SimpleNode, Throwable> objects = PyParser.reparseDocument(new PyParser.ParserInfo(doc, false,
				IPythonNature.LATEST_GRAMMAR_VERSION));
		if (objects.o2 != null)
			throw objects.o2;
		return (Module) objects.o1;
	}

}