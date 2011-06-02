/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jul 15, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;

public abstract class AbstractPyCompletionProposalExtension2 extends PyCompletionProposal implements ICompletionProposalExtension2, ICompletionProposalExtension {
    

    private PyCompletionPresentationUpdater presentationUpdater;
    
    /**
     * Only available when Ctrl is pressed when selecting the completion.
     */
    public int fLen;
    
    public boolean fLastIsPar;
    
    public AbstractPyCompletionProposalExtension2(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition, int priority) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null, null, priority);
    }

    public AbstractPyCompletionProposalExtension2(String replacementString, int replacementOffset, int replacementLength,
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, int onApplyAction, String args) {
        
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString,
                contextInformation, additionalProposalInfo, priority, onApplyAction, args);
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
                this.getPresentationUpdater().updateStyle(viewer, widgetCaret, this.fLen);
            } catch (BadLocationException e) {
                Log.log(e);
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
        this.getPresentationUpdater().repairPresentation(viewer);
    }

    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        String[] strs = PySelection.getActivationTokenAndQual(document, offset, false); 
        //System.out.println("validating:"+strs[0]+" - "+strs[1]);
        String qualifier = strs[1].toLowerCase();
        //when we end with a '.', we should start a new completion (and not stay in the old one).
        if(strs[1].length() == 0 && (strs[0].length() == 0 || strs[0].endsWith("."))){
            //System.out.println(false);
            return false;
        }
        String displayString = getDisplayString().toLowerCase();
        if(displayString.startsWith(qualifier)){
            //System.out.println(true);
            return true;
        }
        
        //System.out.println(false);
        return false;
    }

    
    
  //-------------------- ICompletionProposalExtension
    
    //Note that '.' is always there!!
    protected final static char[] VAR_TRIGGER= new char[] { '.' };

    /**
     * We want to apply it on \n or on '.'
     * 
     * When . is entered, the user will finish (and apply) the current completion
     * and request a new one with '.'
     * 
     * If not added, it won't request the new one (and will just stop the current)
     */
    public char[] getTriggerCharacters() {
        char[] chars = VAR_TRIGGER;
        if(PyCodeCompletionPreferencesPage.applyCompletionOnLParen()){
            chars = StringUtils.addChar(chars, '(');
        }
        if(PyCodeCompletionPreferencesPage.applyCompletionOnRParen()){
            chars = StringUtils.addChar(chars, ')');
        }
        return chars;
    }
    
    public void apply(IDocument document, char trigger, int offset) {
        throw new RuntimeException("Not implemented");
    }

    public int getContextInformationPosition() {
        return this.fCursorPosition;
    }

    public boolean isValidFor(IDocument document, int offset) {
        return validate(document, offset, null);
    }
    
    
    /**
     * Checks if the trigger character should actually a
     * @param trigger
     * @param doc
     * @param offset
     * @return
     */
    protected boolean triggerCharAppliesCurrentCompletion(char trigger, IDocument doc, int offset){
        if(trigger == '.' && !PyCodeCompletionPreferencesPage.applyCompletionOnDot()){
            //do not apply completion when it's triggered by '.', because that's usually not what's wanted
            //e.g.: if the user writes sys and the current completion is SystemError, pressing '.' will apply
            //the completion, but what the user usually wants is just having sys.xxx and not SystemError.xxx
            try {
                doc.replace(offset, 0, ".");
            } catch (BadLocationException e) {
                Log.log(e);
            }
            return false;
        }
        
        return true;
    }


    protected PyCompletionPresentationUpdater getPresentationUpdater() {
        if(presentationUpdater == null){
            presentationUpdater = new PyCompletionPresentationUpdater();
        }
        return presentationUpdater;
    }
}
