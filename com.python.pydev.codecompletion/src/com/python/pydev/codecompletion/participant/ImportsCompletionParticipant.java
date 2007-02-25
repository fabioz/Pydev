/*
 * Created on 21/08/2005
 */
package com.python.pydev.codecompletion.participant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IModulesManager;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCodeCompletion;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCodeCompletionImages;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;

public class ImportsCompletionParticipant implements IPyDevCompletionParticipant{

    private static final Collection EMPTY_COLLECTION = new ArrayList();

    private Collection getThem(CompletionRequest request, boolean addAutoImport) {
        ArrayList<CtxInsensitiveImportComplProposal> list = new ArrayList<CtxInsensitiveImportComplProposal>();
        if(request.isInCalltip){
            return list;
        }
        
        if(request.qualifier.length() >= 2){ //at least n characters required...
            
            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            
            Image img = PyCodeCompletionImages.getImageForType(IPyCodeCompletion.TYPE_PACKAGE);
            
            IModulesManager projectModulesManager = astManager.getModulesManager();
            Set allModuleNames = projectModulesManager.getAllModuleNames();
            
            String lowerQual = request.qualifier.toLowerCase();

            StringBuffer realImportRep=new StringBuffer();
            
            for (Iterator iter = allModuleNames.iterator(); iter.hasNext();) {
                String name = (String) iter.next();
                
                FullRepIterable iterable = new FullRepIterable(name);
                for (String string : iterable) {
                    //clear the buffer...
                    realImportRep.delete(0, realImportRep.length());
                    
                    String[] strings = FullRepIterable.headAndTail(string);
                    String importRep = strings[1];
                    StringBuffer displayString = new StringBuffer(importRep);
                    
                    String packageName = strings[0];
                    if(addAutoImport){
                        realImportRep.append("import ");
                        realImportRep.append(strings[1]);
                    }
                    
                    if(packageName.length() > 0){
                        if(addAutoImport){
                            realImportRep.insert(0, " ");
                            realImportRep.insert(0, packageName);
                            realImportRep.insert(0, "from ");
                        }
                        displayString.append(" - ");
                        displayString.append(packageName);
                    }
                    
                    CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                            importRep,
                            request.documentOffset - request.qlen, 
                            request.qlen, 
                            realImportRep.length(), 
                            img, 
                            displayString.toString(), 
                            (IContextInformation)null, 
                            "", 
                            importRep.toLowerCase().equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_2 : IPyCompletionProposal.PRIORITY_PACKAGES,
                            realImportRep.toString());

                    list.add(proposal);
                }
            }
        }
        return list;
    }

    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state) {
        return getThem(request, true);
    }
    
    public Collection getArgsCompletion(ICompletionState state, ILocalScope localScope, Collection<IToken> interfaceForLocal) {
        return EMPTY_COLLECTION;
    }

    public Collection getStringGlobalCompletions(CompletionRequest request, ICompletionState state) {
        return getThem(request, false);
    }
}
