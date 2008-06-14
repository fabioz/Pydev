/*
 * Created on Jul 1, 2006
 * @author Fabio
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.FastStringBuffer;
import org.python.pydev.core.uiutils.RunInUiThread;
import org.python.pydev.plugin.PydevPlugin;

public class PyLinkedModeCompletionProposal extends PyCompletionProposalExtension2 implements ICompletionProposalExtension{
    

    private int firstParameterLen = 0;
    
    /**
     * The number of positions that we should add to the original position.
     * 
     * Used so that when we enter '.', we add an additional position (because '.' will be added when applying the completion) 
     * or so that we can go back one position when the toggle mode (ctrl) is on and a completion with parameters is applied (and they
     * are removed)
     */
    private int nPositionsAdded = 0;
    
    /**
     * If true, we'll go to the linked mode after applying a completion.
     */
    private boolean goToLinkedMode = true;

    /**
     * This is the token from where we should get the image and additional info.
     */
    private IToken element;
    
    /**
     * Constructor where the image and the docstring are lazily computed (initially added for the java integration).
     */
    public PyLinkedModeCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, 
            IToken element, String displayString, IContextInformation contextInformation,  
            int priority, int onApplyAction, String args) {
        
        super(replacementString, replacementOffset, replacementLength, cursorPosition, null, displayString, contextInformation, 
              "", priority, onApplyAction, args);
        
        this.element = element;
    }
    
    /**
     * Constructor where all the info is passed.
     */
    public PyLinkedModeCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction, String args) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, onApplyAction, args);
    }
    
    /**
     * Constructor where all the info is passed.
     */
    public PyLinkedModeCompletionProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction, String args, boolean goToLinkedMode) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, onApplyAction, args);
        this.goToLinkedMode = goToLinkedMode;
    }
    
    
    
    
    
    //methods overriden to be lazily gotten ----------------------------------------------------------------------------
    @Override
    public Image getImage() {
        if(element != null){
            return element.getImage();
        }else{
            return super.getImage();
        }
    }
    
    @Override
    public String getAdditionalProposalInfo() {
        if(element != null){
            return element.getDocStr();
        }else{
            return super.getAdditionalProposalInfo();
        }
    }
    //end methods overriden to be lazily gotten ------------------------------------------------------------------------
    
    
    
    /*
     * @see ICompletionProposal#getSelection(IDocument)
     */
    public Point getSelection(IDocument document) {
        if(onApplyAction == ON_APPLY_JUST_SHOW_CTX_INFO){
            return null;
        }
        if(onApplyAction == ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS ){
            if(fArgs.length() > 0){
                return new Point(fReplacementOffset + fCursorPosition-1, firstParameterLen); //the difference is the firstParameterLen here (instead of 0)
            }
            return null;
        }
        if(onApplyAction == ON_APPLY_DEFAULT ){
            return new Point(fReplacementOffset + fCursorPosition + nPositionsAdded, firstParameterLen); //the difference is the firstParameterLen here (instead of 0)
        }
        throw new RuntimeException("Unexpected apply mode:"+onApplyAction);
    }


    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        boolean eat = (stateMask & SWT.MOD1) != 0;
        IDocument doc = viewer.getDocument();
        
        if(onApplyAction == ON_APPLY_JUST_SHOW_CTX_INFO){
            return;
        }
        if(onApplyAction == ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS){
            try {
                String args;
                if(fArgs.length() > 0){
                    args = fArgs.substring(1, fArgs.length()-1); //remove the parentesis
                }else{
                    args = "";
                }
                super.apply(doc);
                
                if(!goToLinkedMode){
                    return;
                }
                
                int iPar = -1;
                int exitPos = offset + args.length()+1;
                goToLinkedModeFromArgs(viewer, offset, doc, exitPos, iPar, args);

            }catch(BadLocationException e){
                PydevPlugin.log(e);
            }
            return;
        }
        
        
        if(onApplyAction == ON_APPLY_DEFAULT){
            try {
                int dif = offset - fReplacementOffset;
                String strToAdd = fReplacementString.substring(dif);
                boolean doReturn = applyOnDoc(offset, eat, doc, dif, trigger);
                
                if(doReturn || !goToLinkedMode){
                    return;
                }
                
                //ok, now, on to the linking part
                int iPar = strToAdd.indexOf('(');
                if(iPar != -1 && strToAdd.charAt(strToAdd.length()-1) == ')'){
                    String newStr = strToAdd.substring(iPar+1, strToAdd.length()-1);
                    goToLinkedModeFromArgs(viewer, offset, doc, offset + strToAdd.length()+nPositionsAdded, iPar, newStr);
                }
            } catch (BadLocationException e) {
                PydevPlugin.log(e);
            }
            return;
        }
        
        throw new RuntimeException("Unexpected apply mode:"+onApplyAction);

    }

    /**
     * Applies the changes in the document (useful for testing)
     * 
     * @param offset the offset where the change should be applied
     * @param eat whether we should 'eat' the selection (on toggle)
     * @param doc the document where the changes should be applied 
     * @param dif the difference between the offset and and the replacement offset  
     * 
     * @return whether we should return (and not keep on with the linking mode)
     * @throws BadLocationException
     */
    public boolean applyOnDoc(int offset, boolean eat, IDocument doc, int dif, char trigger) throws BadLocationException {
        boolean doReturn = false;
        
        String rep = fReplacementString;
        int iPar = rep.indexOf('(');
        
        if(eat){
            
            //behavior change: when we have a parenthesis and we're in toggle (eat) mode, let's not add the 
            //parenthesis anymore.
            if(/*fLastIsPar &&*/ iPar != -1){
                rep = rep.substring(0, iPar);
                doc.replace(offset-dif, dif+this.fLen, rep);
                //if the last was a parenthesis, there's nothing to link, so, let's return
                if(!fLastIsPar){
                    nPositionsAdded = -1;
                }
                doReturn = true;
            }else{
                doc.replace(offset-dif, dif+this.fLen, rep);
            }
        }else{
            if(trigger == '.'){
                if(iPar != -1){
                    //if we had a completion with parameters, we should just remove everything that would appear after
                    //the parenthesis -- and we don't need to raise nTriggerCharsAdded because the cursor position would
                    //already be after the '(. -- which in this case we'll substitute for a '.'
                    rep = rep.substring(0, iPar);
                }else{
                    nPositionsAdded = 1;
                }
                rep = rep+trigger;
                
                //linking should not happen when applying '.'
                doReturn = true;
            }
            doc.replace(offset-dif, dif, rep);
        }
        return doReturn;
    }

    private void goToLinkedModeFromArgs(ITextViewer viewer, int offset, IDocument doc, int exitPos, int iPar, String newStr) throws BadLocationException {
        if(!goToLinkedMode){
            return;
        }
        List<Integer> offsetsAndLens = new ArrayList<Integer>();
        
        FastStringBuffer buffer = new FastStringBuffer();
        for (int i = 0; i < newStr.length(); i++) {
            char c = newStr.charAt(i);
            
            if(Character.isJavaIdentifierPart(c)){
                if(buffer.length() == 0){
                    offsetsAndLens.add(i);
                    buffer.append(c);
                }else{
                    buffer.append(c);
                }
            }else{
                if(buffer.length() > 0){
                    offsetsAndLens.add(buffer.length());
                    buffer.clear();
                }
            }
        }
        if(buffer.length() > 0){
            offsetsAndLens.add(buffer.length());
        }
        buffer = null;
        
        goToLinkedMode(viewer, offset, doc, exitPos, iPar, offsetsAndLens);
    }

    private void goToLinkedMode(ITextViewer viewer, int offset, IDocument doc, int exitPos, int iPar, List<Integer> offsetsAndLens) throws BadLocationException {
        if(!goToLinkedMode){
            return;
        }
        if(offsetsAndLens.size() > 0){
            LinkedModeModel model= new LinkedModeModel();
            
            for (int i = 0; i < offsetsAndLens.size(); i++) {
                Integer offs = offsetsAndLens.get(i);
                i++;
                Integer len = offsetsAndLens.get(i);
                if(i == 1){
                    firstParameterLen = len;
                }
                int location = offset+iPar+offs+1;
                LinkedPositionGroup group= new LinkedPositionGroup();
                ProposalPosition proposalPosition = new ProposalPosition(doc, location, len, 0 , new ICompletionProposal[0]);
                group.addPosition(proposalPosition);
                model.addGroup(group);
            }
            
            model.forceInstall();
            
            final LinkedModeUI ui= new EditorLinkedModeUI(model, viewer);
            ui.setDoContextInfo(true); //set it to request the ctx info from the completion processor
            ui.setExitPosition(viewer, exitPos, 0, Integer.MAX_VALUE);
            Runnable r = new Runnable(){
                public void run() {
                    ui.enter();
                }
            };
            RunInUiThread.async(r);

        }
    }
    
    
    
    //ICompletionProposalExtension

    protected final static char[] VAR_TRIGGER= new char[] { '.' };
    
    /**
     * We want to apply it on \n or on '.'
     * 
     * When . is entered, the user will finish (and apply) the current completion
     * and request a new one with '.'
     */
    public char[] getTriggerCharacters(){
        if(onApplyAction == ON_APPLY_DEFAULT){
            return VAR_TRIGGER;
        }
        return null;
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
    
    

}
