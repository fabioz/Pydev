/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 08/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.Collection;

import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.shared_core.SharedCorePlugin;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class StuctureCreationTest extends AdditionalInfoTestsBase {

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            StuctureCreationTest test2 = new StuctureCreationTest();
            test2.setUp();
            test2.testCompletion();
            test2.tearDown();

            System.out.println("Finished");

            junit.textui.TestRunner.run(StuctureCreationTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        participant = new CtxParticipant();
        codeCompletion = new PyCodeCompletion();
        this.restorePythonPath(false);
    }

    // ------------------------------------------------------------------------------------------------- tests

    public void testSetup() {
        // fails on Python >= 2.7 because unittest became a dir instead of one file.
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        AbstractAdditionalTokensInfo additionalSystemInfo;
        try {
            additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(getInterpreterManager(),
                    getInterpreterManager().getDefaultInterpreterInfo(false).getExecutableOrJar(), true);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
        assertTrue(additionalSystemInfo.getAllTokens().size() > 0);
        Collection<IInfo> tokensStartingWith = additionalSystemInfo.getTokensStartingWith("TestC",
                AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertIsIn("TestCase", "unittest", tokensStartingWith);
    }

    public void testCompletion() throws Exception {
        // fails on Python >= 2.7 because unittest became a dir instead of one file.
        if (SharedCorePlugin.skipKnownFailures()) {
            return;
        }

        requestCompl("Tes", -1, -1, new String[] { "TestCase - unittest" }); //at least 3 chars needed by default
    }

    public void testSetup2() throws Exception {
        AbstractAdditionalTokensInfo additionalInfo = AdditionalProjectInterpreterInfo
                .getAdditionalInfoForProject(nature);
        assertTrue(additionalInfo.getAllTokens().size() > 0);
        Collection<IInfo> tokensStartingWith = additionalInfo.getTokensStartingWith("MyInvalidClassInInvalidFil",
                AbstractAdditionalTokensInfo.TOP_LEVEL);
        assertEquals("Expecting no tokens. Found: " + tokensStartingWith, 0, tokensStartingWith.size());
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
