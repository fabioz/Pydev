/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Nov 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited;

import java.util.Arrays;
import java.util.HashSet;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TokensList;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

import junit.framework.TestCase;

/**
 * @author Fabio Zadrozny
 */
public class ModuleTest extends TestCase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            ModuleTest test = new ModuleTest();
            test.setUp();
            test.testMod2();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(ModuleTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testMod1() {
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(getDoc1()),
                IPythonNature.GRAMMAR_PYTHON_VERSION_3_5, null));
        SimpleNode n = (SimpleNode) obj.ast;
        IModule module = AbstractModule.createModule(n, null);

        TokensList globalTokens = module.getGlobalTokens();
        assertEquals(8, globalTokens.size()); //C c D d a __file__ __name__
        compareReps(globalTokens, "__file__ __name__ __dict__ C c D d a");

        TokensList wildImportedModules = module.getWildImportedModules();
        assertEquals(1, wildImportedModules.size()); //m4
        compareReps(wildImportedModules, "m4");

        TokensList tokenImportedModules = module.getTokenImportedModules();
        assertEquals(5, tokenImportedModules.size()); //a1, xx, yy, aa
        compareReps(tokenImportedModules, "a1 xx yy aa m3");

        assertEquals("docstring for module", module.getDocString());

    }

    public void testMod2() {
        String doc = "" +
                "def method(a, b):\n" +
                "    pass\n" +
                "other = method\n" +
                "";
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(doc),
                IPythonNature.GRAMMAR_PYTHON_VERSION_3_5, null));
        SimpleNode n = (SimpleNode) obj.ast;
        IModule module = AbstractModule.createModule(n, null);

        TokensList globalTokens = module.getGlobalTokens();
        assertEquals(5, globalTokens.size());
        compareReps(globalTokens, "__file__ __name__ __dict__ method other");
        int found = 0;
        for (IterTokenEntry entry : globalTokens) {
            IToken t = entry.getToken();
            if (t.getRepresentation().equals("method") || t.getRepresentation().equals("other")) {
                assertEquals("( a, b )", t.getArgs());
                found += 1;
            }
        }
        assertEquals(2, found);
    }

    public void testMod3() {
        String doc = "" +
                "def method(a, b):\n" +
                "    pass\n" +
                "other = another = method\n" +
                "";
        ParseOutput obj = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(doc),
                IPythonNature.GRAMMAR_PYTHON_VERSION_3_5, null));
        SimpleNode n = (SimpleNode) obj.ast;
        IModule module = AbstractModule.createModule(n, null);

        TokensList globalTokens = module.getGlobalTokens();
        assertEquals(6, globalTokens.size());
        compareReps(globalTokens, "__file__ __name__ __dict__ method other another");
        int found = 0;
        for (IterTokenEntry entry : globalTokens) {
            IToken t = entry.getToken();
            if (t.getRepresentation().equals("method") || t.getRepresentation().equals("other")
                    || t.getRepresentation().equals("another")) {
                assertEquals("( a, b )", t.getArgs());
                found += 1;
            }
        }
        assertEquals(3, found);
    }

    /**
     * @param globalTokens
     * @param string
     */
    private void compareReps(TokensList globalTokens, String string) {
        String[] strings = string.split(" ");
        HashSet<String> s1 = new HashSet<String>();
        s1.addAll(Arrays.asList(strings));

        HashSet<String> s2 = new HashSet<String>();
        for (IterTokenEntry entry : globalTokens) {
            IToken t = entry.getToken();
            s2.add(t.getRepresentation());
        }
        assertEquals(s1, s2);
    }

    public String getDoc1() {
        //damn, I really miss python when writing this...
        return "'''" +
                "docstring for module"
                +
                "'''\n"
                +
                "from m1 import a1\n"
                +
                "from mm import aa as xx , gg as yy\n"
                + //this is only 1 import, but it has 2 'aliasType'.
                "from m2 import a2 as aa\n" +
                "import m3\n" +
                "from m4 import *\n" +
                "\n" +
                "class C: \n" +
                "    '''\n"
                +
                "    this is the class c\n" +
                "    '''\n" +
                "    pass\n" +
                "\n" +
                "c = C()\n" +
                "\n"
                +
                "class D:pass\n" +
                "\n" +
                "d = D()\n" +
                "\n" +
                "def a():\n" +
                "    '''\n" +
                "    method a"
                +
                "    '''\n" +
                "    return 1";
    }
}
