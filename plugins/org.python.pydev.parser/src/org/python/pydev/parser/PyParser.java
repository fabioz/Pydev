/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.parser;

import java.io.File;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.texteditor.MarkerUtilities;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.REF;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.Tuple3;
import org.python.pydev.core.callbacks.ICallback;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.parser.ChangedParserInfoForObservers;
import org.python.pydev.core.parser.ErrorParserInfoForObservers;
import org.python.pydev.core.parser.IParserObserver;
import org.python.pydev.core.parser.IParserObserver2;
import org.python.pydev.core.parser.IParserObserver3;
import org.python.pydev.core.parser.IPyParser;
import org.python.pydev.parser.fastparser.FastParser;
import org.python.pydev.parser.grammar24.PythonGrammar24;
import org.python.pydev.parser.grammar25.PythonGrammar25;
import org.python.pydev.parser.grammar26.PythonGrammar26;
import org.python.pydev.parser.grammar27.PythonGrammar27;
import org.python.pydev.parser.grammar30.PythonGrammar30;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ReaderCharStream;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.stmtType;

/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 * 
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

@SuppressWarnings("restriction")
public class PyParser implements IPyParser {

    /**
     * Just for tests: show whenever we're not able to parse some file.
     */
    public static boolean DEBUG_SHOW_PARSE_ERRORS = false;
    
    /**
     * just for tests, when we don't have any editor
     */
    public static boolean ACCEPT_NULL_INPUT_EDITOR = false;
    
    /**
     * Defines whether we should use the fast stream or not
     */
    public static boolean USE_FAST_STREAM = true;
    
    /**
     * this is the document we should parse 
     */
    private volatile IDocument document;

    /**
     * ast for the last successful parsing
     */
    private SimpleNode root = null; 
    
    /**
     * listens to changes in the document
     */
    private IDocumentListener documentListener; 

    /**
     * listeners that get notified of successful or unsuccessful parser achievements
     */
    private ArrayList<IParserObserver> parserListeners; 

    /**
     * used to enable tracing in the grammar
     */
    public static boolean ENABLE_TRACING = false;

    /**
     * this is the object that will keep parser schedules for us (and will call us for doing parsing when requested)
     */
    private ParserScheduler scheduler;
    
    /**
     * indicates we should do analysis only on doc save
     */
    private boolean useAnalysisOnlyOnDocSave;

    /**
     * This is the version of the grammar that should be used for this parser
     */
    private IGrammarVersionProvider grammarVersionProvider;

    /**
     * Identifies whether this parser is disposed.
     */
    private volatile boolean disposed = false;

    
    public static String getGrammarVersionStr(int grammarVersion){
        if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4){
            return "grammar: Python 2.4";
            
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5){
            return "grammar: Python 2.5";
            
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6){
        	return "grammar: Python 2.6";
        	
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7){
            return "grammar: Python 2.7";
            
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0){
        	return "grammar: Python 3.0";
        	
        }else if(grammarVersion == IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_CYTHON){
            return "grammar: Cython";
            
        }else{
            return "grammar: unrecognized: "+grammarVersion;
        }
    }

    /**
     * Should only be called for testing. Does not register as a thread.
     */
    public PyParser(IGrammarVersionProvider grammarVersionProvider) {
        if(grammarVersionProvider == null){
            grammarVersionProvider = new IGrammarVersionProvider(){
                public int getGrammarVersion() {
                    return IPythonNature.LATEST_GRAMMAR_VERSION;
                }
            };
        }
        this.grammarVersionProvider = grammarVersionProvider;
        parserListeners = new ArrayList<IParserObserver>();
        scheduler = new ParserScheduler(this);

        documentListener = new IDocumentListener() {

            public void documentChanged(DocumentEvent event) {
                if(useAnalysisOnlyOnDocSave){
                    //if we're doing analysis only on doc change, the parser will not give any changes
                    //to the scheduler, so, we won't have any parse events to respond to
                    return;
                    
                }
                String text = event.getText();
                
                boolean parseNow = true;
                if (event == null || text == null ) {
                    parseNow = false;
                }
                if(parseNow){
                    if(text.indexOf("\n") == -1 && text.indexOf("\r") == -1){
                        parseNow = false;
                        
                    }
                }
                        
                if(!parseNow){
                    // carriage return in changed text means parse now, anything
                    // else means parse later
                    scheduler.parseLater();
                } else {
                    scheduler.parseNow();
                }
            }

            public void documentAboutToBeChanged(DocumentEvent event) {
            }
        };

    }
    
    /**
     * Ok, create the parser for an editor
     * 
     * @param editorView
     */
    public PyParser(IPyEdit editorView) {
        this(getGrammarProviderFromEdit(editorView));
    }

    /**
     * @param editorView this is the editor that we're getting in the parser
     * @return a provider signaling the grammar to be used for the parser.
     * @throws MisconfigurationException 
     */
    private static IGrammarVersionProvider getGrammarProviderFromEdit(IPyEdit editorView){
        return editorView.getGrammarVersionProvider();
    }


    /**
     * should be called when the editor is disposed
     */
    public void dispose() {
        this.disposed = true;
        this.scheduler.dispose();
        
        // remove the listeners
        if (document != null){
            document.removeDocumentListener(documentListener);
        }
        synchronized(parserListeners){
            parserListeners.clear();
        }
    }

    public SimpleNode getRoot() {
        return root;
    }

    public void notifySaved() {
        //force parse on save
        forceReparse();
    }
    
    /**
     * @return false if we asked a reparse and it will not be scheduled because a reparse is already in action.
     */
    public boolean forceReparse(Object ... argsToReparse){
        if(disposed){
            return true; //reparse didn't happen, but no matter what happens, it won't happen anyways
        }
        return scheduler.parseNow(true, argsToReparse);
    }
    

    /**
     * This is the input from the editor that we're using in the parse
     */
    private IEditorInput input;
    
    public void setDocument(IDocument document, IEditorInput input) {
        setDocument(document, true, input);
    }
    
    public synchronized void setDocument(IDocument doc, boolean addToScheduler, IEditorInput input) {
        this.input = input;
        // Cleans up old listeners
        if (this.document != null) {
            this.document.removeDocumentListener(documentListener);
        }

        // Set up new listener
        this.document = doc;
        if (doc == null) {
            Log.log("No document in PyParser::setDocument?");
            return;
        }

        doc.addDocumentListener(documentListener);
        
        if(addToScheduler){
            // Reparse document on the initial set (force it)
            scheduler.parseNow(true);
        }
    }

    // ---------------------------------------------------------------------------- listeners
    /** stock listener implementation */
    public void addParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized(parserListeners){
            if (!parserListeners.contains(listener)){
                parserListeners.add(listener);
            }
        }
    }

    /** stock listener implementation */
    public void removeParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized(parserListeners){
            parserListeners.remove(listener);
        }
    }

    
    // ---------------------------------------------------------------------------- notifications
    /**
     * stock listener implementation event is fired whenever we get a new root
     * @param original 
     */
    @SuppressWarnings("unchecked")
    protected void fireParserChanged(ChangedParserInfoForObservers info) {
        this.root = (SimpleNode) info.root;
        List<IParserObserver> temp;
        synchronized(parserListeners){
        	temp = new ArrayList<IParserObserver>(parserListeners);
        }
        
		for (IParserObserver l : temp) { 
            //work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
            if(l instanceof IParserObserver3){
                ((IParserObserver3)l).parserChanged(info);
                
            }else if(l instanceof IParserObserver2){
                ((IParserObserver2)l).parserChanged(info.root, info.file, info.doc, info.argsToReparse);
                
            }else{
                l.parserChanged(info.root, info.file, info.doc);
            }
        }

        List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
        for (IParserObserver observer : participants) {
            if(observer instanceof IParserObserver3){
                ((IParserObserver3)observer).parserChanged(info);
                
            }else if(observer instanceof IParserObserver2){
                ((IParserObserver2)observer).parserChanged(info.root, info.file, info.doc, info.argsToReparse);
                
            }else{
                observer.parserChanged(info.root, info.file, info.doc);
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     * @param original 
     */
    @SuppressWarnings("unchecked")
    protected void fireParserError(ErrorParserInfoForObservers info) {
    	List<IParserObserver> temp;
        synchronized(parserListeners){
            temp = new ArrayList<IParserObserver>(parserListeners);
        }
		for (IParserObserver l : temp) {//work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
            if(l instanceof IParserObserver3){
                ((IParserObserver3)l).parserError(info);
                
            }else if(l instanceof IParserObserver2){
                ((IParserObserver2)l).parserError(info.error, info.file, info.doc, info.argsToReparse);
                    
            }else{
                l.parserError(info.error, info.file, info.doc);
            }
        }
        List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
        for (IParserObserver observer : participants) {
            if(observer instanceof IParserObserver3){
                ((IParserObserver3)observer).parserError(info);
                
            }else if(observer instanceof IParserObserver2){
                ((IParserObserver2)observer).parserError(info.error, info.file, info.doc, info.argsToReparse);
                
            }else{
                observer.parserError(info.error, info.file, info.doc);
            }
        }
    }

    // ---------------------------------------------------------------------------- parsing

    /**
     * Parses the document, generates error annotations
     * 
     * @param argsToReparse: will be passed to fireParserError / fireParserChanged so that the IParserObserver2
     * can check it. This is useful when the reparse was done with some specific thing in mind, so that its requestor
     * can pass some specific thing to the parser observers
     * 
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public Tuple<SimpleNode, Throwable> reparseDocument(Object ... argsToReparse) {
        
        //get the document ast and error in object
        int version;
        try{
            version = grammarVersionProvider.getGrammarVersion();
        }catch(MisconfigurationException e1){
            //Ok, we cannot get it... let's put on the default
            version = IGrammarVersionProvider.LATEST_GRAMMAR_VERSION;
        }
        long documentTime = System.currentTimeMillis();
        Tuple<SimpleNode, Throwable> obj = reparseDocument(new ParserInfo(document, version));
        
        IFile original = null;
        IAdaptable adaptable = null;
        
        if (input == null){
            return obj;
        }
        
        original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
        if(original != null){
            adaptable = original;
            
        }else{
            //probably an external file, may have some location provider mechanism
            //it may be org.eclipse.ui.internal.editors.text.JavaFileEditorInput
            adaptable = input;
        }
        
        //delete the markers
        if (original != null){
            try {
                deleteErrorMarkers(original);
            } catch (ResourceException e) {
                //ok, if it is a resource exception, it may have happened because the resource does not exist anymore
                //so, there is no need to log this failure
                if(original.exists()){
                    Log.log(e);
                }
            } catch (CoreException e) {
                Log.log(e);
            }
            
        }else if(adaptable == null){
            //ok, we have nothing... maybe we are in tests...
            if (!PyParser.ACCEPT_NULL_INPUT_EDITOR){
                throw new RuntimeException("Null input editor received in parser!");
            }
        }
        //end delete the markers
        
        
        if(disposed){
            //if it was disposed in this time, don't fire any notification nor return anything valid.
            return new Tuple<SimpleNode, Throwable>(null, null);
        }

        
        if(obj.o1 != null){
            //Ok, reparse successful, lets erase the markers that are in the editor we just parsed
            //Note: we may get the ast even if errors happen (and we'll notify in that case too).
            ChangedParserInfoForObservers info = new ChangedParserInfoForObservers(obj.o1, adaptable, document, documentTime, argsToReparse);
            fireParserChanged(info);
        }
        
        if(obj.o2 instanceof ParseException || obj.o2 instanceof TokenMgrError){
            ErrorParserInfoForObservers info = new ErrorParserInfoForObservers(obj.o2, adaptable, document, argsToReparse);
            fireParserError(info);
        }
        
        return obj;
    }

    /**
     * This function will remove the markers related to errors.
     * @param resource the file that should have the markers removed
     * @throws CoreException
     */
    public static void deleteErrorMarkers(IResource resource) throws CoreException {
        IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        if(markers.length > 0){
            resource.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        }
    }
    

    
    //static methods that can be used to get the ast (and error if any) --------------------------------------
    

    public final static class ParserInfo{
        public IDocument document;
        
        
        /**
         * A set with the lines that were changed when trying to make the document parseable
         */
        public final Set<Integer> linesChanged = new HashSet<Integer>();
        
        /**
         * This is the version of the grammar to be used 
         * @see IPythonNature.GRAMMAR_XXX constants
         */
        public final int grammarVersion;
        
        /**
         * The module name of the contents parsed (may be null)
         */
        public final String moduleName;
        
        /**
         * The file that's been parsed (may be null)
         */
        public final File file;
        
        /**
         * @param grammarVersion: see IPythonNature.GRAMMAR_XXX constants
         */
        public ParserInfo(IDocument document, int grammarVersion){
            this(document, grammarVersion, null, null);
        }
        
        public ParserInfo(IDocument document, IGrammarVersionProvider nature) throws MisconfigurationException{
            this(document, nature.getGrammarVersion());
        }
        
        public ParserInfo(IDocument document, IGrammarVersionProvider nature, String moduleName, File file) throws MisconfigurationException{
            this(document, nature.getGrammarVersion(), moduleName, file);
        }
        

        public ParserInfo(IDocument document, int grammarVersion, String name, File f) {
            this.document = document;
            this.grammarVersion = grammarVersion;
            this.moduleName = name;
            this.file = f;
        }
        
        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("ParserInfo [");
            buf.append("file:");
            buf.append(file);
            buf.append("\nmoduleName:");
            buf.append(moduleName);
            buf.append("]");
            return buf.toString();
        }
    }
    
    /**
     * This list of callbacks is mostly used for testing, so that we can check what's been parsed.
     */
    public final static List<ICallback<Object, Tuple3<SimpleNode, Throwable, ParserInfo>>> successfulParseListeners = 
        new ArrayList<ICallback<Object, Tuple3<SimpleNode, Throwable, ParserInfo>>>();
    
    
    /**
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public static Tuple<SimpleNode, Throwable> reparseDocument(ParserInfo info) {
        if(info.grammarVersion == IPythonNature.GRAMMAR_PYTHON_VERSION_CYTHON){
            IDocument doc = info.document;
            return createCythonAst(doc);
        }
        
        // create a stream with document's data
        String startDoc = info.document.get();
        
        if(startDoc.trim().length() == 0){
            //If empty, don't bother to parse!
            return new Tuple<SimpleNode, Throwable>(new Module(new stmtType[0]), null);
        }
        
        int length = startDoc.length();
        int skipAtStart = 0;
        if(startDoc.startsWith(REF.BOM_UTF8)){
            skipAtStart = REF.BOM_UTF8.length();
        }else if(startDoc.startsWith(REF.BOM_UNICODE)){
            skipAtStart = REF.BOM_UNICODE.length();
        }
        
        int addAtEnd = 0;
        if(!startDoc.endsWith("\n") && !startDoc.endsWith("\r")){
            addAtEnd = 1;
        }
        
        char []charArray = new char[length-skipAtStart+addAtEnd];
        startDoc.getChars(skipAtStart, length, charArray, 0);
        if(addAtEnd > 0){
            charArray[charArray.length-1] = '\n';
        }
        
        CharStream in = null;
        if(USE_FAST_STREAM){
            in = new FastCharStream(charArray);
        }else{
            in = new ReaderCharStream(new StringReader(new String(charArray)));
            throw new RuntimeException("This char stream reader was deprecated (it's maintained only for testing purposes).");
        }
        startDoc = null; //it can be garbage-collected now.
        

        Tuple<SimpleNode, Throwable> returnVar = new Tuple<SimpleNode, Throwable>(null, null);
        IGrammar grammar = null;
        try {
            
            switch(info.grammarVersion){
                case IPythonNature.GRAMMAR_PYTHON_VERSION_2_4:
                    grammar = new PythonGrammar24(in);
                    break;
                case IPythonNature.GRAMMAR_PYTHON_VERSION_2_5:
                    grammar = new PythonGrammar25(in);
                    break;
                case IPythonNature.GRAMMAR_PYTHON_VERSION_2_6:
                    grammar = new PythonGrammar26(in);
                    break;
                case IPythonNature.GRAMMAR_PYTHON_VERSION_2_7:
                    grammar = new PythonGrammar27(in);
                    break;
                case IPythonNature.GRAMMAR_PYTHON_VERSION_3_0:
                    grammar = new PythonGrammar30(in);
                    break;
                //case CYTHON: already treated in the beggining of this method.
                default:
                    throw new RuntimeException("The grammar specified for parsing is not valid: "+info.grammarVersion);
            }
            
            
            if(ENABLE_TRACING){
                //grammar has to be generated with debugging info for this to make a difference
                grammar.enable_tracing();
            }
            SimpleNode newRoot = grammar.file_input(); // parses the file
            returnVar.o1 = newRoot;
            
            //only notify successful parses
            if(successfulParseListeners.size() > 0){
                Tuple3<SimpleNode, Throwable, ParserInfo> param = new Tuple3<SimpleNode, Throwable, ParserInfo>(
                        returnVar.o1, returnVar.o2, info);
                
                for(ICallback<Object, Tuple3<SimpleNode, Throwable, ParserInfo>> callback: successfulParseListeners){
                    callback.call(param);
                }
            }        
            
            returnVar.o2 = grammar.getErrorOnParsing();

        } catch (Throwable e) {
            //ok, some error happened when trying the parse... let's go and clear the local info before doing
            //another parse.
            if(DEBUG_SHOW_PARSE_ERRORS){
                e.printStackTrace();
            }
            
            //If the grammar was not created, the problem wasn't in the parsing... so, let's just rethrow the error
            if(grammar == null){
                throw new RuntimeException(e);
            }
            
            //We have to change it for the 1st error we got (the one in catch is the last one).
            Throwable errorOnParsing = grammar.getErrorOnParsing();
            if(errorOnParsing != null){
                e = errorOnParsing;
            }else if(DEBUG_SHOW_PARSE_ERRORS){
                System.out.println("Unhandled error");
                e.printStackTrace();
            }
            
            startDoc = null;
            in = null;
            grammar = null;
            
            if(e instanceof ParseException || e instanceof TokenMgrError){
                returnVar = new Tuple<SimpleNode, Throwable>(null, e);
                
            }else if(e.getClass().getName().indexOf("LookaheadSuccess") != -1){
                //don't log this kind of error...
            }else{
                Log.log(e);
            }
        
        } 
        
        if(DEBUG_SHOW_PARSE_ERRORS){
            if(returnVar.o1 == null){
                System.out.println("Unable to parse "+info);
            }
        }
//        System.out.println("Output grammar: "+returnVar);
        return returnVar;
    }

    public static Tuple<SimpleNode, Throwable> createCythonAst(IDocument doc) {
        List<stmtType> classesAndFunctions = FastParser.parseCython(doc);
        return new Tuple<SimpleNode, Throwable>(
                new Module(classesAndFunctions.toArray(new stmtType[classesAndFunctions.size()])), null);
    }

    


    public void resetTimeoutPreferences(boolean useAnalysisOnlyOnDocSave) {
        this.useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave;
    }

    public List<IParserObserver> getObservers() {
    	synchronized(parserListeners){
    		return new ArrayList<IParserObserver>(this.parserListeners);
    	}
    }

    /**
     * Adds the error markers for some error that was found in the parsing process.
     * 
     * @param error the error find while parsing the document
     * @param resource the resource that should have the error added
     * @param doc the document with the resource contents
     * @return the error description (or null)
     * 
     * @throws BadLocationException
     * @throws CoreException
     */
    public static ErrorDescription createParserErrorMarkers(Throwable error, IAdaptable resource, IDocument doc)
            throws BadLocationException, CoreException {
        ErrorDescription errDesc;
        if(resource == null){
            return null;
        }
        IResource fileAdapter = (IResource) resource.getAdapter(IResource.class);
        if(fileAdapter == null){
            return null;
        }
    
        errDesc = createErrorDesc(error, doc);
        
        Map<String, Object> map = new HashMap<String, Object>();
        
        map.put(IMarker.MESSAGE, errDesc.message);
        map.put(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
        map.put(IMarker.LINE_NUMBER, errDesc.errorLine);
        map.put(IMarker.CHAR_START, errDesc.errorStart);
        map.put(IMarker.CHAR_END, errDesc.errorEnd);
        map.put(IMarker.TRANSIENT, true);
        MarkerUtilities.createMarker(fileAdapter, map, IMarker.PROBLEM);
        return errDesc;
    }

    
    /**
     * Creates the error description for a given error in the parse.
     */
    private static ErrorDescription createErrorDesc(Throwable error, IDocument doc) throws BadLocationException {
        int errorStart = -1;
        int errorEnd = -1;
        int errorLine = -1;
        String message = null;
        if (error instanceof ParseException) {
            ParseException parseErr = (ParseException) error;
            
            // Figure out where the error is in the document, and create a
            // marker for it
            if(parseErr.currentToken == null){
                IRegion endLine = doc.getLineInformationOfOffset(doc.getLength());
                errorStart = endLine.getOffset();
                errorEnd = endLine.getOffset() + endLine.getLength();
    
            }else{
                Token errorToken = parseErr.currentToken.next != null ? parseErr.currentToken.next : parseErr.currentToken;
                IRegion startLine = doc.getLineInformation(getDocPosFromAstPos(errorToken.beginLine));
                IRegion endLine;
                if (errorToken.endLine == 0){
                    endLine = startLine;
                }else{
                    endLine = doc.getLineInformation(getDocPosFromAstPos(errorToken.endLine));
                }
                errorStart = startLine.getOffset() + getDocPosFromAstPos(errorToken.beginColumn);
                errorEnd = endLine.getOffset() + errorToken.endColumn;
            }
            message = parseErr.getMessage();
    
        } else if(error instanceof TokenMgrError){
            TokenMgrError tokenErr = (TokenMgrError) error;
            IRegion startLine = doc.getLineInformation(tokenErr.errorLine - 1);
            errorStart = startLine.getOffset();
            errorEnd = startLine.getOffset() + tokenErr.errorColumn;
            message = tokenErr.getMessage();
        } else{
            Log.log("Error, expecting ParseException or TokenMgrError. Received: "+error);
            return new ErrorDescription(null, 0, 0, 0);
        }
        errorLine = doc.getLineOfOffset(errorStart); 
    
        // map.put(IMarker.LOCATION, "Whassup?"); this is the location field
        // in task manager
        if (message != null) { // prettyprint
            message = message.replaceAll("\\r\\n", " ");
            message = message.replaceAll("\\r", " ");
            message = message.replaceAll("\\n", " ");
        }
        
        
        return new ErrorDescription(message, errorLine, errorStart, errorEnd);
    }

    /**
     * The ast position starts at 1 and the document starts at 0 (but it could be that we had nothing valid
     * and received an invalid position, so, we must treat that).
     */
    private static int getDocPosFromAstPos(int astPos) {
        if(astPos > 0){
            astPos--;
        }
        return astPos;
    }


    
}

