/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/09/2005
 */
package com.python.pydev.codecompletion.parameter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCodeCompletionPreferencesPage;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.shared_core.callbacks.ICallback;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;
import com.python.pydev.codecompletion.ctxinsensitive.CtxParticipant;

public class ParameterCompletionTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            ParameterCompletionTest test = new ParameterCompletionTest();
            test.setUp();
            test.testCompletion2();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(ParameterCompletionTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        CompiledModule.COMPILED_MODULES_ENABLED = false;

        useOriginalRequestCompl = true;
        participant = new CtxParticipant();

        ExtensionHelper.testingParticipants = new HashMap<String, List<Object>>();
        ArrayList<Object> participants = new ArrayList<Object>(); /*IPyDevCompletionParticipant*/
        participants.add(participant);
        ExtensionHelper.testingParticipants.put(ExtensionHelper.PYDEV_COMPLETION, participants);

        codeCompletion = new PyCodeCompletion();
        this.restorePythonPath(false);

        final IPreferenceStore prefs = new PreferenceStore();
        PyCodeCompletionPreferencesPage.getPreferencesForTests = new ICallback<IPreferenceStore, Object>() {

            @Override
            public IPreferenceStore call(Object arg) {
                return prefs;
            }
        };

        prefs.setValue(PyCodeCompletionPreferencesPage.MATCH_BY_SUBSTRING_IN_CODE_COMPLETION, false);

    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        useOriginalRequestCompl = false;
        ExtensionHelper.testingParticipants = null;
        PyCodeCompletionPreferencesPage.getPreferencesForTests = null;
    }

    // ------------------------------------------------------------------------------------------------- tests

    public void testSetup() throws MisconfigurationException {
        AbstractAdditionalTokensInfo additionalInfo = AdditionalProjectInterpreterInfo
                .getAdditionalInfoForProject(nature);
        assertTrue(additionalInfo.getAllTokens().size() > 0);
        Collection<IInfo> tokensStartingWith = additionalInfo.getTokensStartingWith("existingM",
                AbstractAdditionalTokensInfo.INNER);
        assertTrue(tokensStartingWith.size() == 1);
        assertIsIn("existingMethod", "testAssist.assist", tokensStartingWith);
    }

    public void testCompletion() throws Exception {
        String s = "" +
                "def m1(a):\n" +
                "    a.existingM";
        requestCompl(s, -1, -1, new String[] { "existingMethod()" }); //at least 3 chars needed by default
    }

    public void testCompletion2() throws Exception {
        String s = "" +
                "def m1(a):\n" +
                "    a.another()\n" +
                "    a.assertE";
        requestCompl(s, -1, -1, new String[] { "assertEquals" }); //at least 3 chars needed by default
    }

    // ----------------------------------------------------------------------------------------------- asserts

    private void assertIsIn(String tok, String mod, Collection<IInfo> tokensStartingWith) {
        for (IInfo info : tokensStartingWith) {
            if (info.getName().equals(tok)) {
                if (info.getDeclaringModuleName().equals(mod)) {
                    return;
                }
            }
        }
        fail("The tok " + tok + " was not found for the module " + mod);
    }

}
