/*
 * Created on 24/09/2005
 */
package com.python.pydev.codecompletion.simpleassist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.simpleassist.ISimpleAssistParticipant;

import com.python.pydev.codecompletion.ui.CodeCompletionPreferencesPage;

/**
 * Auto completion for keywords:
 * 
 * import keyword
 * >>> for k in keyword.kwlist: print k
and
assert
break
class
continue
def
del
elif
else
except
exec
finally
for
from
global
if
import
in
is
lambda
not
or
pass
print
raise
return
try
while
yield
 * @author Fabio
 */
public class KeywordsSimpleAssist implements ISimpleAssistParticipant{

    
    public static String defaultKeywordsAsString(){
        String[] KEYWORDS = new String[]{
                "and ",
                "assert ",
                "break",
                "class ",
                "continue",
                "def ",
                "del ",
                "elif ",
                "else:",
                "except:",
                "exec",
                "finally:",
                "for ",
                "from ",
                "global ",
                "if ",
                "import ",
                "in ",
                "is ",
                "lambda",
                "not ",
                "or ",
                "pass",
                "print ",
                "raise ",
                "return",
                "try:",
                "while ",
                "yield ",
                
                //the ones below were not in the initial list
                "self",
                "__init__",
                "as ",
                "False", 
                "None", 
                "object", 
                "True"
            };
        return wordsAsString(KEYWORDS);
    }
    
    //very simple cache (this might be requested a lot).
    private static String cache;
    private static String[] cacheRet;
    
    public static String[] stringAsWords(String keywords){
        if(cache != null && cache.equals(keywords)){
            return cacheRet;
        }
        StringTokenizer tokenizer = new StringTokenizer(keywords);
        ArrayList<String> strs = new ArrayList<String>();
        while(tokenizer.hasMoreTokens()){
            strs.add(tokenizer.nextToken());
        }
        cache = keywords;
        cacheRet = strs.toArray(new String[0]);
        return cacheRet;
    }
    
    public static String wordsAsString(String [] keywords){
        StringBuffer buf = new StringBuffer();
        for (String string : keywords) {
            buf.append(string);
            buf.append("\n");
        }
        return buf.toString();
    }
    
    public Collection<ICompletionProposal> computeCompletionProposals(String activationToken, String qualifier, PySelection ps, PyEdit edit, int offset) {
        List<ICompletionProposal> results = new ArrayList<ICompletionProposal>();
        //check if we have to use it
        if(!CodeCompletionPreferencesPage.useKeywordsCodeCompletion()){
            return results;
        }
        
        //get them
        if(activationToken.equals("") && qualifier.equals("") == false){
            for (String keyw : CodeCompletionPreferencesPage.getKeywords()) {
                if(keyw.startsWith(qualifier)){
                    results.add(new SimpleAssistProposal(keyw, offset - qualifier.length(), qualifier.length(), keyw.length(), PyCompletionProposal.PRIORITY_DEFAULT));
                }
            }
        }

        return results;
    }

    /**
     * by using this assist (with the extension), we are able to just validate it (without recomputing all completions each time).
     * 
     * They are only recomputed on backspace...
     * 
     * @author Fabio
     */
    public static class SimpleAssistProposal extends PyCompletionProposal implements ICompletionProposalExtension2{
        
        public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, int priority) {
            super(replacementString, replacementOffset, replacementLength, cursorPosition, priority);
        }

        public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority) {
            super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority);
            
        }

        public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
            try {
                int dif = offset - fReplacementOffset;
                viewer.getDocument().replace(offset, 0, fReplacementString.substring(dif));
            } catch (BadLocationException x) {
                // ignore
            }
        }

        public void selected(ITextViewer viewer, boolean smartToggle) {
        }

        public void unselected(ITextViewer viewer) {
        }

        public boolean validate(IDocument document, int offset, DocumentEvent event) {
            String[] strs = PyCodeCompletion.getActivationTokenAndQual(document, offset); 

            String activationToken = strs[0];
            String qualifier = strs[1];
            
            if(activationToken.equals("") && qualifier.equals("") == false){
                if(fReplacementString.startsWith(qualifier)){
                    return true;
                }
            }

            return false;
        }
        
        
    }
}
