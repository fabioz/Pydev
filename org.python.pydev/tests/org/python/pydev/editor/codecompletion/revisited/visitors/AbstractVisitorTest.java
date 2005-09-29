package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Import;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import junit.framework.TestCase;

public class AbstractVisitorTest extends TestCase {

	private static final String MODULE_NAME = "testModule";

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AbstractVisitorTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testImportCreation1() throws Exception {
		Iterator<ASTEntry> iterator = createModuleAndGetImports("import os.path", Import.class);
		
		SimpleNode simpleNode = iterator.next().node;
		List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true);
		assertEquals(2, toks.size());
		
		SourceToken token = (SourceToken) toks.get(0);
		checkIt(simpleNode, token, "os");
		
		token = (SourceToken) toks.get(1);
		checkIt(simpleNode, token, "os.path");
	}

	private void checkIt(SimpleNode simpleNode, SourceToken token, String rep) {
		assertEquals(rep, token.getRepresentation());
		assertSame(simpleNode, token.getAst());
	}

	private Iterator<ASTEntry> createModuleAndGetImports(String strDoc, Class<Import> classToGet) throws Exception {
		Document document = new Document(strDoc);
		SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc(MODULE_NAME, null, document, null, 0);
		
		
		EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
		module.getAst().accept(visitor);
		Iterator<ASTEntry> iterator = visitor.getIterator(classToGet);
		return iterator;
	}
}
