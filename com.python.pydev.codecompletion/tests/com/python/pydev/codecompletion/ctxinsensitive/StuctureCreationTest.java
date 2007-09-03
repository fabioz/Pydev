/*
 * Created on 08/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalInfoTestsBase;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class StuctureCreationTest extends AdditionalInfoTestsBase {
    
    

    public static void main(String[] args) {

        try {
            // DEBUG_TESTS_BASE = true;
            StuctureCreationTest test = new StuctureCreationTest();
            test.setUp();
            test.testSetup2();
            test.tearDown();
            System.out.println("Finished");

//            junit.textui.TestRunner.run(StuctureCreationTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        } catch (Error e) {
            e.printStackTrace();
        }
    }

    
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        participant = new CtxParticipant();
        codeCompletion = new PyCodeCompletion();
        this.restorePythonPath(false);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    

    // ------------------------------------------------------------------------------------------------- tests
    
    public void testSetup() {
        AbstractAdditionalInterpreterInfo additionalSystemInfo = AdditionalSystemInterpreterInfo.getAdditionalSystemInfo(getInterpreterManager());
        assertTrue(additionalSystemInfo.getAllTokens().size() > 0);
        List<IInfo> tokensStartingWith = additionalSystemInfo.getTokensStartingWith("TestC", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertIsIn("TestCase", "unittest", tokensStartingWith);
    }

    
    public void testCompletion() throws CoreException, BadLocationException {
        requestCompl("Tes", -1, -1, new String[]{"TestCase - unittest"}); //at least 3 chars needed by default
    }
    
    public void testSetup2() throws CoreException, BadLocationException {
        AbstractAdditionalInterpreterInfo additionalInfo = AdditionalProjectInterpreterInfo.getAdditionalInfoForProject(nature.getProject());
        assertTrue(additionalInfo.getAllTokens().size() > 0);
        List<IInfo> tokensStartingWith = additionalInfo.getTokensStartingWith("MyInvalidClassInInvalidFil", AbstractAdditionalInterpreterInfo.TOP_LEVEL);
        assertEquals("Expecting no tokens. Found: "+tokensStartingWith, 0, tokensStartingWith.size());
    }

    // ----------------------------------------------------------------------------------------------- asserts
    
    
    private void assertIsIn(String tok, String mod, List<IInfo> tokensStartingWith) {
        for (IInfo info : tokensStartingWith) {
            if(info.getName().equals(tok)){
                if(info.getDeclaringModuleName().equals(mod)){
                    return;
                }
            }
        }
        fail("The tok "+tok+" was not found for the module "+mod);
    }

}
