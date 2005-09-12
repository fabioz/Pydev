/*
 * License: Common Public License v1.0
 * Created on Sep 12, 2005
 * 
 * @author Fabio Zadrozny
 */
package com.python.pydev.codecompletion;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.revisited.CodeCompletionTestsBase;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;

public class CompletionParticipantTestsBase  extends CodeCompletionTestsBase {

    protected IPyDevCompletionParticipant participant;

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

}
