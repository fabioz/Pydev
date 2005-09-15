/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.ImageCache;

import com.python.pydev.codecompletion.CodecompletionPlugin;

public class CtxParticipant implements IPyDevCompletionParticipant{

    public Collection getGlobalCompletions(CompletionRequest request, CompletionState state) {
        ImageCache imageCache = CodecompletionPlugin.getImageCache();
        Image classWithImport = imageCache.get(CodecompletionPlugin.CLASS_WITH_IMPORT_ICON);
        Image methodWithImport = imageCache.get(CodecompletionPlugin.METHOD_WITH_IMPORT_ICON);

        PySelection selection = new PySelection(request.doc);
        int lineAvailableForImport = selection.getLineAvailableForImport();
        String delim = selection.getEndLineDelim();
        
        ArrayList<CtxInsensitiveImportComplProposal> completions = new ArrayList<CtxInsensitiveImportComplProposal>();
        if(request.qualifier.length() >= 3){ //at least n characters required...
            for (AbstractAdditionalInterpreterInfo additionalSystemInfo : AdditionalProjectInterpreterInfo.getAdditionalInfo(request.nature)){
                List<IInfo> tokensStartingWith = additionalSystemInfo.getTokensStartingWith(request.qualifier);
                
                for (IInfo info : tokensStartingWith) {
                    //there always must be a declaringModuleName
                    String declaringModuleName = info.getDeclaringModuleName();
                    
                    String rep = info.getName();
                    StringBuffer buffer = new StringBuffer();
                    buffer.append("from ");
                    buffer.append(declaringModuleName);
                    buffer.append(" import ");
                    buffer.append(rep);
                    buffer.append(delim);
                    String realImportRep = buffer.toString();
                    
                    buffer = new StringBuffer();
                    buffer.append(rep );
                    buffer.append(" - ");
                    buffer.append(declaringModuleName);
                    String displayString = buffer.toString();
    
                    //get the image
                    Image img;
                    if(info.getType() == IInfo.CLASS_WITH_IMPORT_TYPE){
                        img = classWithImport; 
                    }else if(info.getType() == IInfo.METHOD_WITH_IMPORT_TYPE){
                        img = methodWithImport; 
                    }else{
                        throw new RuntimeException("Undefined type.");
                    }
                    
                    
                    CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                            rep,
                            request.documentOffset - request.qlen, 
                            request.qlen, 
                            realImportRep.length(), 
                            img, 
                            displayString, 
                            (IContextInformation)null, 
                            "", 
                            IPyCompletionProposal.PRIORITY_GLOBALS,
                            realImportRep,
                            lineAvailableForImport);
                    
                    completions.add(proposal);
                }
    
            }
        }        
        return completions;
    }
    
    static class CtxInsensitiveImportComplProposal extends PyCompletionProposal{

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
                //first do the completion
                document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
                
                //then do the import 
                if(lineToAddImport >=0 && lineToAddImport <= document.getNumberOfLines()){
                    IRegion lineInformation = document.getLineInformation(lineToAddImport);
                    document.replace(lineInformation.getOffset(), 0, realImportRep);
                }

            } catch (BadLocationException x) {
                PydevPlugin.log(x);
            }
        }
        
        @Override
        public Point getSelection(IDocument document) {
            return new Point(fReplacementOffset+fReplacementString.length()+realImportRep.length(), 0 );
        }
        
    }


}
