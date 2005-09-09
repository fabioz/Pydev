/*
 * Created on 08/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;

public class StuctureCreationTest extends CodeCompletionTestsBase{
    
    private InterpreterObserver observer;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        observer = new InterpreterObserver();
        this.restorePythonPath(false);
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    @Override
    protected boolean restoreSystemPythonPath(boolean force, String path) {
        boolean restored = super.restoreSystemPythonPath(force, path);
        if(restored){
            IProgressMonitor monitor = new NullProgressMonitor();
            observer.notifyDefaultPythonpathRestored(getInterpreterManager(), monitor);
        }
        return restored;
    }
    
    @Override
    protected boolean restoreProjectPythonPath(boolean force, String path) {
        boolean ret = super.restoreProjectPythonPath(force, path);
        if(ret){
            observer.notifyProjectPythonpathRestored(nature);
        }
        return ret;
    }
    
    public void testSetup() {
        AdditionalInterpreterInfo additionalSystemInfo = AdditionalInterpreterInfo.getAdditionalSystemInfo();
        assertTrue(additionalSystemInfo.getAllTokens().size() > 0);
        List<IInfo> tokensStartingWith = additionalSystemInfo.getTokensStartingWith("TestC");
        assertIsIn("TestCase", "unittest", tokensStartingWith);
    }

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
