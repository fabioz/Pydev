/*
 * Created on 16/09/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.PyCompletionProposalExtension2;
import org.python.pydev.plugin.PydevPlugin;

/**
 * This is the proposal that should be used to do a completion that can have a related import. 
 * 
 * @author Fabio
 */
public class CtxInsensitiveImportComplProposal extends PyCompletionProposalExtension2{
    
    /**
     * If empty, act as a regular completion
     */
    public String realImportRep;

    public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength, 
            int cursorPosition, Image image, String displayString, IContextInformation contextInformation, 
            String additionalProposalInfo, int priority, 
            String realImportRep) {
        
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, ON_APPLY_DEFAULT, "");
        this.realImportRep = realImportRep;
    }
    
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument document = viewer.getDocument();
        apply(document, trigger, stateMask, offset);
    }
    
    public void apply(IDocument document, char trigger, int stateMask, int offset) {
        try {
            PySelection selection = new PySelection(document);
            int lineToAddImport;
            if (realImportRep.length() > 0){
                lineToAddImport = selection.getLineAvailableForImport();
            }else{
                lineToAddImport = -1;
            }
            String delimiter = PyAction.getDelimiter(document);
            
            //first do the completion
            int dif = offset - fReplacementOffset;
            document.replace(offset-dif, dif+this.fLen, fReplacementString);
            
            //then do the import (if we have a valid location for it) 
            if(lineToAddImport >=0 && lineToAddImport <= document.getNumberOfLines()){
                IRegion lineInformation = document.getLineInformation(lineToAddImport);
                document.replace(lineInformation.getOffset(), 0, realImportRep+delimiter);
            }
            
        } catch (BadLocationException x) {
            PydevPlugin.log(x);
        }
    }    
    
    
    @Override
    public Point getSelection(IDocument document) {
        int importLen = 0;
        if(realImportRep.length() > 0){
            importLen = realImportRep.length()+PyAction.getDelimiter(document).length();
        }
        return new Point(fReplacementOffset+fReplacementString.length()+importLen, 0 );
    }
    
    public String getInternalDisplayStringRepresentation() {
        return fReplacementString;
    }


    @Override
    public int getOverrideBehavior(ICompletionProposal curr) {
        if(curr instanceof CtxInsensitiveImportComplProposal){
            if(curr.getDisplayString().equals(getDisplayString())){
                return BEHAVIOR_IS_OVERRIDEN;
            }else{
                return BEHAVIOR_COEXISTS;
            }
        }else{
            return BEHAVIOR_IS_OVERRIDEN;
        }
    }
}