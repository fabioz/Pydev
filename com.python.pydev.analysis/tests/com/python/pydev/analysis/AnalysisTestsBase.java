/*
 * Created on 01/10/2005
 */
package com.python.pydev.analysis;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.TestDependent;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalDependencyInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.InterpreterObserver;
import com.python.pydev.analysis.messages.IMessage;

public class AnalysisTestsBase extends CodeCompletionTestsBase {

    protected String sDoc;
    protected Document doc;
    protected OccurrencesAnalyzer analyzer;
    protected IMessage[] msgs;
    protected AnalysisPreferencesStub prefs;

    //additional info
    protected InterpreterObserver observer;

    /**
     * @return Returns the manager.
     */
    protected ICodeCompletionASTManager getManager() {
        return (ICodeCompletionASTManager) nature.getAstManager();
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        AbstractAdditionalDependencyInfo.TESTING = true;
        //additional info
        observer = new InterpreterObserver();

        CompiledModule.COMPILED_MODULES_ENABLED = true;
        if(TestDependent.HAS_WXPYTHON_INSTALLED){
            restorePythonPath(TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES+"|"+TestDependent.PYTHON_WXPYTHON_PACKAGES, false);
        }else{
            restorePythonPath(TestDependent.PYTHON_LIB+"|"+TestDependent.PYTHON_SITE_PACKAGES, false);
        }
        prefs = new AnalysisPreferencesStub();
        analyzer = new OccurrencesAnalyzer();
        
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        AbstractAdditionalDependencyInfo.TESTING = false;
        CompiledModule.COMPILED_MODULES_ENABLED = false;
    }


    // ---------------------------------------------------------------------- additional info
    @Override
    protected boolean restoreSystemPythonPath(boolean force, String path) {
        boolean restored = super.restoreSystemPythonPath(force, path);
        if(restored){
            IProgressMonitor monitor = new NullProgressMonitor();
            
            //try to load it from previous session
            IInterpreterManager interpreterManager = getInterpreterManager();
            if(!AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(interpreterManager)){
                observer.notifyDefaultPythonpathRestored(interpreterManager, interpreterManager.getDefaultInterpreter(), monitor);
            }
        }
        return restored;
    }
    
    @Override
    protected boolean restoreProjectPythonPath(boolean force, String path) {
        boolean ret = super.restoreProjectPythonPath(force, path);
        if(ret){
            //try to load it from previous session
            if(!AdditionalProjectInterpreterInfo.loadAdditionalInfoForProject(nature.getProject())){
                observer.notifyProjectPythonpathRestored(nature, new NullProgressMonitor(), null);
            }
        }
        return ret;
    }

    //------------------------------------------------------------------------- analysis
    /**
     * @param msgs
     */
    protected void printMessages(IMessage ... msgs) {
        for (int i = 0; i < msgs.length; i++) {
            System.out.println(msgs[i]);
        }
    }
    protected void assertNotContainsMsg(String msg, IMessage[] msgs2) {
        if(containsMsg(msg, msgs2) != null){
            fail("The message "+msg+" was found within the messages (it should not have been found).");
        }
    }
    protected IMessage assertContainsMsg(String msg, IMessage[] msgs2) {
        return assertContainsMsg(msg, msgs2, -1);
    }

    protected IMessage assertContainsMsg(String msg, IMessage[] msgs2, int line) {
        IMessage found = containsMsg(msg, msgs2, line);
        
        if(found != null){
            return found;
        }
        
        StringBuffer msgsAvailable = new StringBuffer();
        for (IMessage message : msgs2) {
            msgsAvailable.append(message.getMessage());
            msgsAvailable.append("\n");
        }
        fail(StringUtils.format("No message named %s could be found. Available: %s", msg, msgsAvailable));
        return null;
    }

    /**
     * Checks if a specific message is contained within the messages passed
     */
    protected IMessage containsMsg(String msg, IMessage[] msgs2) {
        return containsMsg(msg, msgs2, -1);
    }
    
    /**
     * Checks if a specific message is contained within the messages passed
     */
    protected IMessage containsMsg(String msg, IMessage[] msgs2, int line) {
        IMessage ret = null;
        for (IMessage message : msgs2) {
            if(message.getMessage().equals(msg)){
                if(line != -1){
                    ret = message;
                    if(line == message.getStartLine(doc)){
                        return message;
                    }
                }else{
                    return message;
                }
            }
        }
        
        if(line != -1){
            fail("The message :"+msg+" was not found in the specified line ("+line+")");
        }
        return ret;
    }

    protected void printMessages(IMessage[] msgs, int i) {
        if(msgs.length != i){
            printMessages(msgs);
        }
        assertEquals(i, msgs.length);
    }

}
