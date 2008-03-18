package org.python.pydev.editor.codecompletion;

import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.docutils.ImportsSelection;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;

public abstract class AbstractPyCodeCompletion  implements IPyCodeCompletion  {

    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.IPyCodeCompletion#getImportsTipperStr(org.python.pydev.editor.codecompletion.CompletionRequest)
     */
    public ImportInfo getImportsTipperStr(CompletionRequest request) {
        
        IDocument doc = request.doc;
        int documentOffset = request.documentOffset;
        
        return ImportsSelection.getImportsTipperStr(doc, documentOffset);
    }

    
    

    /**
     * This is the place where we change the tokens we've gathered so far with the 'inference' engine and transform those
     * tokens to actual completions as requested by the Eclipse infrastructure.
     * @param lookingForInstance if looking for instance, we should not add the 'self' as parameter.
     */
    protected void changeItokenToCompletionPropostal(ITextViewer viewer, CompletionRequest request, List<ICompletionProposal> convertedProposals, List iTokenList, boolean importsTip, ICompletionState state) {
        for (Iterator iter = iTokenList.iterator(); iter.hasNext();) {
            
            Object obj = iter.next();
            
            if(obj instanceof IToken){
                IToken element =  (IToken) obj;
                
                String name = element.getRepresentation();
                
                //GET the ARGS
                int l = name.length();
                
                String args = "";
                if(!importsTip ){
                    boolean getIt = true;
                    if(AbstractToken.isClassDef(element)){
                        if(!request.isInCalltip){
                            getIt=false;
                        }
                    }
                    if(getIt){
                        args = getArgs(element, state);                
                        if(args.length()>0){
                            l++; //cursor position is name + '('
                        }
                    }
                }
                //END
                
                if(name.equals(request.fullQualifier) && args.trim().length() == 0){
                    //we don't want to get the tokens that are equal to the current 'full' qualifier
                    //...unless it adds the parameters to a call...
                    continue; 
                }
                
                int type = element.getType();
                
                int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                if(type == IToken.TYPE_PARAM || type == IToken.TYPE_LOCAL || type == IToken.TYPE_OBJECT_FOUND_INTERFACE){
                    priority = IPyCompletionProposal.PRIORITY_LOCALS;
                }
                
                int onApplyAction = PyCompletionProposal.ON_APPLY_DEFAULT;
                if(request.isInCalltip){
                    if(request.alreadyHasParams){
                        onApplyAction = PyCompletionProposal.ON_APPLY_JUST_SHOW_CTX_INFO;
                        
                    }else{
                        onApplyAction = PyCompletionProposal.ON_APPLY_SHOW_CTX_INFO_AND_ADD_PARAMETETRS;
                    }
                }
                PyCalltipsContextInformation pyContextInformation = null;
                if(args.length() > 2){
                    String contextArgs = args.substring(1, args.length()-1); //remove the parentesis
                    pyContextInformation = new PyCalltipsContextInformation(contextArgs, contextArgs, request);
                }
                PyCompletionProposal proposal = new PyLinkedModeCompletionProposal(name+args,
                        request.documentOffset - request.qlen, request.qlen, l, element, null, 
                        pyContextInformation, priority, onApplyAction, args);
                

                convertedProposals.add(proposal);
                    
            
            }else if(obj instanceof Object[]){
                Object element[] = (Object[]) obj;
                
                String name = (String) element[0];
                String docStr = (String) element [1];
                int type = -1;
                if(element.length > 2){
                    type = ((Integer) element [2]).intValue();
                }

                int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                if(type == IToken.TYPE_PARAM){
                    priority = IPyCompletionProposal.PRIORITY_LOCALS;
                }
                
                PyCompletionProposal proposal = new PyCompletionProposal(name,
                        request.documentOffset - request.qlen, request.qlen, name.length(), PyCodeCompletionImages.getImageForType(type), null, null, docStr, priority);
                
                convertedProposals.add(proposal);
                
            }else if(obj instanceof ICompletionProposal){
                //no need to convert
                convertedProposals.add((ICompletionProposal) obj);
            }
            
        }
    }

    
    protected String getArgs(IToken element, ICompletionState state) {
        int lookingFor = state.getLookingFor();
        return getArgs(element, lookingFor);
    }
    
    private String getArgs(IToken element, int lookingFor) {
        return getArgs(element.getArgs(), element.getType(), lookingFor);
    }
    
    /**
     * @return a string with the arguments to be shown for the given element.
     * 
     * E.g.: >>(self, a, b)<<
     */
    public static String getArgs(String argsReceived, int type, int lookingFor) {
        String args = "";
        boolean lookingForInstance = lookingFor==ICompletionState.LOOKING_FOR_INSTANCE_UNDEFINED || 
                                     lookingFor==ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE ||
                                     lookingFor==ICompletionState.LOOKING_FOR_ASSIGN;
        if(argsReceived.trim().length() > 0){
            StringBuffer buffer = new StringBuffer("(");
            StringTokenizer strTok = new StringTokenizer(argsReceived, "( ,)");

            while(strTok.hasMoreTokens()){
                String tok = strTok.nextToken();
                boolean addIt;
                if(lookingForInstance && tok.equals("self")){
                    addIt=false;
                }else if(!lookingForInstance && tok.equals("cls")){
                    addIt=false;
                }else{
                    addIt=true;
                }
                
                if(addIt){
                    if(buffer.length() > 1){
                        buffer.append(", ");
                    }
                    buffer.append(tok);
                }
            }
            buffer.append(")");
            args = buffer.toString();
        } else if (type == IToken.TYPE_FUNCTION){
            args = "()";
        }
        
        return args;
    }


}
