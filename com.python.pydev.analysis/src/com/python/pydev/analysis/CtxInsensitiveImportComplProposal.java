/*
 * Created on 16/09/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.codecompletion.PyCompletionProposalExtension2;
import org.python.pydev.plugin.PydevPlugin;

public class CtxInsensitiveImportComplProposal extends PyCompletionProposalExtension2{

    public String realImportRep;

    public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, 
            String realImportRep) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, ON_APPLY_DEFAULT, "");
        this.realImportRep = realImportRep;
    }
    
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        try {
            IDocument document = viewer.getDocument();
            PySelection selection = new PySelection(document);
            int lineToAddImport = selection.getLineAvailableForImport();
            String delimiter = PyAction.getDelimiter(document);
            
            //first do the completion
            int dif = offset - fReplacementOffset;
            document.replace(offset-dif, dif+this.fLen, fReplacementString);
            
            //then do the import 
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
        String delimiter = PyAction.getDelimiter(document);
        return new Point(fReplacementOffset+fReplacementString.length()+realImportRep.length()+delimiter.length(), 0 );
    }
    
}