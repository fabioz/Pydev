/******************************************************************************
* Copyright (C) 2006-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
/*
 * Created on 25/08/2005
 *
 * @author Fabio Zadrozny
 */
package com.python.pydev.codecompletion.participant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.TestDependent;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.SharedCorePlugin;

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.codecompletion.ctxinsensitive.CtxParticipant;

public class CompletionParticipantBuiltinsTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            CompletionParticipantBuiltinsTest test = new CompletionParticipantBuiltinsTest();
            test.setUp();
            test.testImportCompletion2();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(CompletionParticipantBuiltinsTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = true;

        participant = new CtxParticipant();

        ExtensionHelper.testingParticipants = new HashMap<String, List<Object>>();

        ArrayList<Object> participants = new ArrayList<Object>(); /*IPyDevCompletionParticipant*/
        participants.add(participant);
        ExtensionHelper.testingParticipants.put(ExtensionHelper.PYDEV_COMPLETION, participants);

        codeCompletion = new PyCodeCompletion();
        this.restorePythonPath(false);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        useOriginalRequestCompl = false;
        ExtensionHelper.testingParticipants = null;
    }

    //    public void testCompletionBuiltins() throws Exception {
    ////        wx.Frame.CaptureMouse()
    ////        wx.Frame.CacheBestSize()
    ////        wx.Frame.AcceptsFocus()
    ////        wx.Frame.AcceptsFocusFromKeyboard()
    //        useOriginalRequestCompl = true;
    //        String s = "" +
    //                "def m1(a):\n" +
    //                "    a.Accepts";
    //        requestCompl(s, -1, -1, new String[]{"AcceptsFocus()", "AcceptsFocusFromKeyboard()"});
    //
    //
    //    }

    public void testImportCompletion2() throws Exception {
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }
        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            CompiledModule module = new CompiledModule("wx", this.getManager().getModulesManager());

            participant = new CtxParticipant();
            ICompletionProposal[] proposals = requestCompl("Frame", -1, -1, new String[] {});
            assertContains("Frame - wx", proposals); //Expected to fail. See: com.python.pydev.analysis.additionalinfo.builders.AdditionalInfoModulesObserver.notifyCompiledModuleCreated(CompiledModule, IModulesManager)
        }
    }

    public void testDiscoverReturnFromDocstring2() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype testlib.unittest.GUITest'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverReturnFromDocstring3() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype GUITest'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverReturnFromDocstring4() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype :class:`GUITest`'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverReturnFromDocstring5() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype :class:`~GUITest`'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverReturnFromDocstring6() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype :class:`!GUITest`'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverReturnFromDocstring7() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':rtype :function:`IgnoreTitle GUITest`'\n" +
                "a = Foo()\n" +
                "a.rara().";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverParamFromDocstring() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, a):\n" +
                "        ':type a: GUITest'\n" +
                "        a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    /**
     * See: http://sphinx-doc.org/ext/autodoc.html#directive-autoattribute
     *
     * For module data members and class attributes, documentation can either be put:
     *
     * - into a comment with special formatting (using a #: to start the comment instead of just #),
     * - in a docstring after the definition i.e.: a = 10\n':type int'
     *
     * - Comments need to be either on a line of their own before the definition i.e.: #: :type int\na = 10
     * - or immediately after the assignment on the same line. -- i.e.: a = 10 #: :type int
     *
     * The latter form is restricted to one line only.
     * - Check LocalScope to fix this tests!
     */
    public void testDiscoverParamFromInline1() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, lst):\n" +
                "        for a in lst: #: :type a: GUITest\n" +
                "            a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverParamFromInline2() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, lst):\n" +
                "        #: :type a: GUITest\n" +
                "        for a in lst:\n" +
                "            a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverParamFromInline2a() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, lst):\n" +
                "        #@type a: GUITest\n" +
                "        for a in lst:\n" +
                "            a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

    public void testDiscoverParamFromInline3() throws Exception {
        this.useOriginalRequestCompl = true;
        String s;
        s = "" +
                "class Foo:\n" +
                "    def rara(self, lst):\n" +
                "        for a in lst:\n" +
                "            ': :type a: GUITest'\n" +
                "            a.";
        ICompletionProposal[] comps = requestCompl(s, s.length(), -1, new String[] { "SetWidget(widget, show, wait)" });
        assertTrue(comps.length > 30);
    }

}
