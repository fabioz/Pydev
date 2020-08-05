/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.string.StringUtils;

public class PythonCompletionStringsTest extends CodeCompletionTestsBase {
    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            PythonCompletionStringsTest test = new PythonCompletionStringsTest();
            test.setUp();
            test.test1();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionStringsTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(false);
        codeCompletion = new PyStringCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    public void test1() throws Exception {
        String doc = "" +
                "def m1(foo, bar):\n" +
                "   '''\n" +
                "   @param \n" +
                "   '''"; //<- bring tokens that are already defined in the local

        String[] toks = new String[] { "bar", "foo" };
        requestCompl(doc, doc.length() - "\n   '''".length(), toks.length, toks); //request right after the params
    }

    public void test2() throws Exception {
        String doc = "" +
                "def m1(foo, bar):\n" +
                "   '''\n" +
                "   @\n" +
                "   '''"; //<- bring tokens that are already defined in the local

        String[] toks = new String[] { "param", "type" };
        requestCompl(doc, doc.length() - "\n   '''".length(), -1, toks); //request right after the params

    }

    public void test3() throws Exception {
        String doc = "" +
                "def m1(foo, bar):\n" +
                "   '''\n" +
                "   @para\n" +
                "   '''"; //<- bring tokens that are already defined in the local

        String[] toks = new String[] { "param" };
        requestCompl(doc, doc.length() - "\n   '''".length(), -1, toks); //request right after the params
    }

    public void test3a() throws Exception {
        String doc = "" +
                "def m1(foo, bar):\n" +
                "   '''\n" +
                "   :para\n" +
                "   '''"; //<- bring tokens that are already defined in the local

        String[] toks = new String[] { "param" };
        requestCompl(doc, doc.length() - "\n   '''".length(), -1, toks); //request right after the params
    }

    public void test4() throws Exception {
        String doc = "" +
                "class foo(object):\n" +
                "    \n" +
                "    def m1(self, create2, bar2):\n" +
                "        pass\n"
                +
                "    def m1(self, create, bar):\n" +
                "        '''\n" +
                "            @param cr\n" +
                "        '''\n";

        String[] toks = new String[] { "create" };
        requestCompl(doc, doc.length() - "\n        '''\n".length(), 1, toks); //request right after the params
    }

    public void test4a() throws Exception {
        String doc = "" +
                "class foo(object):\n" +
                "    \n" +
                "    def m1(self, create2, bar2):\n" +
                "        pass\n"
                +
                "    def m1(self, create, bar):\n" +
                "        '''\n" +
                "            :param cr\n" +
                "        '''\n";

        String[] toks = new String[] { "create" };
        requestCompl(doc, doc.length() - "\n        '''\n".length(), 1, toks); //request right after the params
    }

    public void testCompletionInsideFstrings() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "print(f'Bar is: {b.ba}')";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 3, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings2() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "print(f'Bar is: {b.ba')";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 3, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings3() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "print(f\"Bar is: {b.ba}\")";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 3, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings4() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "print(f\"Bar is: {b.ba\")";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 3, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings5() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "s = f'''\n" +
                "Bar is:\n" +
                "{b.ba}\n" +
                "'''";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 5, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings6() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "s = f'''\n" +
                "Bar is:\n" +
                "{b.ba\n" +
                "'''";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 5, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings7() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "s = f\"\"\"\n" +
                "Bar is:\n" +
                "{b.ba}\n" +
                "\"\"\"";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 5, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

    public void testCompletionInsideFstrings8() throws Exception {
        String s;
        String original = "class Bar(object):\n" +
                "\n" +
                "    def bar(self):\n" +
                "        pass\n" +
                "\n" +
                "b = Bar()\n" +
                "s = f\"\"\"\n" +
                "Bar is:\n" +
                "{b.ba\n" +
                "\"\"\"";
        s = StringUtils.format(original, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 5, -1, new String[] {});
        assertEquals(1, proposals.length);
        assertEquals("bar()", proposals[0].getDisplayString());
    }

}
