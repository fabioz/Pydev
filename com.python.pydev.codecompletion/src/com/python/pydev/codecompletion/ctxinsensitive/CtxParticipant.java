/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class CtxParticipant implements IPyDevCompletionParticipant{

    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state) {
    	ArrayList<CtxInsensitiveImportComplProposal> completions = new ArrayList<CtxInsensitiveImportComplProposal>();
    	String qual = request.qualifier;
    	if(qual.length() >= 3){ //at least n characters required...
	        
	        String lowerQual = qual.toLowerCase();
	        
	        String initialModule = null;
	        if (request.editorFile != null){
	        	request.nature.resolveModule(request.editorFile);
	        }
        
            List<IInfo> tokensStartingWith = AdditionalProjectInterpreterInfo.getTokensStartingWith(qual, request.nature, AbstractAdditionalInterpreterInfo.TOP_LEVEL);
            
            for (IInfo info : tokensStartingWith) {
                //there always must be a declaringModuleName
                String declaringModuleName = info.getDeclaringModuleName();
                if(initialModule != null && declaringModuleName != null){
                    if(initialModule.equals(declaringModuleName)){
                    	continue;
                    }
                }
                boolean hasInit = false;
                if(declaringModuleName.endsWith(".__init__")){
                	declaringModuleName = declaringModuleName.substring(0, declaringModuleName.length()-9);//remove the .__init__
                	hasInit = true;
                }
                
                String rep = info.getName();
                StringBuffer buffer = new StringBuffer();
                buffer.append("from ");
                buffer.append(declaringModuleName);
                buffer.append(" import ");
                buffer.append(rep);
                String realImportRep = buffer.toString();
                
                buffer = new StringBuffer();
                buffer.append(rep );
                buffer.append(" - ");
                buffer.append(declaringModuleName);
                if(hasInit){
                	buffer.append(".__init__");
                }
                String displayString = buffer.toString();

                CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                        rep,
                        request.documentOffset - request.qlen, 
                        request.qlen, 
                        realImportRep.length(), 
                        AnalysisPlugin.getImageForAutoImportTypeInfo(info), 
                        displayString, 
                        (IContextInformation)null, 
                        "", 
                        rep.toLowerCase().equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_1 : IPyCompletionProposal.PRIORITY_GLOBALS,
                        realImportRep);
                
                completions.add(proposal);
            }
    
        }        
        return completions;
    }


}
