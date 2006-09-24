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
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;

public class ImportsCompletionParticipant implements IPyDevCompletionParticipant{

    private static final Collection EMPTY_COLLECTION = new ArrayList();

    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state) {
        ArrayList<CtxInsensitiveImportComplProposal> list = new ArrayList<CtxInsensitiveImportComplProposal>();
        if(request.isInCalltip){
            return list;
        }
        
        if(request.qualifier.length() >= 2){ //at least n characters required...
            
            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            
            Image img = PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_PACKAGE);
            
            IModulesManager projectModulesManager = astManager.getModulesManager();
            Set allModuleNames = projectModulesManager.getAllModuleNames();
            
            String lowerQual = request.qualifier.toLowerCase();

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
                    
                    CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                            importRep,
                            request.documentOffset - request.qlen, 
                            request.qlen, 
                            realImportRep.length(), 
                            img, 
                            displayString, 
                            (IContextInformation)null, 
                            "", 
                            importRep.toLowerCase().equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_2 : IPyCompletionProposal.PRIORITY_PACKAGES,
                            realImportRep);

                    list.add(proposal);
                }
            }
        }
        return list;
    }

    public Collection getArgsCompletion(ICompletionState state, ILocalScope localScope, IToken[] interfaceForLocal) {
        return EMPTY_COLLECTION;
    }
}
