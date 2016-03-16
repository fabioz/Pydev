/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.refactoring.AbstractPyRefactoring;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.string.FastStringBuffer;

import com.python.pydev.refactoring.refactorer.Refactorer;

/**
 * @author Fabio
 *
 */
public class TddCodeGenerationQuickFixParticipantTest extends CodeCompletionTestsBase {

    public static void main(String[] args) {

        try {
            //DEBUG_TESTS_BASE = true;
            TddCodeGenerationQuickFixParticipantTest test = new TddCodeGenerationQuickFixParticipantTest();
            test.setUp();
            test.testDontCreate();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(TddCodeGenerationQuickFixParticipantTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    public void setUp() throws Exception {
        super.setUp();
        AbstractPyRefactoring.setPyRefactoring(new Refactorer());
        CompiledModule.COMPILED_MODULES_ENABLED = false;
        this.restorePythonPath(TestDependent.GetCompletePythonLib(true) +
                "|" + TestDependent.PYTHON_PIL_PACKAGES +
                "|"
                + TestDependent.TEST_PYSRC_LOC +
                "configobj-4.6.0-py2.6.egg", false);

        this.restorePythonPath(false);
        codeCompletion = new PyCodeCompletion();
        TddCodeGenerationQuickFixParticipant.onGetTddPropsError = new ICallback<Boolean, Exception>() {

            @Override
            public Boolean call(Exception e) {
                throw new RuntimeException("Error:" + Log.getExceptionStr(e));
            }
        };
        PyCodeCompletion.onCompletionRecursionException = new ICallback<Object, CompletionRecursionException>() {

            @Override
            public Object call(CompletionRecursionException e) {
                throw new RuntimeException("Recursion error:" + Log.getExceptionStr(e));
            }

        };
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    public void tearDown() throws Exception {
        CompiledModule.COMPILED_MODULES_ENABLED = true;
        super.tearDown();
        AbstractPyRefactoring.setPyRefactoring(null);
        PyCodeCompletion.onCompletionRecursionException = null;
    }

    public void testCreate() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "    pass\n" +
                "\n" +
                "def makeTestObj():\n"
                +
                "    return MyClass()\n" +
                "\n" +
                "def makeTestObj2():\n" +
                "    return makeTestObj()\n" +
                "\n"
                +
                "def testName():\n" +
                "    obj = makeTestObj2()\n" +
                "    obj.unimplementedFunction()\n" +
                "";
        TddCodeGenerationQuickFixParticipant participant = new TddCodeGenerationQuickFixParticipant();
        Document doc = new Document(s);
        List<ICompletionProposal> props = participant.getTddProps(new PySelection(doc, s.length() - 1), null, null,
                nature, null, s.length() - 1, null);
        assertContains("Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposal[0]));
    }

    public void testCreate2() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "    pass\n" +
                "def testName():\n" +
                "    obj = MyClass()\n" +
                "    obj.unimplementedFunction(a.x, 'Some comment in ticket.')\n" +
                "";
        TddCodeGenerationQuickFixParticipant participant = new TddCodeGenerationQuickFixParticipant();
        Document doc = new Document(s);
        List<ICompletionProposal> props = participant.getTddProps(new PySelection(doc, s.length() - 1), null, null,
                nature, null, s.length() - 1, null);
        TddRefactorCompletionInModule proposal = (TddRefactorCompletionInModule) assertContains(
                "Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposal[0]));
        //Todo: check result of apply as string is breaking!
        List<String> parametersAfterCall = proposal.getParametersAfterCall();
        FastStringBuffer createParametersList = AbstractPyCreateClassOrMethodOrField
                .createParametersList(parametersAfterCall);
        assertEquals("${x}, ${param1}", createParametersList.toString());
    }

    public void testDontCreate() throws Exception {
        String s = "" +
                "class MyClass(object):\n" +
                "\n" +
                "    def unimplementedFunction(self):\n" +
                "        pass\n"
                +
                "\n" +
                "def makeTestObj():\n" +
                "    return MyClass()\n" +
                "\n" +
                "def makeTestObj2():\n"
                +
                "    return makeTestObj()\n" +
                "\n" +
                "def testName():\n" +
                "    obj = makeTestObj2()\n"
                +
                "    obj.unimplementedFunction()\n" +
                "";
        TddCodeGenerationQuickFixParticipant participant = new TddCodeGenerationQuickFixParticipant();
        Document doc = new Document(s);
        List<ICompletionProposal> props = participant.getTddProps(new PySelection(doc, s.length() - 1), null, null,
                nature, null, s.length() - 1, null);
        assertNotContains("Create unimplementedFunction method at MyClass (__module_not_in_the_pythonpath__)",
                props.toArray(new ICompletionProposal[0]));
    }
}
