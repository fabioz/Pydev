package com.python.pydev.codecompletion.simpleassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.autoedit.DocCmd;
import org.python.pydev.editor.autoedit.PyAutoIndentStrategy;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;

/**
 * by using this assist (with the extension), we are able to just validate it (without recomputing all completions each time).
 * 
 * They are only recomputed on backspace...
 * 
 * @author Fabio
 */
public class SimpleAssistProposal extends PyCompletionProposal implements ICompletionProposalExtension2{
    
    public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, priority);
    }

    public SimpleAssistProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority);
    }
    
    private int changeInCursorPos = 0;
    
    public Point getSelection(IDocument document) {
        return new Point(fReplacementOffset + fCursorPosition + changeInCursorPos, 0);
    }


    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        try {
            IDocument doc = viewer.getDocument();
            if(fReplacementString.equals("else:") || fReplacementString.equals("except:") || fReplacementString.equals("finally:")){
                //make the replacement for the 'else'
                int dif = offset - fReplacementOffset;
                String replacementString = fReplacementString.substring(0, fReplacementString.length()-1);
                doc.replace(offset, 0, replacementString.substring(dif));
                
                //and now check the ':'
                PyAutoIndentStrategy strategy = new PyAutoIndentStrategy();
                DocCmd cmd = new DocCmd(offset+replacementString.length()-dif, 0, ":"); 
                Tuple<String, Integer> dedented = strategy.autoDedentAfterColon(doc, cmd);
                doc.replace(cmd.offset, 0, ":");
                if(dedented != null){
                    changeInCursorPos = -dedented.o2;
                }
            }else{
                int dif = offset - fReplacementOffset;
                doc.replace(offset, 0, fReplacementString.substring(dif));
            }
        } catch (BadLocationException x) {
            // ignore
        }
    }

    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    public void unselected(ITextViewer viewer) {
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false); 

        String activationToken = strs[0];
        String qualifier = strs[1];
        
        if(activationToken.equals("") && qualifier.equals("") == false){
            if(fReplacementString.startsWith(qualifier) && !fReplacementString.equals(qualifier)){
                return true;
            }
        }

        return false;
    }
    
    
}