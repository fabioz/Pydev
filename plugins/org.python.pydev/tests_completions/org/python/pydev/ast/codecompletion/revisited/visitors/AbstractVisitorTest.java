/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion.revisited.visitors;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.CorePlugin;
import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.parser.visitors.scope.EasyASTIteratorVisitor;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.BundleInfoStub;

import junit.framework.TestCase;

public class AbstractVisitorTest extends TestCase {

    private String MODULE_NAME;

    public static void main(String[] args) {
        junit.textui.TestRunner.run(AbstractVisitorTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        PydevPlugin.setBundleInfo(new BundleInfoStub());
        CorePlugin.setBundleInfo(new BundleInfoStub());
        MODULE_NAME = "testModule";
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        PydevPlugin.setBundleInfo(null);
        CorePlugin.setBundleInfo(null);
    }

    public void testImportCreation1() throws Exception {
        Iterator<ASTEntry> iterator = createModuleAndGetImports("import os.path", Import.class);

        SimpleNode simpleNode = iterator.next().node;
        List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true,
                null, null);
        assertEquals(2, toks.size());

        SourceToken token = (SourceToken) toks.get(0);
        checkIt(simpleNode, token, "os", "os", "os");

        token = (SourceToken) toks.get(1);
        checkIt(simpleNode, token, "os.path", "os.path", "os.path");
    }

    public void testImportCreation2() throws Exception {
        Iterator<ASTEntry> iterator = createModuleAndGetImports("from os import path, notDefined", ImportFrom.class);

        SimpleNode simpleNode = iterator.next().node;
        List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true,
                null, null);
        assertEquals(2, toks.size());

        SourceToken token = (SourceToken) toks.get(0);
        checkIt(simpleNode, token, "path", "os.path", "os.path");

        token = (SourceToken) toks.get(1);
        checkIt(simpleNode, token, "notDefined", "os.notDefined", "os.notDefined");
    }

    public void testImportCreation3() throws Exception {
        Iterator<ASTEntry> iterator = createModuleAndGetImports("from os import path as tt, notDefined as aa",
                ImportFrom.class);

        SimpleNode simpleNode = iterator.next().node;
        List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true,
                null, null);
        assertEquals(2, toks.size());

        SourceToken token = (SourceToken) toks.get(0);
        checkIt(simpleNode, token, "tt", "os.path", "os.path");

        token = (SourceToken) toks.get(1);
        checkIt(simpleNode, token, "aa", "os.notDefined", "os.notDefined");
    }

    public void testImportCreation4() throws Exception {
        Iterator<ASTEntry> iterator = createModuleAndGetImports("from os.path import *", ImportFrom.class);

        SimpleNode simpleNode = iterator.next().node;
        List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), MODULE_NAME, true,
                null, null);
        assertEquals(1, toks.size());

        SourceToken token = (SourceToken) toks.get(0);
        checkIt(simpleNode, token, "os.path", "os.path", "os.path");
    }

    public void testImportCreation5() throws Exception {
        Iterator<ASTEntry> iterator = createModuleAndGetImports("from os.path import *", ImportFrom.class);
        MODULE_NAME = "some.dotted.name";
        SimpleNode simpleNode = iterator.next().node;
        List<IToken> toks = AbstractVisitor.makeImportToken(simpleNode, new ArrayList<IToken>(), "some.dotted.name",
                true, null, null);
        assertEquals(1, toks.size());

        SourceToken token = (SourceToken) toks.get(0);
        checkIt(simpleNode, token, "os.path", "some.dotted.os.path", "os.path");
    }

    private void checkIt(SimpleNode simpleNode, SourceToken token, String rep, String relativeImport,
            String originalRep) {
        assertEquals(rep, token.getRepresentation());
        assertSame(simpleNode, token.getAst());
        assertEquals(relativeImport, token.getAsRelativeImport(MODULE_NAME));
        assertEquals(originalRep, token.getOriginalRep());
    }

    private Iterator<ASTEntry> createModuleAndGetImports(String strDoc, Class classToGet) throws Exception {
        Document document = new Document(strDoc);
        SourceModule module = AbstractModule.createModuleFromDoc(MODULE_NAME, null, document,
                CodeCompletionTestsBase.createStaticNature(), true);

        EasyASTIteratorVisitor visitor = new EasyASTIteratorVisitor();
        module.getAst().accept(visitor);
        Iterator<ASTEntry> iterator = visitor.getIterator(classToGet);
        return iterator;
    }
}
