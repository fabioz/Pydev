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
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.swt.graphics.Image;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Name;
import org.python.pydev.editor.codecompletion.revisited.CompletionRecustionException;
import org.python.pydev.editor.codecompletion.revisited.CompletionState;
import org.python.pydev.editor.codecompletion.revisited.IASTManager;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.CompiledModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.FindScopeVisitor;
import org.python.pydev.parser.PyParser;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * @author Dmoore
 * @author Fabio Zadrozny
 */
public class PyCodeCompletion {

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
     * Position in document prior to the activation token
     */
    private int docBoundary = -1; 
    
    /**
     * 
     */
    public PyCodeCompletion() {
        imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
    }

    /**
     * 
     */
    public PyCodeCompletion(boolean useImageCache) {
        if(useImageCache)
            imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
    }

    /**
     * Returns an image for the given type
     * @param type
     * @return
     */
    public Image getImageForType(int type){
        if(imageCache == null)
            return null;
        
        switch(type){
        	case PyCodeCompletion.TYPE_IMPORT: 
        	    return imageCache.get(UIConstants.COMPLETION_IMPORT_ICON);
        	
        	case PyCodeCompletion.TYPE_CLASS: 
        	    return imageCache.get(UIConstants.COMPLETION_CLASS_ICON);
        	
        	case PyCodeCompletion.TYPE_FUNCTION: 
        	    return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);

        	case PyCodeCompletion.TYPE_ATTR: 
        	    return imageCache.get(UIConstants.PUBLIC_METHOD_ICON);

        	case PyCodeCompletion.TYPE_BUILTIN: 
        	    return imageCache.get(UIConstants.BUILTINS_ICON);
        	
        	case PyCodeCompletion.TYPE_PARAM: 
        	    return imageCache.get(UIConstants.COMPLETION_PARAMETERS_ICON);
        	
        	default:
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
     * 
     * @return list of IToken.
     * 
     * (This is where we do the "REAL" work).
     * @throws BadLocationException
     */
    public List getCodeCompletionProposals(CompletionRequest request) throws CoreException, BadLocationException {
        
        ArrayList ret = new ArrayList();
        try {
            PythonNature pythonNature = request.nature;
            if (pythonNature == null) {
                throw new RuntimeException("Unable to get python nature.");
            }
            IASTManager astManager = pythonNature.getAstManager();
            if (astManager == null) { //we're probably still loading it.
                return new ArrayList();
            }

            List theList = new ArrayList();
            PythonShell serverShell = null;
            try {
                if (CompiledModule.COMPILED_MODULES_ENABLED) {
                    serverShell = PythonShell.getServerShell(PythonShell.COMPLETION_SHELL);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            String trimmed = request.activationToken.replace('.', ' ').trim();

            String importsTipper = getImportsTipperStr(request);

            int line = request.doc.getLineOfOffset(request.documentOffset);
            IRegion region = request.doc.getLineInformation(line);

            CompletionState state = new CompletionState(line, request.documentOffset - region.getOffset(), null, request.nature);

            boolean importsTip = false;
            //code completion in imports 
            if (importsTipper.length() != 0) {

                //get the project and make the code completion!!
                //so, we want to do a code completion for imports...
                //let's see what we have...

                importsTip = true;
                importsTipper = importsTipper.trim();
                IToken[] imports = astManager.getCompletionsForImport(importsTipper, request.nature);
                theList.addAll(Arrays.asList(imports));

                //code completion for a token
            } else if (trimmed.equals("") == false && request.activationToken.indexOf('.') != -1) {

                List completions = new ArrayList();
                if (trimmed.equals("self")) {
                    getSelfCompletions(request, theList, state);

                } else {
                    if (request.activationToken.endsWith(".")) {
                        request.activationToken = request.activationToken.substring(0, request.activationToken.length() - 1);
                    }

                    state.activationToken = request.activationToken;

                    //Ok, looking for a token in globals.
                    IToken[] comps = astManager.getCompletionsForToken(request.editorFile, request.doc, state);
                    theList.addAll(Arrays.asList(comps));
                }
                theList.addAll(completions);

            } else { //go to globals
                List completions = new ArrayList();

                state.activationToken = request.activationToken;
                IToken[] comps = astManager.getCompletionsForToken(request.editorFile, request.doc, state);

                theList.addAll(Arrays.asList(comps));
            }

            changeItokenToCompletionPropostal(request, ret, theList, importsTip);
        } catch (CompletionRecustionException e) {
            ret.add(new CompletionProposal("",request.documentOffset,0,0,null,e.getMessage(), null,null));
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
        SimpleNode s = (SimpleNode) PyParser.reparseDocument(new PyParser.ParserInfo(request.doc, true, request.nature, state.line))[0];
        if(s != null){
            FindScopeVisitor visitor = new FindScopeVisitor(state.line, 0);
            try {
                s.accept(visitor);
                
                while(visitor.scope.scope.size() > 0){
                    SimpleNode node = (SimpleNode) visitor.scope.scope.pop();
                    if(node instanceof ClassDef){
                        ClassDef d = (ClassDef) node;
                        IToken[] comps = new IToken[0];
                        
                        if(getOnlySupers){
                            List gottenComps = new ArrayList();
                            for (int i = 0; i < d.bases.length; i++) {
	                            if(d.bases[i] instanceof Name){
	                                Name n = (Name) d.bases[i];
			                        state.activationToken = n.id;
			        	            IToken[] completions = request.nature.getAstManager().getCompletionsForToken(request.editorFile, request.doc, state);
			        	            theList.addAll(Arrays.asList(completions));
			        	            gottenComps.addAll(Arrays.asList(completions));
	                            }
                            }
                            comps = (IToken[]) gottenComps.toArray(new IToken[0]);
                        }else{
	                        state.activationToken = d.name;
	        	            comps = request.nature.getAstManager().getCompletionsForToken(request.editorFile, request.doc, state);
	        	            theList.addAll(Arrays.asList(comps));
                        }
                        return comps;
                    }
                }
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return new IToken[0];
    }

    /**
     * @param request
     * @param convertedProposals
     * @param iTokenList
     * @param importsTip
     */
    private void changeItokenToCompletionPropostal(CompletionRequest request, List convertedProposals, List iTokenList, boolean importsTip) {
        for (Iterator iter = iTokenList.iterator(); iter.hasNext();) {
            
            Object obj = iter.next();
            
            if(obj instanceof IToken){
                IToken element =  (IToken) obj;
                
                String name = element.getRepresentation();
                
                //GET the ARGS
                int l = name.length();
                
                String args = "";
                if(! importsTip){
	                args = getArgs(element);                
	                if(args.length()>0){
	                    l++;
	                }
                }
                //END
                
                String docStr = element.getDocStr();
                int type = element.getType();
                
                    
                CompletionProposal proposal = new CompletionProposal(name+args,
                        request.documentOffset - request.qlen, request.qlen, l, getImageForType(type), null, null, docStr);
                
                convertedProposals.add(proposal);
            
            }else if(obj instanceof Object[]){

                Object element[] = (Object[]) obj;
                
                String name = (String) element[0];
                String docStr = (String) element [1];
                int type = -1;
                if(element.length > 2){
                    type = ((Integer) element [2]).intValue();
                }
                CompletionProposal proposal = new CompletionProposal(name,
                        request.documentOffset - request.qlen, request.qlen, name.length(), getImageForType(type), null, null, docStr);
                
                convertedProposals.add(proposal);
            }
            
        }
    }

    
    
    /**
     * @param element
     * @param args
     * @return
     */
    private String getArgs(IToken element) {
        String args = "";
        if(element.getArgs().trim().length() > 0){
            StringBuffer buffer = new StringBuffer("(");
            StringTokenizer strTok = new StringTokenizer(element.getArgs(), "( ,)");

            while(strTok.hasMoreTokens()){
                String tok = strTok.nextToken();
                if(tok.equals("self") == false){
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
     * This is an image cache (probably this should be initialized elsewhere).
     */
    private ImageCache imageCache;

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
    public String getImportsTipperStr(CompletionRequest request) {
        
        IDocument doc = request.doc;
        int documentOffset = request.documentOffset;
        
        return getImportsTipperStr(doc, documentOffset);
    }

    /**
     * @param doc
     * @param documentOffset
     * @return
     */
    public static String getImportsTipperStr(IDocument doc, int documentOffset) {
        String importMsg = "";
        try {
            
            IRegion region = doc.getLineInformationOfOffset(documentOffset);
            String string = doc.get(region.getOffset(), documentOffset-region.getOffset());
            int fromIndex = string.indexOf("from");
            int importIndex = string.indexOf("import");

            //check if we have a from or an import.
            if(fromIndex  != -1 || importIndex != -1){
                string = string.replaceAll("#.*", ""); //remove comments 
                String[] strings = string.split(" ");
                
                for (int i = 0; i < strings.length; i++) {
                    if(strings[i].equals("from")==false && strings[i].equals("import")==false){
                        if(importMsg.length() != 0){
                            importMsg += '.';
                        }
                        importMsg += strings[i];
                    }
                }
                
                if(fromIndex  != -1 && importIndex != -1){
                    if(strings.length == 3){
                        importMsg += '.';
                    }
                }
            }else{
                return "";
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
        if (importMsg.indexOf(".") == -1){
            return " "; //we have only import fff (so, we're going for all imports).
        }

        //now, we may still have something like 'unittest.test,' or 'unittest.test.,'
        //so, we have to remove this comma (s).
        int i;
        while ( ( i = importMsg.indexOf(',')) != -1){
            if(importMsg.charAt(i-1) == '.'){
                int j = importMsg.lastIndexOf('.');
                importMsg = importMsg.substring(0, j);
            }
            
            int j = importMsg.lastIndexOf('.');
            importMsg = importMsg.substring(0, j);
        }

        //if it is something like aaa.sss.bb : removes the bb because it is the qualifier
        //if it is something like aaa.sss.   : removes only the last point
        if (importMsg.length() > 0 && importMsg.indexOf('.') != -1){
            importMsg = importMsg.substring(0, importMsg.lastIndexOf('.'));
        }
        
        
        return importMsg;
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
        String wholeDoc = doc.get();
        String newDoc = "";
        try {
            IRegion lineInformation = doc.getLineInformation(lineOfOffset);

            int docLength = doc.getLength();

            String before = wholeDoc.substring(0, lineInformation.getOffset());
            String after = wholeDoc.substring(lineInformation.getOffset()
                    + lineInformation.getLength(), docLength);
            
            String src = doc.get(lineInformation.getOffset(), lineInformation.getLength());

            String spaces = "";
            for (int i = 0; i < src.length(); i++) {
                if (src.charAt(i) != ' ') {
                    break;
                }
                spaces += ' ';
            }


            src = src.trim();
            if (src.startsWith("class")){
                //let's discover if we should put a pass or not...
                //e.g if we are declaring the class and no methods are put, we have
                //to put a pass, otherwise, the pass would ruin the indentation, therefore,
                //we cannot put it.
                //
                //so, search for another class or def after this line and discover if it has another indentation 
                //or not.
                
                StringTokenizer tokenizer = new StringTokenizer(after, "\r\n");
                String tokSpaces = null;
                
                while(tokenizer.hasMoreTokens()){
                    String tok = tokenizer.nextToken();
                    String t = tok.trim();
                    if(t.startsWith("class") || t.startsWith("def") ){
                        tokSpaces = "";
                        for (int i = 0; i < tok.length(); i++) {
                            if (tok.charAt(i) != ' ') {
                                break;
                            }
                            tokSpaces += ' ';
                        }
                        break;
                    }
                }
                
                if(tokSpaces != null && tokSpaces.length() > spaces.length()){
	                if(src.indexOf('(') != -1){
	                    src = src.substring(0, src.indexOf('('))+":";
	                }else{
	                    src = "class COMPLETION_HELPER_CLASS:";
	                }
                }else{
	                if(src.indexOf('(') != -1){
	                    src = src.substring(0, src.indexOf('('))+":pass";
	                }else{
	                    src = "class COMPLETION_HELPER_CLASS:pass";
	                }
                }
                
                
            }else{
                src = "pass";
            }
            
            newDoc = before;
            newDoc += spaces + src;
            newDoc += after;

        } catch (BadLocationException e1) {
            //that's ok...
            //e1.printStackTrace();
            return null;
        }
        return newDoc;
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
     * The docBoundary should get until the last line before the one we are
     * editing.
     */
    public void calcDocBoundary(String theDoc, int documentOffset) {
        this.docBoundary = theDoc.substring(0, documentOffset)
                .lastIndexOf('\n');
    }
    
	private static String extractPrefix(IDocument document, int offset) {
		int i= offset;
		if (i > document.getLength())
			return ""; //$NON-NLS-1$
		
		try {
			while (i > 0) {
				char ch= document.getChar(i - 1);
				if (!Character.isJavaIdentifierPart(ch))
					break;
				i--;
			}

			return document.get(i, offset - i);
		} catch (BadLocationException e) {
			return ""; //$NON-NLS-1$
		}
	}

    public static String [] getActivationTokenAndQual(String theDoc, int documentOffset) {
        return getActivationTokenAndQual(new Document(theDoc), documentOffset);
        
    }
	
    /**
     * Returns the activation token.
     * 
     * @param theDoc
     * @param documentOffset
     * @return the activation token and the qualifier.
     */
    public static String [] getActivationTokenAndQual(IDocument theDoc, int documentOffset) {
        String activationToken = extractPrefix(theDoc, documentOffset);
        documentOffset = documentOffset-activationToken.length()-1;

        try {
            while(documentOffset >= 0 && documentOffset < theDoc.getLength() && theDoc.get(documentOffset, 1).equals(".")){
                String tok = extractPrefix(theDoc, documentOffset);

                    
                String c = theDoc.get(documentOffset-1, 1);
                
                if(c.equals("]")){
	                activationToken = "list."+activationToken;  
                    break;
                    
                }else if(c.equals("}")){
	                activationToken = "dict."+activationToken;  
                    break;
                    
                }else if(c.equals("'") || c.equals("\"")){
	                activationToken = "str."+activationToken;  
                    break;
                
                }else if(c.equals(")")){
                    documentOffset = eatFuncCall(theDoc, documentOffset-1);
                    tok = extractPrefix(theDoc, documentOffset);
	                activationToken = tok+"()."+activationToken;  
                    documentOffset = documentOffset-tok.length()-1;
                
                }else if(tok.length() > 0){
	                activationToken = tok+"."+activationToken;  
                    documentOffset = documentOffset-tok.length()-1;
                    
                }else{
                    break;
                }

            }
        } catch (BadLocationException e) {
            System.out.println("documentOffset "+documentOffset);
            System.out.println("theDoc.getLength() "+theDoc.getLength());
            e.printStackTrace();
        }
        
        String qualifier = "";
        //we complete on '.' and '('.
        //' ' gets globals
        //and any other char gets globals on token and templates.

        //we have to get the qualifier. e.g. bla.foo = foo is the qualifier.
        if (activationToken.indexOf('.') != -1) {
            while (endsWithSomeChar(new char[] { '.','[' }, activationToken) == false
                    && activationToken.length() > 0) {

                qualifier = activationToken.charAt(activationToken.length() - 1) + qualifier;
                activationToken = activationToken.substring(0, activationToken.length() - 1);
            }
        } else { //everything is a part of the qualifier.
            qualifier = activationToken.trim();
            activationToken = "";
        }
        return new String[]{activationToken, qualifier};
    }

    
    /**
     * @param theDoc
     * @param documentOffset
     * @return
     * @throws BadLocationException
     */
    private static int eatFuncCall(IDocument theDoc, int documentOffset) throws BadLocationException {
        String c = theDoc.get(documentOffset, 1);
        if(c.equals(")") == false){
            throw new AssertionError("Expecting ) to eat callable. Received: "+c);
        }
        
        while(documentOffset > 0 && theDoc.get(documentOffset, 1).equals("(") == false){
            documentOffset -= 1;
        }
        
        return documentOffset;
    }


    /**
     * Checks if the activationToken ends with some char from cs.
     */
    private static boolean endsWithSomeChar(char cs[], String activationToken) {
        for (int i = 0; i < cs.length; i++) {
            if (activationToken.endsWith(cs[i] + "")) {
                return true;
            }
        }
        return false;

    }
    
    /**
     * @param pythonAndTemplateProposals
     * @param qualifier
     * @return
     */
    public ICompletionProposal[] onlyValidSorted(List pythonAndTemplateProposals, String qualifier) {
        //FOURTH: Now, we have all the proposals, only thing is deciding wich ones are valid (depending on
        //qualifier) and sorting them correctly.
        Collection returnProposals = new HashSet();
        String lowerCaseQualifier = qualifier.toLowerCase();
        
        for (Iterator iter = pythonAndTemplateProposals.iterator(); iter.hasNext();) {
            Object o = iter.next();
            if (o instanceof ICompletionProposal) {
                ICompletionProposal proposal = (ICompletionProposal) o;
            
	            if (proposal.getDisplayString().toLowerCase().startsWith(lowerCaseQualifier)) {
	                returnProposals.add(proposal);
	            }
            }else{
                throw new RuntimeException("Error: expected instanceof ICompletionProposal and received: "+o.getClass().getName());
            }
        }

        ICompletionProposal[] proposals = new ICompletionProposal[returnProposals.size()];

        // and fill with list elements
        returnProposals.toArray(proposals);

        Arrays.sort(proposals, proposalsComparator);
        return proposals;
    }

    /**
     * Compares proposals so that we can order them.
     */
    private ProposalsComparator proposalsComparator = new ProposalsComparator();

}