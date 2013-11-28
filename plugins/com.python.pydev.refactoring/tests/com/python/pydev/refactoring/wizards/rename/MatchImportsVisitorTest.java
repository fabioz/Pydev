package com.python.pydev.refactoring.wizards.rename;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.PythonNatureStub;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public class MatchImportsVisitorTest extends TestCase {

    public void testMatchImports() throws Exception {
        Document doc = new Document(""
                + "from a.b.c.d import e\n" //rename a.b.c
                + "from a.b.c import d\n" //rename a.b.c
                + "from a.b import c\n" //rename a.b.c
                + "from a.b import g, c, f\n" //rename a.b.c (but not g nor f)
                + "from a import b\n"
                + "from a import *\n"
                + "from a.b.c import *\n" //rename a.b.c with wild import
                + "from a.b.c.d import *\n" //rename a.b.c with wild import
                + "");
        IPythonNature nature = new PythonNatureStub();
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, nature));
        SourceModule module = (SourceModule) AbstractModule.createModule((SimpleNode) obj.ast, null, "z");

        MatchImportsVisitor visitor = new MatchImportsVisitor(nature, "a.b.c", module, null);
        module.getAst().accept(visitor);
        assertEquals(visitor.importFromsMatchingOnAliasPart.size(), 2);
        assertEquals(visitor.importFromsMatchingOnModulePart.size(), 4);
        assertEquals(visitor.occurrences.size(), 6);
    }

    public void testMatchImports2() throws Exception {
        Document doc = new Document(""
                + "import a.b.c.d\n" //rename a.b.c
                + "import a.b.c\n" //rename a.b.c
                + "import a.b\n"
                + "");
        IPythonNature nature = new PythonNatureStub();
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, nature));
        SourceModule module = (SourceModule) AbstractModule.createModule((SimpleNode) obj.ast, null, "z");

        MatchImportsVisitor visitor = new MatchImportsVisitor(nature, "a.b.c", module, null);
        module.getAst().accept(visitor);
        assertEquals(visitor.importsMatchingOnAliasPart.size(), 2);
        assertEquals(visitor.occurrences.size(), 2);
    }

    public void testMatchRelativeImports() throws Exception {
        //Note: on Python 2.x, we should get the from b import c unless from __future__ import absolute_import is used.
        //In Python 3.x, we'll only get it when actually marked as a relative import (with leading dots).
        Document doc = new Document(""
                + "from __future__ import absolute_import\n"
                + "from b import c\n"
                + "from .b import c\n" //rename a.b.c
                + "from ..a.b import c\n" //rename a.b.c
                + "");
        IPythonNature nature = new PythonNatureStub() {
            @Override
            public int getGrammarVersion() {
                return IPythonNature.GRAMMAR_PYTHON_VERSION_2_7;
            }
        };
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(doc, nature));
        SourceModule module = (SourceModule) AbstractModule.createModule((SimpleNode) obj.ast, null, "a.g");

        MatchImportsVisitor visitor = new MatchImportsVisitor(nature, "a.b.c", module, null);
        module.getAst().accept(visitor);
        assertEquals(2, visitor.importFromsMatchingOnAliasPart.size());
        assertEquals(2, visitor.occurrences.size());
    }
}
