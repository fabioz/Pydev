/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 1, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion.revisited;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.IASTManagerObserver;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * Tests here have no dependency on the pythonpath.
 * 
 * @author Fabio Zadrozny
 */
public class ASTManagerTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {
        CompiledModule.COMPILED_MODULES_ENABLED = false;

        try {
            ASTManagerTest test = new ASTManagerTest();
            test.setUp();
            test.testCompletion();
            test.tearDown();

            junit.textui.TestRunner.run(ASTManagerTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private ICompletionState state;
    private String token;
    private int line;
    private int col;
    private String sDoc;
    private Document doc;
    private IToken[] comps = null;

    /**
     * @return Returns the manager.
     */
    private ICodeCompletionASTManager getManager() {
        return (ICodeCompletionASTManager) nature.getAstManager();
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        super.restorePythonPath(false);
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        PydevPlugin.setBundleInfo(null);
    }

    public void testCompletion() {
        token = "C";
        line = 6;
        col = 11;
        sDoc = "" +
                "class C:             \n" +
                "                     \n" +
                "    def makeit(self):\n"
                +
                "        pass         \n" +
                "                     \n" +
                "class D(C.:          \n"
                +
                "                     \n" +
                "    def a(self):     \n" +
                "        pass         \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(1);
        assertEquals("makeit", comps[0].getRepresentation());

        sDoc = "" +
                "import unittest       \n" +
                "                      \n" +
                "class Classe1:        \n"
                +
                "                      \n" +
                "    def makeit(self): \n" +
                "        self.makeit   \n"
                +
                "                      \n" +
                "                      \n" +
                "class Test(unit       \n";

        line = 8;
        col = 16;
        token = "";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();

        checkExpected(6);
        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);

        sDoc = "" +
                "import unittest       \n" +
                "                      \n" +
                "class Classe1:        \n"
                +
                "                      \n" +
                "    def makeit(self): \n" +
                "        self.makeit   \n"
                +
                "                      \n" +
                "                      \n" +
                "class Test(unit       \n"
                +
                "                      \n" +
                "def meth1():          \n" +
                "    pass              \n";

        line = 8;
        col = 16;
        token = "";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(7);
        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        assertIsIn("meth1", comps);

        sDoc = "" +
                "import unittest       \n" +
                "                      \n" +
                "class Classe1:        \n"
                +
                "                      \n" +
                "    def makeit(self): \n" +
                "        self.makeit   \n"
                +
                "                      \n" +
                "                      \n" +
                "class Test(unit       \n"
                +
                "                      \n" +
                "    def meth1():      \n" +
                "        pass          \n";

        line = 8;
        col = 16;
        token = "";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(6);
        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);

        sDoc = "" +
                "class Classe1:       \n" +
                "                     \n" +
                "    def foo(self):   \n"
                +
                "        ignoreThis=0 \n" +
                "        self.a = 1   \n" +
                "        self.        \n"
                +
                "                     \n";

        line = 6;
        col = 13;
        token = "Classe1";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(2);
        assertIsIn("a", comps);
        assertIsIn("foo", comps);

        sDoc = "" +
                "class Classe1:       \n" +
                "                     \n" +
                "    def foo(self):   \n"
                +
                "        self.a = 2   \n" +
                "                     \n" +
                "    test = foo       \n"
                +
                "                     \n" +
                "Classe1.             \n";

        line = 8;
        col = 9;
        token = "Classe1";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(3);
        assertIsIn("foo", comps);
        assertIsIn("a", comps);
        assertIsIn("test", comps);

        sDoc = "" +
                "class LinkedList:                      \n" +
                "    def __init__(self,content='Null'): \n"
                +
                "        if not content:                \n" +
                "            self.first=content         \n"
                +
                "            self.last=content          \n" +
                "        else:                          \n"
                +
                "            self.first='Null'          \n" +
                "            self.last='Null'           \n"
                +
                "        self.content=content           \n" +
                "        self.                          \n";

        line = 9;
        col = 9;
        token = "LinkedList";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        assertIsIn("first", comps);
        assertIsIn("last", comps);
        assertIsIn("content", comps);

    }

    private void checkExpected(String... expected) {
        for (String s : expected) {
            assertIsIn(s, comps);
        }
        List<String> asList = Arrays.asList(expected);
        for (IToken t : comps) {
            assertContains(asList, t.getRepresentation());
        }
        checkExpected(expected.length);

    }

    private void checkExpected(int expected) {
        FastStringBuffer buf = new FastStringBuffer(40 * comps.length);
        for (IToken t : comps) {
            buf.append(t.getRepresentation());
            buf.append(", ");
        }
        String msg = "Expected " + expected +
                ". Found: " + buf.toString();
        assertEquals(msg, expected, comps.length);
    }

    private IToken[] getComps() {
        try {
            IToken[] completionsForToken = getManager().getCompletionsForToken(doc, state);
            HashMap<String, IToken> map = new HashMap<String, IToken>();
            for (IToken iToken : completionsForToken) {
                map.put(iToken.getRepresentation(), iToken);
            }

            return map.values().toArray(new IToken[map.size()]);
        } catch (CompletionRecursionException e) {
            throw new RuntimeException(e);
        }
    }

    public void testRecursion() {
        token = "B";
        line = 0;
        col = 0;
        sDoc = "" +
                "class A(B):pass          \n" +
                "class B(A):pass          \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(0); //no tokens returned

    }

    public void testRelative() {
        super.restorePythonPath(false);
        token = "Test1";
        line = 1;
        col = 0;
        sDoc = "" +
                "from testlib.unittest.relative import Test1 \n" +
                "\n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(1);
        assertIsIn("test1", comps);

    }

    public void testLocals() {
        token = "";
        line = 2;
        sDoc = "" +
                "contentsCopy = applicationDb.getContentsCopy()\n"
                +
                "database.Database.fromContentsCopy(self, cont)";
        col = sDoc.length() - 3;
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(4);
        assertIsIn("contentsCopy", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("__name__", comps);
    }

    public void testLocals2() {
        token = "";
        line = 2;
        col = 10;
        sDoc = "" +
                "def met(par1, par2):          \n" +
                "    print                     \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(6);
        assertIsIn("__file__", comps);
        assertIsIn("__name__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("met", comps);

        token = "";
        line = 3;
        col = 13;
        sDoc = "" +
                "class C:                         \n" +
                "    def met(self, par1, par2):   \n"
                +
                "        print                    \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected(7);
        assertIsIn("__name__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("self", comps);
        assertIsIn("C", comps);

        token = "";
        line = 4;
        col = 13;
        sDoc = "" +
                "class C:                         \n" +
                "    def met(self, par1, par2):   \n"
                +
                "        loc1 = 10                \n" +
                "        print                    \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected("__name__", "__file__", "__dict__", "par1", "loc1", "par2", "self", "C");

        token = "";
        line = 4;
        col = 13;
        sDoc = "" +
                "class C:                         \n" +
                "    def met(self, par1, par2):   \n"
                +
                "        loc1 = 10                \n" +
                "        print                    \n"
                +
                "        ignoreLineAfter = 5      \n";
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        checkExpected("__name__", "__file__", "__dict__", "par1", "loc1", "par2", "self", "C");
    }

    private static class ManagerObserver implements IASTManagerObserver {

        boolean called;

        @Override
        public void notifyASTManagerAttached(ICodeCompletionASTManager manager) {
            called = true;
        }

    }

    /**
     * Check that registered observers are called when ASTManager is 
     * associated with project.
     */
    public void testManagerObserver() {
        Map<String, List<Object>> oldExtensions = ExtensionHelper.testingParticipants;
        try {
            ManagerObserver trackingObserver = new ManagerObserver();
            Map<String, List<Object>> extensions = new HashMap<String, List<Object>>();
            extensions
                    .put(ExtensionHelper.PYDEV_MANAGER_OBSERVER, Collections.<Object> singletonList(trackingObserver));

            ExtensionHelper.testingParticipants = extensions;
            restoreProjectPythonPath(false, TestDependent.TEST_PYSRC_LOC, "TestProject");
            assertTrue(trackingObserver.called);
        } finally {
            ExtensionHelper.testingParticipants = oldExtensions;
        }
    }

    /**
     * @param string
     * @param comps
     */
    public static void assertIsIn(String string, IToken[] comps) {
        StringBuffer buffer = new StringBuffer("Available: \n");
        boolean found = false;
        for (int i = 0; i < comps.length; i++) {
            String rep = comps[i].getRepresentation();
            if (string.equals(rep)) {
                found = true;
            }
            buffer.append(rep);
            buffer.append("\n");
        }

        assertTrue("The searched token (" + string +
                ")was not found in the completions. " + buffer, found);
    }

}
