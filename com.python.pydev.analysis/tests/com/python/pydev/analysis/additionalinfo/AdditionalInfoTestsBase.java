/*
 * License: Common Public License v1.0
 * Created on Sep 12, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.analysis.additionalinfo;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;

import com.python.pydev.analysis.builder.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.MarkerStub;

public class AdditionalInfoTestsBase  extends CodeCompletionTestsBase {

    protected IPyDevCompletionParticipant participant;
    protected InterpreterObserver observer;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        observer = new InterpreterObserver();
    }

    @Override
    protected boolean restoreSystemPythonPath(boolean force, String path) {
        boolean restored = super.restoreSystemPythonPath(force, path);
        if(restored){
            IProgressMonitor monitor = new NullProgressMonitor();
            
            //try to load it from previous session
            if(!AdditionalSystemInterpreterInfo.loadAdditionalSystemInfo(getInterpreterManager())){
                observer.notifyDefaultPythonpathRestored(getInterpreterManager(), monitor);
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
                observer.notifyProjectPythonpathRestored(nature, new NullProgressMonitor());
            }
        }
        return ret;
    }

    public void requestCompl(File file, String strDoc, int documentOffset, int returned, String []retCompl) throws CoreException, BadLocationException{
        if(documentOffset == -1)
            documentOffset = strDoc.length();
        
        IDocument doc = new Document(strDoc);
        CompletionRequest request = new CompletionRequest(file, nature, doc, documentOffset, codeCompletion);

        CompletionState state = CompletionState.getEmptyCompletionState(nature);
        List props = new ArrayList(participant.getGlobalCompletions(request, state));
        ICompletionProposal[] codeCompletionProposals = codeCompletion.onlyValidSorted(props, request.qualifier);
        
        
        for (int i = 0; i < retCompl.length; i++) {
            assertContains(retCompl[i], codeCompletionProposals);
        }

        if(returned > -1){
            StringBuffer buffer = getAvailableAsStr(codeCompletionProposals);
            assertEquals("Expected "+returned+" received: "+codeCompletionProposals.length+"\n"+buffer, returned, codeCompletionProposals.length);
        }
    }

    /**
     * This method creates a marker stub
     * 
     * @param start start char
     * @param end end char
     * @param type the marker type
     * @return the created stub
     */
    protected MarkerStub createMarkerStub(int start, int end, int type) {
        HashMap attrs = new HashMap();

        attrs.put(AnalysisRunner.PYDEV_ANALYSIS_TYPE, type);
        attrs.put(IMarker.CHAR_START, start);
        attrs.put(IMarker.CHAR_END, end);
    
        MarkerStub marker = new MarkerStub(attrs);
        return marker;
    }
    
}
