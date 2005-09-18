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
import org.python.pydev.editor.actions.PySelection;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.ICodeCompletionASTManager;
import org.python.pydev.editor.codecompletion.revisited.ProjectModulesManager;

import com.python.pydev.codecompletion.ctxinsensitive.CtxInsensitiveImportComplProposal;

public class CompletionParticipant implements IPyDevCompletionParticipant{

    public Collection getGlobalCompletions(CompletionRequest request, CompletionState state) {
        
        ArrayList list = new ArrayList();
        if(request.qualifier.length() >= 2){ //at least n characters required...
            
            ICodeCompletionASTManager astManager = request.nature.getAstManager();
            
            Image img = PyCodeCompletion.getImageForType(PyCodeCompletion.TYPE_PACKAGE);
            
            PySelection selection = new PySelection(request.doc);
            int lineAvailableForImport = selection.getLineAvailableForImport();

            ProjectModulesManager projectModulesManager = astManager.getProjectModulesManager();
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
                            realImportRep,
                            lineAvailableForImport);

                    list.add(proposal);
                }
            }
        }
        return list;
    }

}
