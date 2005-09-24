/*
 * Created on 16/09/2005
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.plugin.PydevPlugin;

public class CtxInsensitiveImportComplProposal extends PyCompletionProposal{

    private String realImportRep;
    private int lineToAddImport;

    public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, 
            String realImportRep, int lineToAddImport) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority);
        this.realImportRep = realImportRep;
        this.lineToAddImport = lineToAddImport;
    }
    
    @Override
    public void apply(IDocument document) {
        try {
            String delimiter = PyAction.getDelimiter(document);
            
            //first do the completion
            document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
            
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