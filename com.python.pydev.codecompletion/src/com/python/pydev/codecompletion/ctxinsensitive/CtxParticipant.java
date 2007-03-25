/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.text.contentassist.IContextInformation;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.ILocalScope;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.CompletionRequest;
import org.python.pydev.editor.codecompletion.IPyCompletionProposal;
import org.python.pydev.editor.codecompletion.IPyDevCompletionParticipant;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;
import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

public class CtxParticipant implements IPyDevCompletionParticipant{

    private Collection getThem(CompletionRequest request, boolean addAutoImport) {
        ArrayList<CtxInsensitiveImportComplProposal> completions = new ArrayList<CtxInsensitiveImportComplProposal>();
    	if(request.isInCalltip){
    	    return completions;
    	}
        
    	String qual = request.qualifier;
    	if(qual.length() >= 3){ //at least n characters required...
	        String lowerQual = qual.toLowerCase();
	        
	        String initialModule = request.resolveModule();
        
            List<IInfo> tokensStartingWith = AdditionalProjectInterpreterInfo.getTokensStartingWith(qual, request.nature, AbstractAdditionalInterpreterInfo.TOP_LEVEL);
            
            StringBuffer realImportRep = new StringBuffer();
            StringBuffer displayString = new StringBuffer();
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
                if(addAutoImport){
                    realImportRep.delete(0, realImportRep.length()); //clear the buffer
                    realImportRep.append("from ");
                    realImportRep.append(declaringModuleName);
                    realImportRep.append(" import ");
                    realImportRep.append(rep);
                }
                
                displayString.delete(0, displayString.length()); //clear the buffer
                displayString.append(rep );
                displayString.append(" - ");
                displayString.append(declaringModuleName);
                if(hasInit){
                	displayString.append(".__init__");
                }

                CtxInsensitiveImportComplProposal  proposal = new CtxInsensitiveImportComplProposal (
                        rep,
                        request.documentOffset - request.qlen, 
                        request.qlen, 
                        realImportRep.length(), 
                        AnalysisPlugin.getImageForAutoImportTypeInfo(info), 
                        displayString.toString(), 
                        (IContextInformation)null, 
                        "", 
                        rep.toLowerCase().equals(lowerQual)? IPyCompletionProposal.PRIORITY_LOCALS_1 : IPyCompletionProposal.PRIORITY_GLOBALS,
                        realImportRep.toString());
                
                completions.add(proposal);
            }
    
        }        
        return completions;
    }
    
    public Collection getGlobalCompletions(CompletionRequest request, ICompletionState state) {
        return getThem(request, true);
    }

    public Collection getArgsCompletion(ICompletionState state, ILocalScope localScope, Collection<IToken> interfaceForLocal) {
        ArrayList<IToken> ret = new ArrayList<IToken>();
        String qual = state.getQualifier();
        if(qual.length() >= 3){ //at least n characters or 3 interface tokens required
            
            List<IInfo> tokensStartingWith = AdditionalProjectInterpreterInfo.getTokensStartingWith(qual, state.getNature(), AbstractAdditionalInterpreterInfo.INNER);
            for (IInfo info : tokensStartingWith) {
                ret.add(new SourceToken(null, info.getName(), null, null, info.getDeclaringModuleName(), info.getType()));
            }
            
        }
        return ret;
    }

    public Collection getStringGlobalCompletions(CompletionRequest request, ICompletionState state) {
        return getThem(request, false);
    }


}
