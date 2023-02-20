/**
 * Copyright (c) 2019 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.ast.cython.GenCythonAstImpl;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

/**
 * @author Fabio
 */
public class PyParserCythonTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        try {
            PyParserCythonTest test = new PyParserCythonTest();
            test.setUp();
            test.testCythonParsing();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyParserCythonTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        GenCythonAstImpl.IN_TESTS = true;
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
    }

    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        GenCythonAstImpl.IN_TESTS = false;
    }

    public void testCythonParsing() throws Exception {
        String str = "" +
                "import os\n" +
                "";
        Document doc = new Document(str);
        ParseOutput parseOutput = PyParser
                .reparseDocument(new ParserInfo(doc, IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON,
                        true, null));
        assertTrue(parseOutput.isCython);
        assertNull(parseOutput.error);
    }

}
