/*
 * Created on Jul 15, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.plugin.PydevPlugin;

public abstract class PyCompletionProposalExtension2 extends PyCompletionProposal implements ICompletionProposalExtension2{
    protected PyCompletionPresentationUpdater presentationUpdater;
    public int fLen;
    public boolean fLastIsPar;

    public PyCompletionProposalExtension2(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction, String args) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, onApplyAction, args);
        presentationUpdater = new PyCompletionPresentationUpdater();
    }


    /**
     * Called when Ctrl is selected during the completions
     * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer, boolean)
     */
    public void selected(ITextViewer viewer, boolean smartToggle) {
        if(smartToggle){
            StyledText text= viewer.getTextWidget();
            if (text == null || text.isDisposed())
                return;

            int widgetCaret= text.getCaretOffset();
            IDocument document = viewer.getDocument();
            int finalOffset = widgetCaret;
            
            try {
                if(finalOffset >= document.getLength()){
                    unselected(viewer);
                    return;
                }
                char c;
                do{
                    c = document.getChar(finalOffset);
                    finalOffset++;
                }while(isValidChar(c) && finalOffset < document.getLength());
                
                if(c == '('){
                    fLastIsPar = true;
                }else{
                    fLastIsPar = false;
                }
                
                if(!isValidChar(c)){
                    finalOffset--;
                }
                
                this.fLen = finalOffset-widgetCaret;
                this.presentationUpdater.updateStyle(viewer, widgetCaret, this.fLen);
            } catch (BadLocationException e) {
                PydevPlugin.log(e);
            }
            
        }else{
            unselected(viewer);
        }
    }

    /**
     * @param c
     * @return
     */
    private boolean isValidChar(char c) {
    	return Character.isJavaIdentifierPart(c);
    }

    public void unselected(ITextViewer viewer) {
        this.presentationUpdater.repairPresentation(viewer);
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false); 
        //System.out.println("validating:"+strs[0]+" - "+strs[1]);
        String qualifier = strs[1].toLowerCase();
        if(strs[0].length() == 0 && strs[1].length() == 0){
            return false;
        }
        String displayString = getDisplayString().toLowerCase();
        if(displayString.startsWith(qualifier)){
            return true;
        }
        
        return false;
    }

}
