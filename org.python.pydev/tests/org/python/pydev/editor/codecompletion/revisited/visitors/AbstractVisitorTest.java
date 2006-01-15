package org.python.pydev.editor.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.parser.ast.Import;
import org.python.parser.ast.ImportFrom;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;

import junit.framework.TestCase;

public class AbstractVisitorTest extends TestCase {

	private String MODULE_NAME;

	public static void main(String[] args) {
		junit.textui.TestRunner.run(AbstractVisitorTest.class);
	}

	protected void setUp() throws Exception {
		super.setUp();
		MODULE_NAME = "testModule";
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
		checkIt(simpleNode, token, "os", "os", "os");
		
		token = (SourceToken) toks.get(1);
		checkIt(simpleNode, token, "os.path", "os.path", "os.path");
	}

	public void testImportCreation2() throws Exception {
	    Iterator<ASTEntry> iterator = createModuleAndGetImports("from os import path, notDefined", ImportFrom.class);
	    
	    SimpleNode simpleNode = iterator.next().node;
	    List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true);
	    assertEquals(2, toks.size());
	    
	    SourceToken token = (SourceToken) toks.get(0);
	    checkIt(simpleNode, token, "path", "os.path", "os.path");
	    
	    token = (SourceToken) toks.get(1);
	    checkIt(simpleNode, token, "notDefined", "os.notDefined", "os.notDefined");
	}
	
	public void testImportCreation3() throws Exception {
	    Iterator<ASTEntry> iterator = createModuleAndGetImports("from os import path as tt, notDefined as aa", ImportFrom.class);
	    
	    SimpleNode simpleNode = iterator.next().node;
	    List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true);
	    assertEquals(2, toks.size());
	    
	    SourceToken token = (SourceToken) toks.get(0);
	    checkIt(simpleNode, token, "tt", "os.path", "os.path");
	    
	    token = (SourceToken) toks.get(1);
	    checkIt(simpleNode, token, "aa",  "os.notDefined", "os.notDefined");
	}
	
	
	public void testImportCreation4() throws Exception {
		Iterator<ASTEntry> iterator = createModuleAndGetImports("from os.path import *", ImportFrom.class);
		
		SimpleNode simpleNode = iterator.next().node;
		List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true);
		assertEquals(1, toks.size());
		
		SourceToken token = (SourceToken) toks.get(0);
		checkIt(simpleNode, token, "os.path",  "os.path", "os.path");
	}
	
	public void testImportCreation5() throws Exception {
		Iterator<ASTEntry> iterator = createModuleAndGetImports("from os.path import *", ImportFrom.class);
		MODULE_NAME = "some.dotted.name";
		SimpleNode simpleNode = iterator.next().node;
		List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), "some.dotted.name", true);
		assertEquals(1, toks.size());
		
		SourceToken token = (SourceToken) toks.get(0);
		checkIt(simpleNode, token, "os.path",  "some.dotted.os.path", "os.path");
	}
	
	
	
	private void checkIt(SimpleNode simpleNode, SourceToken token, String rep, String relativeImport, String originalRep) {
		assertEquals(rep, token.getRepresentation());
		assertSame(simpleNode, token.getAst());
		assertEquals(relativeImport, token.getAsRelativeImport(MODULE_NAME));
		assertEquals(originalRep, token.getOriginalRep());
	}

	private Iterator<ASTEntry> createModuleAndGetImports(String strDoc, Class classToGet) throws Exception {
		Document document = new Document(strDoc);
		SourceModule module = (SourceModule) AbstractModule.createModuleFromDoc(MODULE_NAME, null, document, null, 0);
		
		
		EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
		module.getAst().accept(visitor);
		Iterator<ASTEntry> iterator = visitor.getIterator(classToGet);
		return iterator;
	}
}
