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

import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.builders.AdditionalInfoModulesObserver;
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

        ArrayList<Object> modulesObserver = new ArrayList<Object>(); /*IModulesObserver*/
        modulesObserver.add(new AdditionalInfoModulesObserver());
        ExtensionHelper.testingParticipants.put(ExtensionHelper.PYDEV_MODULES_OBSERVER, modulesObserver);

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
        if (TestDependent.PYTHON_WXPYTHON_PACKAGES != null) {
            CompiledModule module = new CompiledModule("wx", this.getManager().getModulesManager());

            participant = new CtxParticipant();
            ICompletionProposal[] proposals = requestCompl("Frame", -1, -1, new String[] {});
            assertContains("Frame - wx", proposals);
        }
    }

}
