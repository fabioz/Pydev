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
package org.python.pydev.ast.codecompletion.revisited;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.IASTManagerObserver;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.IterTokenEntry;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.TokensList;
import org.python.pydev.core.structure.CompletionRecursionException;
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
    private TokensList comps = null;

    /**
     * @return Returns the manager.
     */
    private ICodeCompletionASTManager getManager() {
        return nature.getAstManager();
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
        comps = getComps(true);
        checkExpected(1);
        assertEquals("makeit", comps.getFirst().getRepresentation());

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

        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        assertIsIn("AssertionError", comps);

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
        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        assertIsIn("meth1", comps);
        assertIsIn("AssertionError", comps);

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
        assertIsIn("__name__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("unittest", comps);
        assertIsIn("Classe1", comps);
        assertIsIn("Test", comps);
        assertIsIn("AssertionError", comps);

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
        comps = getComps(true);
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
        comps = getComps(true);
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
        comps = getComps(true);
        assertIsIn("first", comps);
        assertIsIn("last", comps);
        assertIsIn("content", comps);

    }

    private void checkExpected(String... expected) {
        for (String s : expected) {
            assertIsIn(s, comps);
        }
    }

    private void checkExpected(int expected) {
        FastStringBuffer buf = new FastStringBuffer(40 * comps.size());
        for (IterTokenEntry entry : comps) {
            IToken t = entry.getToken();
            buf.append(t.getRepresentation());
            buf.append("\n");
        }
        String msg = "Expected " + expected +
                ". Found: " + buf.toString();
        assertEquals(msg, expected, comps.size());
    }

    private TokensList getComps() {
        return getComps(false);
    }

    private TokensList getComps(boolean excludeStartingWithUnder) {
        try {
            TokensList completionsForToken = getManager().getCompletionsForToken(doc, state);
            HashMap<String, IToken> map = new HashMap<String, IToken>();
            for (IterTokenEntry entry : completionsForToken) {
                IToken iToken = entry.getToken();
                if (excludeStartingWithUnder && iToken.getRepresentation().startsWith("_")) {
                    continue;
                }
                map.put(iToken.getRepresentation(), iToken);
            }

            return new TokensList(map.values().toArray(new IToken[map.size()]));
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
        comps = getComps(true);
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
        comps = getComps(true);
        checkExpected(1);
        assertIsIn("test1", comps);

    }

    public void testLocals() {
        token = "";
        line = 2;
        sDoc = "contentsCopy = applicationDb.getContentsCopy()\n" +
                "database.Database.fromContentsCopy(self, cont)";
        col = sDoc.length() - 3;
        doc = new Document(sDoc);
        state = new CompletionState(line, col, token, nature, "");
        comps = getComps();
        assertIsIn("contentsCopy", comps);
        assertIsIn("__file__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("__name__", comps);
        assertIsIn("AttributeError", comps);
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
        assertIsIn("__file__", comps);
        assertIsIn("__name__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("met", comps);
        assertIsIn("AssertionError", comps);

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
        assertIsIn("__name__", comps);
        assertIsIn("__dict__", comps);
        assertIsIn("__file__", comps);
        assertIsIn("par1", comps);
        assertIsIn("par2", comps);
        assertIsIn("self", comps);
        assertIsIn("C", comps);
        assertIsIn("AssertionError", comps);

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
        checkExpected("__name__", "__file__", "__dict__", "par1", "loc1", "par2", "self", "C", "AssertionError");

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
        checkExpected("__name__", "__file__", "__dict__", "par1", "loc1", "par2", "self", "C", "AssertionError");
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
            restoreProjectPythonPath(false, TestDependent.TEST_PYSRC_TESTING_LOC, "TestProject");
            assertTrue(trackingObserver.called);
        } finally {
            ExtensionHelper.testingParticipants = oldExtensions;
        }
    }

    /**
     * @param string
     * @param comps
     */
    public static void assertIsIn(String string, TokensList comps) {
        StringBuffer buffer = new StringBuffer("Available: \n");
        boolean found = false;
        for (IterTokenEntry entry : comps) {
            IToken t = entry.getToken();
            String rep = t.getRepresentation();
            if (string.equals(rep)) {
                found = true;
            }
            buffer.append(rep);
            buffer.append("\n");
        }

        assertTrue("The searched token (" + string +
                ") was not found in the completions. " + buffer, found);
    }

}
