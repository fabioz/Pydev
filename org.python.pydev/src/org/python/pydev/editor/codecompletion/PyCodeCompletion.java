/*
 * Created on Aug 11, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.codecompletion;

import java.io.File;
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
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ICodeCompletionASTManager.ImportInfo;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.docutils.DocUtils;
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
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion {

    
    /**
     * This constant is used to debug the code-completion process on a production environment,
     * so that we gather enough information about what's happening and the possible reasons
     * for some bug (at this moment this is being specifically added because of a halting bug
     * for pydev in linux: https://sourceforge.net/tracker/index.php?func=detail&aid=1509582&group_id=85796&atid=577329)
     */
    public static boolean DEBUG_CODE_COMPLETION = PyCodeCompletionPreferencesPage.isToDebugCodeCompletion();
    
    /**
     * Type for unknown.
     */
    public static final int TYPE_UNKNOWN = -1;

    /**
     * Type for import (used to decide the icon)
     */
    public static final int TYPE_IMPORT = 0;
    
    /**
     * Type for class (used to decide the icon)
     */
    public static final int TYPE_CLASS = 1;
    
    /**
     * Type for function (used to decide the icon)
     */
    public static final int TYPE_FUNCTION = 2;
    
    /**
     * Type for attr (used to decide the icon)
     */
    public static final int TYPE_ATTR = 3;
    
    /**
     * Type for attr (used to decide the icon)
     */
    public static final int TYPE_BUILTIN = 4;
    
    /**
     * Type for parameter (used to decide the icon)
     */
    public static final int TYPE_PARAM = 5;
    
    /**
     * Type for package (used to decide the icon)
     */
    public static final int TYPE_PACKAGE = 6;

    /**
     * Type for relative import
     */
    public static final int TYPE_RELATIVE_IMPORT = 7;
    

    /**
     * Returns an image for the given type
     * @param type
     * @return
     */
    public static Image getImageForType(int type){
        try {
            ImageCache imageCache = PydevPlugin.getImageCache();
            if (imageCache == null)
                return null;

            switch (type) {
            case PyCodeCompletion.TYPE_IMPORT:
                return imageCache.get(UIConstants.COMPLETION_IMPORT_ICON);

            case PyCodeCompletion.TYPE_CLASS:
                return imageCache.get(UIConstants.COMPLETION_CLASS_ICON);

            case PyCodeCompletion.TYPE_FUNCTION:
                return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);

            case PyCodeCompletion.TYPE_ATTR:
                return imageCache.get(UIConstants.PUBLIC_ATTR_ICON);

            case PyCodeCompletion.TYPE_BUILTIN:
                return imageCache.get(UIConstants.BUILTINS_ICON);

            case PyCodeCompletion.TYPE_PARAM:
                return imageCache.get(UIConstants.COMPLETION_PARAMETERS_ICON);

            case PyCodeCompletion.TYPE_PACKAGE:
                return imageCache.get(UIConstants.COMPLETION_PACKAGE_ICON);
                
            case PyCodeCompletion.TYPE_RELATIVE_IMPORT:
                return imageCache.get(UIConstants.COMPLETION_RELATIVE_IMPORT_ICON);

            default:
                return null;
            }
            
        } catch (Exception e) {
            PydevPlugin.log(e, false);
            return null;
        }
    }

    /**
     * Returns a list with the tokens to use for autocompletion.
     * 
     * The list is composed from tuples containing the following:
     * 
     * 0 - String  - token name
     * 1 - String  - token description
     * 2 - Integer - token type (see constants)
     * @param viewer 
     * 
     * @return list of IToken.
     * 
     * (This is where we do the "REAL" work).
     * @throws BadLocationException
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
                if (trimmed.equals("self") || trimmed.startsWith("self")) {
                    state.setLookingForInstance(true);
                    getSelfCompletions(request, theList, state);

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
     * @param request
     * @param pythonNature
     * @param astManager
     * @param theList OUT - returned completions are added here. (IToken instances)
     * @param line 
     * @param state
     * @return the same tokens added in theList
     */
    public static IToken[] getSelfCompletions(CompletionRequest request, List theList, CompletionState state) {
        return getSelfCompletions(request, theList, state, false);
    }
    
    /**
     * @param request
     * @param pythonNature
     * @param astManager
     * @param theList OUT - returned completions are added here. (IToken instances)
     * @param line 
     * @param state
     * @return the same tokens added in theList
     */
    public static IToken[] getSelfCompletions(CompletionRequest request, List theList, CompletionState state, boolean getOnlySupers) {
    	IToken[] comps = new IToken[0];
        SimpleNode s = PyParser.reparseDocument(new PyParser.ParserInfo(request.doc, true, request.nature, state.line)).o1;
        if(s != null){
            FindScopeVisitor visitor = new FindScopeVisitor(state.line, 0);
            try {
                s.accept(visitor);
                comps = getSelfCompletions(visitor.scope, request, theList, state, getOnlySupers);
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
    public static IToken[] getSelfCompletions(LocalScope scope, CompletionRequest request, List theList, CompletionState state, boolean getOnlySupers) throws BadLocationException {
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
                    if(actTokStrs.length == 0 || actTokStrs[0].equals("self") == false){
                        throw new AssertionError("We need to have at least one token (self) for doing completions in the class.");
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
                if(type == PyCodeCompletion.TYPE_PARAM){
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
                        request.documentOffset - request.qlen, request.qlen, l, getImageForType(type), null, 
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
                if(type == PyCodeCompletion.TYPE_PARAM){
                    priority = IPyCompletionProposal.PRIORITY_LOCALS;
                }
                
                PyCompletionProposal proposal = new PyCompletionProposal(name,
                        request.documentOffset - request.qlen, request.qlen, name.length(), getImageForType(type), null, null, docStr, priority);
                
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
        boolean lookingForInstance = state.getIsLookingForInstance();
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
        } else if (element.getType() == PyCodeCompletion.TYPE_FUNCTION){
            args = "()";
        }
        
        return args;
    }



    /**
     * Returns non empty string if we are in imports section 
     * 
     * @param theActivationToken
     * @param edit
     * @param doc
     * @param documentOffset
     * @return single space string if we are in imports but without any module
     *         string with current module (e.g. foo.bar.
     */
    public ImportInfo getImportsTipperStr(CompletionRequest request) {
        
        IDocument doc = request.doc;
        int documentOffset = request.documentOffset;
        
        return getImportsTipperStr(doc, documentOffset);
    }
    

    public static ImportInfo getImportsTipperStr(IDocument doc, int documentOffset) {
        IRegion region;
        try {
            region = doc.getLineInformationOfOffset(documentOffset);
            String trimmedLine = doc.get(region.getOffset(), documentOffset-region.getOffset());
            trimmedLine = trimmedLine.trim();
            return getImportsTipperStr(trimmedLine, true);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * @param doc
     * @param documentOffset
     * @return the import info or null if none is available
     */
    public static ImportInfo getImportsTipperStr(String trimmedLine, boolean returnEvenEmpty) {
        String importMsg = "";
        
        if(!trimmedLine.startsWith("from") && !trimmedLine.startsWith("import")){
            return new ImportInfo("", false); //it is not an import
        }
        
        int fromIndex = trimmedLine.indexOf("from");
        int importIndex = trimmedLine.indexOf("import");
        boolean foundImportOnArray = false;

        //check if we have a from or an import.
        if(fromIndex  != -1 || importIndex != -1){
            trimmedLine = trimmedLine.replaceAll("#.*", ""); //remove comments 
            String[] strings = trimmedLine.split(" ");
            
            if(fromIndex != -1 && importIndex == -1){
                if(strings.length > 2){
                    //user has spaces as in  'from xxx uuu'
                    return new ImportInfo("", foundImportOnArray);
                }
            }
            
            for (int i = 0; i < strings.length; i++) {
                if(strings[i].equals("import")){
                    foundImportOnArray = true;
                }
                
                if(strings[i].equals("from")==false && strings[i].equals("import")==false){
                    if(importMsg.length() != 0){
                        importMsg += '.';
                    }
                    importMsg += strings[i];
                }
                //now, if we have a from xxx import something, we'll always want to return only the xxx
                if(fromIndex != -1 && importIndex == -1 && (foundImportOnArray || i == strings.length-1)){
                    if(importMsg.length() == 0){
                        return doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
                    }
                    if(importMsg.startsWith(".")){
                        return new ImportInfo(importMsg, foundImportOnArray);
                    }
                    if(importMsg.indexOf(".") == -1){
                        return doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
                    }
                    return new ImportInfo(importMsg.substring(0, importMsg.lastIndexOf(".")+1), foundImportOnArray);
                    
                }
            }
            
            if(fromIndex  != -1 && importIndex != -1){
                if(strings.length == 3){
                    importMsg += '.';
                }
            }
        }else{
            return new ImportInfo("", foundImportOnArray);
        }
        if (importMsg.indexOf(".") == -1){
            //we have only import fff or from iii (so, we're going for all imports).
            return doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
        }

        if(fromIndex == -1 && importMsg.indexOf(',') != -1){
            //we have something like import xxx, yyy, ...
            importMsg = importMsg.substring(importMsg.lastIndexOf(',')+1, importMsg.length());
            if(importMsg.startsWith(".")){
                importMsg = importMsg.substring(1);
            }
            
            int j = importMsg.lastIndexOf('.');
            if(j != -1){
                importMsg = importMsg.substring(0, j);
                return new ImportInfo(importMsg, foundImportOnArray);
            }else{
                return doExistingOrEmptyReturn(returnEvenEmpty, importMsg, foundImportOnArray);
            }
            
        }else{
            //now, we may still have something like 'unittest.test,' or 'unittest.test.,'
            //so, we have to remove this comma (s).
            int i;
            boolean removed = false;
            while ( ( i = importMsg.indexOf(',')) != -1){
                if(importMsg.charAt(i-1) == '.'){
                    int j = importMsg.lastIndexOf('.');
                    importMsg = importMsg.substring(0, j);
                }
                
                int j = importMsg.lastIndexOf('.');
                importMsg = importMsg.substring(0, j);
                removed = true;
            }
    
            //if it is something like aaa.sss.bb : removes the bb because it is the qualifier
            //if it is something like aaa.sss.   : removes only the last point
            if (!removed && importMsg.length() > 0 && importMsg.indexOf('.') != -1){
                importMsg = importMsg.substring(0, importMsg.lastIndexOf('.'));
            }
            
            
            return new ImportInfo(importMsg, foundImportOnArray);
        }
    }

    private static ImportInfo doExistingOrEmptyReturn(boolean returnEvenEmpty, String importMsg, boolean foundImportOnArray) {
        if(returnEvenEmpty || importMsg.trim().length() > 0){
            return new ImportInfo(" ", foundImportOnArray); 
        }else{
            return new ImportInfo("", foundImportOnArray); 
        }
    }

    /**
     * Return a document to parse, using some heuristics to make it parseable.
     * 
     * @param doc
     * @param documentOffset
     * @return
     */
    public static String getDocToParse(IDocument doc, int documentOffset) {
        int lineOfOffset = -1;
        try {
            lineOfOffset = doc.getLineOfOffset(documentOffset);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        
        if(lineOfOffset!=-1){
            String docToParseFromLine = getDocToParseFromLine(doc, lineOfOffset);
            if(docToParseFromLine != null)
                return docToParseFromLine;
//                return "\n"+docToParseFromLine;
            else
                return "";
        }else{
            return "";
        }
    }

    /**
     * Return a document to parse, using some heuristics to make it parseable.
     * (Changes the line specified by a pass)
     * 
     * @param doc
     * @param documentOffset
     * @param lineOfOffset
     * @return
     */
    public static String getDocToParseFromLine(IDocument doc, int lineOfOffset) {
        return DocUtils.getDocToParseFromLine(doc, lineOfOffset);
    }

    /**
     * 
     * @param useSimpleTipper
     * @return the script to get the variables.
     * 
     * @throws CoreException
     */
    public static File getAutoCompleteScript() throws CoreException {
        return PydevPlugin.getScriptWithinPySrc("simpleTipper.py");
    }

    
    /**
     * Filters the python completions so that only the completions we care about are shown (given the qualifier) 
     * @param pythonAndTemplateProposals the completions to sort / filter
     * @param qualifier the qualifier we care about
     * @param onlyForCalltips if we should filter having in mind that we're going to show it for a calltip
     * @return the completions to show to the user
     */
    @SuppressWarnings("unchecked")
    public ICompletionProposal[] onlyValidSorted(List pythonAndTemplateProposals, String qualifier, boolean onlyForCalltips) {
        //FOURTH: Now, we have all the proposals, only thing is deciding wich ones are valid (depending on
        //qualifier) and sorting them correctly.
        Collection returnProposals = new HashSet();
        String lowerCaseQualifier = qualifier.toLowerCase();
        
        for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof ICompletionProposal) {
                ICompletionProposal proposal = (ICompletionProposal) o;
            
                String displayString = proposal.getDisplayString();
                if(onlyForCalltips){
                    if (displayString.equals(qualifier)){
                        returnProposals.add(proposal);
                        
                    }else if (displayString.length() > qualifier.length() && displayString.startsWith(qualifier)){
                        if(displayString.charAt(qualifier.length()) == '('){
                            returnProposals.add(proposal);
                            
                        }
                    }
                }else if (displayString.toLowerCase().startsWith(lowerCaseQualifier)) {
                    returnProposals.add(proposal);
	            }
            }else{
                throw new RuntimeException("Error: expected instanceof ICompletionProposal and received: "+o.getClass().getName());
            }
        }

        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];

        // and fill with list elements
        returnProposals.toArray(proposals);

        Arrays.sort(proposals, PROPOSAL_COMPARATOR);
        return proposals;
    }

    /**
     * Compares proposals so that we can order them.
     */
    public static final ProposalsComparator PROPOSAL_COMPARATOR = new ProposalsComparator();

}