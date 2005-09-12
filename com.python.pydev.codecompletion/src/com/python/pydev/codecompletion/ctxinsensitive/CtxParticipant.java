/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.ui.ImageCache;

import com.python.pydev.codecompletion.CodecompletionPlugin;

public class CtxParticipant implements IPyDevCompletionParticipant{

    public Collection getGlobalCompletions(CompletionRequest request, CompletionState state) {
        ImageCache imageCache = CodecompletionPlugin.getImageCache();
        Image classWithImport = imageCache.get(CodecompletionPlugin.CLASS_WITH_IMPORT_ICON);
        Image methodWithImport = imageCache.get(CodecompletionPlugin.METHOD_WITH_IMPORT_ICON);

        ArrayList<CtxInsensitiveImportComplProposal> completions = new ArrayList<CtxInsensitiveImportComplProposal>();
        if(request.qualifier.length() >= 3){ //at least n characters required...
            for (AdditionalInterpreterInfo additionalSystemInfo : AdditionalInterpreterInfo.getAdditionalInfo(request.nature)){
                List<IInfo> tokensStartingWith = additionalSystemInfo.getTokensStartingWith(request.qualifier);
                
                for (IInfo info : tokensStartingWith) {
                    String rep = info.getName();
                    String realImportRep = "";
                    String displayString = rep + " - "+ info.getDeclaringModuleName();
    
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
                            IPyCompletionProposal.PRIORITY_PACKAGES,
                            realImportRep);
                    
                    completions.add(proposal);
                }
    
            }
        }        
        return completions;
    }
    
    static class CtxInsensitiveImportComplProposal extends PyCompletionProposal{

        private String realImportRep;

        public CtxInsensitiveImportComplProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, String realImportRep) {
            super(replacementString, replacementOffset, replacementLength, cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority);
            this.realImportRep = realImportRep;
        }
        
        @Override
        public void apply(IDocument document) {
            try {
                document.replace(fReplacementOffset, fReplacementLength, realImportRep);
            } catch (BadLocationException x) {
                x.printStackTrace();
                // ignore
            }
        }
        
    }


}
