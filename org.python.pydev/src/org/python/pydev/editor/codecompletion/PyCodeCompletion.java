/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.ASTManager;
import org.python.pydev.editor.codecompletion.revisited.AbstractToken;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.modules.AbstractModule;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.editor.codecompletion.revisited.visitors.LocalScope;
import org.python.pydev.editor.codecompletion.shell.AbstractShell;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion extends AbstractPyCodeCompletion {

    
    /**
     * This constant is used to debug the code-completion process on a production environment,
     * so that we gather enough information about what's happening and the possible reasons
     * for some bug (at this moment this is being specifically added because of a halting bug
     * for pydev in linux: https://sourceforge.net/tracker/index.php?func=detail&aid=1509582&group_id=85796&atid=577329)
     */
    public static boolean DEBUG_CODE_COMPLETION = PyCodeCompletionPreferencesPage.isToDebugCodeCompletion();
    
    /* (non-Javadoc)
     * @see org.python.pydev.editor.codecompletion.IPyCodeCompletion#getCodeCompletionProposals(org.eclipse.jface.text.ITextViewer, org.python.pydev.editor.codecompletion.CompletionRequest)
     */
    @SuppressWarnings("unchecked")
    public List getCodeCompletionProposals(ITextViewer viewer, CompletionRequest request) throws CoreException, BadLocationException {
        if(DEBUG_CODE_COMPLETION){
            Log.toLogFile(this,"Starting getCodeCompletionProposals");
            Log.addLogLevel();
            Log.toLogFile(this,"Request:"+request);
        }
        ArrayList ret = new ArrayList();
        try {
        	IPythonNature pythonNature = request.nature;
            if (pythonNature == null) {
                throw new RuntimeException("Unable to get python nature.");
            }
            ICodeCompletionASTManager astManager = pythonNature.getAstManager();
            if (astManager == null) { //we're probably still loading it.
                return new ArrayList();
            }

            List theList = new ArrayList();
            try {
                if(DEBUG_CODE_COMPLETION){
                    Log.toLogFile(this,"AbstractShell.getServerShell");
                }
                if (CompiledModule.COMPILED_MODULES_ENABLED) {
                    AbstractShell.getServerShell(request.nature, AbstractShell.COMPLETION_SHELL); //just start it
                }
                if(DEBUG_CODE_COMPLETION){
                    Log.toLogFile(this,"END AbstractShell.getServerShell");
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String trimmed = request.activationToken.replace('.', ' ').trim();

            ImportInfo importsTipper = getImportsTipperStr(request);

            int line = request.doc.getLineOfOffset(request.documentOffset);
            IRegion region = request.doc.getLineInformation(line);

            CompletionState state = new CompletionState(line, request.documentOffset - region.getOffset(), null, request.nature, request.qualifier);
            state.isInCalltip = request.isInCalltip;

            boolean importsTip = false;
            //code completion in imports 
            if (importsTipper.importsTipperStr.length() != 0) {

                //get the project and make the code completion!!
                //so, we want to do a code completion for imports...
                //let's see what we have...

                importsTip = true;
                importsTipper.importsTipperStr = importsTipper.importsTipperStr.trim();
                IToken[] imports = astManager.getCompletionsForImport(importsTipper, request);
                theList.addAll(Arrays.asList(imports));

                //code completion for a token
            } else if (trimmed.equals("") == false && request.activationToken.indexOf('.') != -1) {

                if (request.activationToken.endsWith(".")) {
                    request.activationToken = request.activationToken.substring(0, request.activationToken.length() - 1);
                }
                
                List completions = new ArrayList();
                if (trimmed.equals("self") || FullRepIterable.getFirstPart(trimmed).equals("self")) {
                    state.setLookingFor(ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE);
                    getSelfOrClsCompletions(request, theList, state, false);
                    
                }else if (trimmed.equals("cls") || FullRepIterable.getFirstPart(trimmed).equals("cls")) { 
                    state.setLookingFor(ICompletionState.LOOKING_FOR_CLASSMETHOD_VARIABLE);
                    getSelfOrClsCompletions(request, theList, state, false);

                } else {

                    state.activationToken = request.activationToken;

                    //Ok, looking for a token in globals.
                    IToken[] comps = astManager.getCompletionsForToken(request.editorFile, request.doc, state);
                    theList.addAll(Arrays.asList(comps));
                }
                theList.addAll(completions);

            } else { //go to globals

                state.activationToken = request.activationToken;
                if(DEBUG_CODE_COMPLETION){
                    Log.toLogFile(this,"astManager.getCompletionsForToken");
                    Log.addLogLevel();
                }
                IToken[] comps = astManager.getCompletionsForToken(request.editorFile, request.doc, state);
                if(DEBUG_CODE_COMPLETION){
                    Log.remLogLevel();
                    Log.toLogFile(this,"END astManager.getCompletionsForToken");
                }

                theList.addAll(Arrays.asList(comps));
                
                theList.addAll(getGlobalsFromParticipants(request, state));
            }

            Set<String> alreadyChecked = new HashSet<String>();
            
            for(ListIterator it=theList.listIterator(); it.hasNext();){
                Object o = it.next();
                if(o instanceof IToken){
                    alreadyChecked.clear();
                    IToken initialToken = (IToken) o;
                    
                    IToken token = initialToken;
                    while(token.isImportFrom()){
                        String strRep = token.toString();
                        if(alreadyChecked.contains(strRep)){
                            break;
                        }
                        alreadyChecked.add(strRep);
                        
                        ICompletionState s = state.getCopyForResolveImportWithActTok(token.getRepresentation());
                        s.checkFindResolveImportMemory(token);
                        
                        IToken token2 = astManager.resolveImport(s, token);
                        if(token2 != null && initialToken != token2){
                            initialToken.setArgs(token2.getArgs());
                            initialToken.setDocStr(token2.getDocStr());
                            token = token2;
                        }
                    }
                }
            }
            changeItokenToCompletionPropostal(viewer, request, ret, theList, importsTip, state);
        } catch (CompletionRecursionException e) {
            ret.add(new CompletionProposal("",request.documentOffset,0,0,null,e.getMessage(), null,null));
        }
        
        if(DEBUG_CODE_COMPLETION){
            Log.remLogLevel();
            Log.toLogFile(this, "Finished completion. Returned:"+ret.size()+" completions.\r\n");
        }

        return ret;
    }

    @SuppressWarnings("unchecked")
    private Collection getGlobalsFromParticipants(CompletionRequest request, ICompletionState state) {
        ArrayList ret = new ArrayList();
        
        List participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_COMPLETION);
        for (Iterator iter = participants.iterator(); iter.hasNext();) {
            IPyDevCompletionParticipant participant = (IPyDevCompletionParticipant) iter.next();
            ret.addAll(participant.getGlobalCompletions(request, state));
        }
        return ret;
    }

    
    /**
     * @param request this is the request for the completion
     * @param theList OUT - returned completions are added here. (IToken instances)
     * @param getOnlySupers whether we should only get things from super classes (in this case, we won't get things from the current class)
     * @return the same tokens added in theList
     */
    public static IToken[] getSelfOrClsCompletions(CompletionRequest request, List theList, CompletionState state, boolean getOnlySupers) {
    	IToken[] comps = new IToken[0];
        SimpleNode s = PyParser.reparseDocument(new PyParser.ParserInfo(request.doc, true, request.nature, state.line)).o1;
        if(s != null){
            FindScopeVisitor visitor = new FindScopeVisitor(state.line, 0);
            try {
                s.accept(visitor);
                comps = getSelfOrClsCompletions(visitor.scope, request, theList, state, getOnlySupers);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return comps;
    }
    
    /**
     * Get self completions when you already have a scope
     */
    @SuppressWarnings("unchecked")
    public static IToken[] getSelfOrClsCompletions(LocalScope scope, CompletionRequest request, List theList, CompletionState state, boolean getOnlySupers) throws BadLocationException {
    	IToken[] comps = new IToken[0];
        while(scope.scope.size() > 0){
            SimpleNode node = (SimpleNode) scope.scope.pop();
            if(node instanceof ClassDef){
                ClassDef d = (ClassDef) node;
                
                if(getOnlySupers){
                    List gottenComps = new ArrayList();
                    for (int i = 0; i < d.bases.length; i++) {
                        if(d.bases[i] instanceof Name){
                            Name n = (Name) d.bases[i];
	                        state.activationToken = n.id;
	        	            IToken[] completions;
							try {
								completions = request.nature.getAstManager().getCompletionsForToken(request.editorFile, request.doc, state);
								gottenComps.addAll(Arrays.asList(completions));
							} catch (CompletionRecursionException e) {
								//ok...
							}
                        }
                    }
                    comps = (IToken[]) gottenComps.toArray(new IToken[0]);
                }else{
                    //ok, get the completions for the class, only thing we have to take care now is that we may 
                    //not have only 'self' for completion, but somthing lile self.foo.
                    //so, let's analyze our activation token to see what should we do.
                    
                    String trimmed = request.activationToken.replace('.', ' ').trim();
                    String[] actTokStrs = trimmed.split(" ");
                    if(actTokStrs.length == 0 || (!actTokStrs[0].equals("self")&& !actTokStrs[0].equals("cls")) ){
                        throw new AssertionError("We need to have at least one token (self or cls) for doing completions in the class.");
                    }
                    
                    if(actTokStrs.length == 1){
                        //ok, it's just really self, let's get on to get the completions
                        state.activationToken = NodeUtils.getNameFromNameTok((NameTok) d.name);
        	            try {
							comps = request.nature.getAstManager().getCompletionsForToken(request.editorFile, request.doc, state);
						} catch (CompletionRecursionException e) {
							//ok
						}
        	            
                    }else{
                        //it's not only self, so, first we have to get the definition of the token
                        //the first one is self, so, just discard it, and go on, token by token to know what is the last 
                        //one we are completing (e.g.: self.foo.bar)
                        int line = request.doc.getLineOfOffset(request.documentOffset);
                        IRegion region = request.doc.getLineInformationOfOffset(request.documentOffset);
                        int col =  request.documentOffset - region.getOffset();
                        IModule module = AbstractModule.createModuleFromDoc("", null, request.doc, request.nature, line);
                      
                        ASTManager astMan = ((ASTManager)request.nature.getAstManager());
                        comps = astMan.getAssignCompletions(module, new CompletionState(line, col, request.activationToken, request.nature, request.qualifier));

                    }
                }
	            theList.addAll(Arrays.asList(comps));
            }
        }
        return comps;

    }

    /**
     * This is the place where we change the tokens we've gathered so far with the 'inference' engine and transform those
     * tokens to actual completions as requested by the Eclipse infrastructure.
     * @param lookingForInstance if looking for instance, we should not add the 'self' as parameter.
     */
    private void changeItokenToCompletionPropostal(ITextViewer viewer, CompletionRequest request, List<ICompletionProposal> convertedProposals, List iTokenList, boolean importsTip, ICompletionState state) {
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
                
                String docStr = element.getDocStr();
                int type = element.getType();
                
                int priority = IPyCompletionProposal.PRIORITY_DEFAULT;
                if(type == IPyCodeCompletion.TYPE_PARAM){
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
                        request.documentOffset - request.qlen, request.qlen, l, PyCodeCompletionImages.getImageForType(type), null, 
                        pyContextInformation, docStr, priority, onApplyAction, args);
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
                if(type == IPyCodeCompletion.TYPE_PARAM){
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

    
    
    /**
     * @param element
     * @param lookingForInstance 
     * @param args
     * @return
     */
    private String getArgs(IToken element, ICompletionState state) {
        String args = "";
        int lookingFor = state.isLookingFor();
        boolean lookingForInstance = lookingFor==ICompletionState.LOOKING_FOR_INSTANCE_UNDEFINED || lookingFor==ICompletionState.LOOKING_FOR_INSTANCED_VARIABLE;
        if(element.getArgs().trim().length() > 0){
            StringBuffer buffer = new StringBuffer("(");
            StringTokenizer strTok = new StringTokenizer(element.getArgs(), "( ,)");

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
        } else if (element.getType() == IPyCodeCompletion.TYPE_FUNCTION){
            args = "()";
        }
        
        return args;
    }



}