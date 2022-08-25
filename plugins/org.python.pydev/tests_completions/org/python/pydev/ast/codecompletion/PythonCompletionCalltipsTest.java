/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.ast.codecompletion;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.ast.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.PyContextInformationValidator;
import org.python.pydev.editor.codecompletion.proposals.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.proposals.PyLinkedModeCompletionProposal;
import org.python.pydev.editor.codefolding.IPyCalltipsContextInformation;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

public class PythonCompletionCalltipsTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            PythonCompletionCalltipsTest test = new PythonCompletionCalltipsTest();
            test.setUp();
            test.testCalltips3a();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PythonCompletionCalltipsTest.class);
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
        codeCompletion = new PyCodeCompletion();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
    }

    public void testCalltips1() throws Exception {
        String s;
        s = "" +
                "GLOBAL_VAR = 10\n" + //this variable should not show in the return
                "def m1(a, b):\n" +
                "    print a, b\n" +
                "\n" +
                "m1(a, b)"; //we'll request a completion inside the parentesis to check for calltips. For calltips, we
        //should get the activation token as an empty string and the qualifier as "m1",
        //so, the completion that should return is "m1(a, b)", with the information context
        //as "a, b".
        //
        //
        //
        //The process of getting the completions actually starts at:
        //org.python.pydev.ast.codecompletion.PyCodeCompletion#getCodeCompletionProposals

        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 5, -1, new String[] {});

        if (false) { //make true to see which proposals were returned.
            for (ICompletionProposalHandle proposal : proposals) {
                System.out.println(proposal.getDisplayString());
            }
        }

        assertEquals(1, proposals.length); //now, here's the first part of the failing test: we can only have one
                                           //returned proposal: m1(a, b)

        //check if the returned proposal is there
        ICompletionProposalHandle prop = proposals[0];
        assertEquals("m1(a, b)", prop.getDisplayString());
        PyCompletionProposal p4 = (PyCompletionProposal) prop;
        assertTrue(p4.isAutoInsertable());
        assertEquals(IPyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO, p4.onApplyAction);

        //the display string for the context 'context' and 'information' should be the same
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) prop.getContextInformation();
        assertEquals("a, b", contextInformation.getContextDisplayString());
        assertEquals("a, b", contextInformation.getInformationDisplayString());

        //now, this proposal has one interesting thing about it: it should actually not change the document
        //where it is applied (it is there just to show the calltip).
        //
        //To implement that, when we see that it is called inside some parenthesis, we should create a subclass of
        //PyCompletionProposal that will have its apply method overriden, so that nothing happens here (the calltips will
        //still be shown)
        Document doc = new Document();
        prop.apply(doc);
        assertEquals("", doc.get());
    }

    public void testCalltips2() throws Exception {
        String s;
        s = "" +
                "GLOBAL_VAR = 10\n" +
                "def m1(a, b):\n" +
                "    print a, b\n" +
                "def m1Other(a, b):\n" + //this one should not show, as we're returning it for calltip purposes only
                "    print a, b\n" +
                "\n" +
                "m1()";
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] { "m1(a, b)" });
        assertEquals(1, proposals.length);
    }

    public void testCalltips3() throws Exception {
        String s;
        s = "" +
                "def m1(a, b):\n" +
                "    print a, b\n" +
                "m1()";
        PyContextInformationValidator validator = new PyContextInformationValidator();
        int requestOffset = s.length() - 1;
        ICompletionProposalHandle[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length);
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) proposals[0]
                .getContextInformation();

        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(0));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertFalse(validator.isContextInformationValid(requestOffset + 1));
        assertEquals("a, b", contextInformation.getContextDisplayString());
    }

    public void testCalltips3a() throws Exception {
        String s;
        s = "" +
                "def m1(a, b, c):\n" + //yes, this is no longer supported (and this construct is rarely used).
                "    print(a, b, c)\n" +
                "m1()";
        PyContextInformationValidator validator = new PyContextInformationValidator();
        int requestOffset = s.length() - 1;
        ICompletionProposalHandle[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length);
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) proposals[0]
                .getContextInformation();

        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(0));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertFalse(validator.isContextInformationValid(requestOffset + 1));
        assertEquals("a, b, c", contextInformation.getContextDisplayString());
    }

    public void testCalltips4() throws Exception {
        String s;
        s = "" +
                "def m1(a, b):\n" +
                "    print a, b\n" +
                "m1(a,b)";
        int requestOffset = s.length() - 4;
        ICompletionProposalHandle[] proposals = requestCompl(s, requestOffset, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyContextInformationValidator validator = new PyContextInformationValidator();
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) proposals[0]
                .getContextInformation();

        validator.install(contextInformation, new Document(s), requestOffset);
        assertFalse(validator.isContextInformationValid(requestOffset - 1));
        assertTrue(validator.isContextInformationValid(requestOffset));
        assertTrue(validator.isContextInformationValid(requestOffset + 3));
        assertFalse(validator.isContextInformationValid(requestOffset + 4));
    }

    public void testCalltips5() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, a, b):\n" +
                "        pass\n" +
                "    \n"
                +
                "TestCase(%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(1, proposals.length);
        PyCompletionProposal p = (PyCompletionProposal) proposals[0];
        assertEquals("TestCase(a, b)", p.getDisplayString());

        Document document = new Document(s);
        p.apply(document);
        assertEquals(StringUtils.format(s0, "a, b"), document.get());
    }

    public void testCalltips6() throws Exception {
        String s = "from extendable import calltips\n" +
                "calltips.";

        requestCompl(s, s.length(), 1,
                new String[] { "method1(a, b)" });
    }

    public void testCalltips6a() throws Exception {
        String s = "from extendable import calltips\n" +
                "calltips._";

        requestCompl(s, s.length(), 4,
                new String[] { "__file__", "__dict__", "__name__", "__path__" });
    }

    public void testCalltips7() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, param1, param2):\n" +
                "        pass\n"
                +
                "    \n" +
                "TestCase(para%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(2, proposals.length);
        PyCompletionProposal param1Proposal = (PyCompletionProposal) assertContains("param1=", proposals);
        assertContains("param2=", proposals);

        Document document = new Document(s);
        param1Proposal.apply(document);
        assertEquals(StringUtils.format(s0, "m1="), document.get());
    }

    public void testCalltips7a() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, *, param1: int, param2: int=2):\n" +
                "        pass\n"
                +
                "    \n" +
                "TestCase(para%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(2, proposals.length);

        PyCompletionProposal param1Proposal = (PyCompletionProposal) assertContains("param1=", proposals);
        assertTrue("Found: " + param1Proposal.getAdditionalProposalInfo(),
                param1Proposal.getAdditionalProposalInfo().indexOf("param: param1:int") == 0);

        PyCompletionProposal param2Proposal = (PyCompletionProposal) assertContains("param2=", proposals);
        assertTrue("Found: " + param2Proposal.getAdditionalProposalInfo(),
                param2Proposal.getAdditionalProposalInfo().indexOf("param: param2:int=2") == 0);

        Document document = new Document(s);
        param1Proposal.apply(document);
        assertEquals(StringUtils.format(s0, "m1="), document.get());
    }

    public void testSortCalltips1() throws Exception {
        //should keep the variables from the __builtins__ in this module
        String s = "[].sort(r)";
        requestCompl(s, s.length() - 1, -1, new String[] { "reverse=" });
    }

    public void testSortCalltips2() throws Exception {
        String s = "[].sort()";
        ICompletionProposalHandle[] completions = requestCompl(s, s.length() - 1, 1, new String[] { "sort()" });
        PyLinkedModeCompletionProposal comp = (PyLinkedModeCompletionProposal) completions[0];
        assertEquals(comp.getOnApplyAction(), IPyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS);
    }

    public void testCalltips8() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, param1, param2):\n" +
                "        pass\n"
                +
                "    \n" +
                "TestCase(param1=10, para%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(2, proposals.length);
        PyCompletionProposal paramProposal = (PyCompletionProposal) assertContains("param1=", proposals);
        paramProposal = (PyCompletionProposal) assertContains("param2=", proposals);

        Document document = new Document(s);
        paramProposal.apply(document);
        assertEquals(StringUtils.format(s0, "m2="), document.get());
    }

    public void testCalltips8a() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, param1, param2):\n" +
                "        pass\n"
                +
                "    \n" +
                "TestCase(param1=10, para%s=20)";
        String s = StringUtils.format(s0, "m3");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 7, -1, new String[] {});
        assertEquals(2, proposals.length);
        PyLinkedModeCompletionProposal paramProposal = (PyLinkedModeCompletionProposal) assertContains("param1=",
                proposals);
        paramProposal = (PyLinkedModeCompletionProposal) assertContains("param2=", proposals);

        Document document = new Document(s);
        paramProposal.setLen(2); //only the 'm3'
        paramProposal.applyOnDoc(paramProposal.getReplacementOffset() + 4, true, document, 4, '\0');
        assertEquals(StringUtils.format(s0, "m2"), document.get());
    }

    public void testCalltips9() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, param1, param2, *args, **kwargs):\n"
                +
                "        pass\n" +
                "    \n" +
                "TestCase(%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, -1, new String[] {});
        assertEquals(1, proposals.length);
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) proposals[0]
                .getContextInformation();
        assertEquals("self, param1, param2, *args, **kwargs", contextInformation.getContextDisplayString());
        assertEquals(108, contextInformation.getShowCalltipsOffset());

        Document document = new Document(s);
        proposals[0].apply(document);
        assertEquals(StringUtils.format(s0, "param1, param2"), document.get());
    }

    public void testCalltips10() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    def __init__(self, param1, param2, *args, **kwargs):\n"
                +
                "        pass\n" +
                "    \n" +
                "TestCase(param1=10, p)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 2, -1, new String[] {});
        assertEquals(1, proposals.length);
        IPyCalltipsContextInformation contextInformation = (IPyCalltipsContextInformation) proposals[0]
                .getContextInformation();
        assertEquals("self, param1, param2, *args, **kwargs", contextInformation.getContextDisplayString());
        assertEquals(108, contextInformation.getShowCalltipsOffset());

        Document document = new Document(s);
        proposals[0].apply(document);
        assertEquals(StringUtils.format(s0, "param1, param2"), document.get());
    }

    public void testCalltips11() throws Exception {
        String s0 = "class TestCase(object):\n" +
                "    \n" +
                "    def M1(self, kkk, xxx):\n" +
                "        pass\n" +
                "\n"
                +
                "    def __init__(self):\n" +
                "        self.M1(k%s)";

        String s = StringUtils.format(s0, "");
        ICompletionProposalHandle[] proposals = requestCompl(s, s.length() - 1, 3,
                new String[] { "kkk=", "KeyboardInterrupt", "KeyError" });
        Document document = new Document(s);
        proposals[0].apply(document);
        assertEquals(StringUtils.format(s0, "kk="), document.get());
    }

    public void testMakeArgsForDocumentReplacement() throws Exception {

        FastStringBuffer temp = new FastStringBuffer();
        FastStringBuffer result = new FastStringBuffer();
        assertEquals("", AbstractPyCodeCompletion.makeArgsForDocumentReplacement("", result, temp));
        assertEquals("()", AbstractPyCodeCompletion.makeArgsForDocumentReplacement("()", result, temp));
        assertEquals("(a, b, c)", AbstractPyCodeCompletion.makeArgsForDocumentReplacement("(a, b, c)", result, temp));
        assertEquals("((a, b), c)",
                AbstractPyCodeCompletion.makeArgsForDocumentReplacement("((a, b), c)", result, temp));
        assertEquals("(a, b)",
                AbstractPyCodeCompletion.makeArgsForDocumentReplacement("(object a, object b)", result, temp));
        assertEquals("(a, b)", AbstractPyCodeCompletion.makeArgsForDocumentReplacement("(o\ta,\to\tb)", result, temp));
        assertEquals("(a, *b, **c)",
                AbstractPyCodeCompletion.makeArgsForDocumentReplacement("(o\ta,\to\t* b, o  **  c)", result, temp));

    }

    public void testCalltipsArgs() throws Exception {
        assertEquals("()", AbstractPyCodeCompletion.getArgs("", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("()", AbstractPyCodeCompletion.getArgs("(", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("()", AbstractPyCodeCompletion.getArgs(")", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("(a, b)", AbstractPyCodeCompletion.getArgs("(a, b)", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("(a, b)", AbstractPyCodeCompletion.getArgs("(self, a, b)", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("(a, b)", AbstractPyCodeCompletion.getArgs("(cls, a, b)", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("(clsParam, a, b)", AbstractPyCodeCompletion.getArgs("(clsParam, a, b)", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
        assertEquals("(selfParam, a, b)", AbstractPyCodeCompletion.getArgs("(selfParam, a, b)", IToken.TYPE_FUNCTION,
                ICompletionState.LookingFor.LOOKING_FOR_INSTANCED_VARIABLE));
    }
}
