/*
 * Created on 21/08/2005
 */
package com.python.pydev.codecompletion.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.PyCompletionProposal;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;

public class CompletionParticipant implements IPyDevCompletionParticipant{

    public Collection getGlobalCompletions(CompletionRequest request, CompletionState state) {
        
        ArrayList list = new ArrayList();
        if(request.qualifier.length() >= 2){ //at least n characters required...
            
            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            AbstractModule module = ASTManager.createModule(request.editorFile, request.doc, state, astManager);
            
            //we don't want to add an import if it already exists
            IToken[] tokenImportedModules = module.getTokenImportedModules();
            Set toks = new HashSet();
            for (IToken token : tokenImportedModules) {
                toks.add(token.getRepresentation()); //representation for the modules that already exist
            }
            
            
            
            ProjectModulesManager projectModulesManager = astManager.getProjectModulesManager();
            Set allModuleNames = projectModulesManager.getAllModuleNames();
            for (Iterator iter = allModuleNames.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                
                FullRepIterable iterable = new FullRepIterable(name);
                for (String string : iterable) {
                    
                    String[] strings = FullRepIterable.headAndTail(string);
                    String packageName = strings[0];
                    String realImportRep = "import "+strings[1];
                    String importRep = strings[1];
                    
                    String displayString = importRep;
                    if(packageName.length() > 0){
                        realImportRep = "from "+packageName+" "+realImportRep;
                        displayString += " - "+ packageName;
                    }
                    
                    ImportComplProposal proposal = new ImportComplProposal(importRep,
                            request.documentOffset - request.qlen, 
                            request.qlen, 
                            realImportRep.length(), 
                            PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_PACKAGE), 
                            displayString, 
                            (IContextInformation)null, 
                            "", 
                            IPyCompletionProposal.PRIORITY_PACKAGES,
                            realImportRep);
                    
                    list.add(proposal);
                }
            }
        }
        return list;
    }
    
    private static class ImportComplProposal extends PyCompletionProposal{

        private String realImportRep;

        public ImportComplProposal(String replacementString, int replacementOffset, int replacementLength, int cursorPosition, Image image, String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority, String realImportRep) {
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
