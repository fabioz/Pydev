/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.parser.jython.CharStream;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.IParserHost;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.PythonGrammar;
import org.python.pydev.parser.jython.ReaderCharStream;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.commentType;

/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 * 
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

public class PyParser {
    /**
     * just for tests, when we don't have any editor
     */
    public static boolean ACCEPT_NULL_EDITOR = false;
    
    /**
     * Defines whether we should use the fast stream or not
     */
    public static boolean USE_FAST_STREAM = true;
    
    /**
     * To know whether we should try to do some reparse changing the input
     */
    public static boolean TRY_REPARSE = true; 

    /**
     * this is the document we should parse 
     */
    private IDocument document;

    /**
     * this is the editor associated
     */
    private IPyEdit editorView;

    /**
     * ast for the last succesful parsing
     */
    private SimpleNode root = null; 

    /**
     * listens to changes in the document
     */
    private IDocumentListener documentListener; 

    /**
     * listeners that get notified of succesfull or unsuccessful parser achievements
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
     * indicates the time we should elapse before doing analysis
     */
    private int elapseMillisBeforeAnalysis;
    
    /**
     * should only be called for testing. does not register as a thread
     */
    public PyParser() {
        parserListeners = new ArrayList<IParserObserver>();
        scheduler = new ParserScheduler(this);

        documentListener = new IDocumentListener() {

            public void documentChanged(DocumentEvent event) {
                if (event == null || event.getText() == null || event.getText().indexOf("\n") == -1) {
                    // carriage return in changed text means parse now, anything
                    // else means parse later
                    if(!useAnalysisOnlyOnDocSave){
                        scheduler.parseLater();
                    }
                } else {
                    if(!useAnalysisOnlyOnDocSave){
                        scheduler.parseNow();
                    }
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
        this();
        this.editorView = editorView;
    }


    /**
     * should be called when the editor is disposed
     */
    public void dispose() {
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
        parseNow(true);
    }
    
    public void parseNow(boolean force){
        scheduler.parseNow(force);
    }

    public void setDocument(IDocument document) {
    	setDocument(document, true);
    }
    
    public void setDocument(IDocument document, boolean addToScheduler) {
        // Cleans up old listeners
        if (this.document != null) {
            document.removeDocumentListener(documentListener);
        }

        // Set up new listener
        this.document = document;
        if (document == null) {
            System.err.println("No document in PyParser::setDocument?");
            return;
        }

        document.addDocumentListener(documentListener);
        
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
	protected void fireParserChanged(SimpleNode root, IAdaptable file, IDocument doc) {
        this.root = root;
        synchronized(parserListeners){
        	for (IParserObserver l : parserListeners) {
        		l.parserChanged(root, file, doc);
			}

        	List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
            for (IParserObserver observer : participants) {
                observer.parserChanged(root, file, doc);
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     * @param original 
     */
    @SuppressWarnings("unchecked")
	protected void fireParserError(Throwable error, IAdaptable file, IDocument doc) {
        synchronized(parserListeners){
        	for (IParserObserver l : parserListeners) {
                l.parserError(error, file, doc);
            }
            List<IParserObserver> participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_PARSER_OBSERVER);
            for (IParserObserver observer : participants) {
                observer.parserError(error, file, doc);
            }
        }
    }

    // ---------------------------------------------------------------------------- parsing
    /**
     * reparses the document getting the nature associated to the corresponding editor 
     * @return
     */
    public Object[] reparseDocument() {
    	return reparseDocument(editorView.getPythonNature());
    }
    /**
     * Parses the document, generates error annotations
     * 
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public Object[] reparseDocument(IPythonNature nature) {
        
        //get the document ast and error in object
        Object obj[] = reparseDocument(new ParserInfo(document, true, nature, -1));
        
        IFile original = null;
        IAdaptable adaptable = null;
        
        if(editorView != null){
            IEditorInput input = editorView.getEditorInput();
            if (input == null){
                return null;
            }
            
            original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
            if(original != null){
                adaptable = original;
                
            }else{
                //probabl an external file, may have some location provider mechanism
                //it may be org.eclipse.ui.internal.editors.text.JavaFileEditorInput
                adaptable = input;
            }
        }
        
        if(obj[0] != null && obj[0] instanceof SimpleNode){
            //ok, reparse succesful, lets erase the markers that are in the editor we just parsed
            if (original != null){
                try {
                    original.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                } catch (CoreException e) {
                    Log.log(e);
                }
                
            }else if(adaptable == null){
                //ok, we have nothing... maybe we are in tests...
                if (!PyParser.ACCEPT_NULL_EDITOR){
                    throw new RuntimeException("Null editor received in parser!");
                }
            }
            fireParserChanged((SimpleNode) obj[0], adaptable, document);
        }
        
        if(obj[1] != null && obj[1] instanceof ParseException){
            fireParserError((ParseException) obj[1], adaptable, document);
        }
        
        if(obj[1] != null && obj[1] instanceof TokenMgrError){
            fireParserError((TokenMgrError) obj[1], adaptable, document);
        }
        
        return obj;
    }
    

    
    //static methods that can be used to get the ast (and error if any) --------------------------------------
    

    public static class ParserInfo{
        public IDocument document;
        public boolean stillTryToChangeCurrentLine=true; 
        public IPythonNature nature;
        public int currentLine=-1;
        public String initial = null;
        public List<Integer> linesChanged = new ArrayList<Integer>();
        public ParseException parseErr;
        public boolean tryReparse = TRY_REPARSE;
        
        public ParserInfo(IDocument document, boolean changedCurrentLine, IPythonNature nature){
            this.document = document;
            this.stillTryToChangeCurrentLine = changedCurrentLine;
            this.nature = nature;
        }

        public ParserInfo(IDocument document, boolean changedCurrentLine, IPythonNature nature, int currentLine){
            this(document, changedCurrentLine, nature);
            this.currentLine = currentLine;
        }
    }
    
    /**
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public static Object[] reparseDocument(ParserInfo info) {
        // create a stream with document's data
        String startDoc = info.document.get();
        if(info.initial == null){
            info.initial = startDoc;
        }

        IDocument newDoc = new Document(startDoc);
        StringBuffer endingComments = PySelection.removeEndingComments(newDoc);
        try {
            //make sure it ends with a new line
            newDoc.replace(newDoc.getLength(), 0, "\n");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String initialDoc = newDoc.get();
        
        
        CharStream in = null;
        if(USE_FAST_STREAM){
        	in = new FastCharStream(initialDoc);
        }else{
        	//this should be deprecated in the future (it is still here so that we can evaluate
        	//the changes done by the change of the reader).
	        StringReader inString = new StringReader(initialDoc);
	        in = new ReaderCharStream(inString);
        }
        
        IParserHost host = new CompilerAPI();
        PythonGrammar grammar = null;

        try {
        	grammar = new PythonGrammar(in, host);
        	
        	if(ENABLE_TRACING){
        		//grammar has to be generated with debugging info for this to make a difference
        		grammar.enable_tracing();
        	}
            SimpleNode newRoot = grammar.file_input(); // parses the file
            if(newRoot != null){
                Module m = (Module) newRoot;
                m.addSpecial(new commentType(endingComments.toString()), true);
            }
            return new Object[]{newRoot,null};
		

        } catch (ParseException parseErr) {
            SimpleNode newRoot = null;

            if(info.parseErr == null){
                info.parseErr = parseErr;
            }
            
            if(info.tryReparse){
                if (info.stillTryToChangeCurrentLine){
                    newRoot = tryReparseAgain(info, info.parseErr);
                } else {
                    info.currentLine = -1;
                    info.document = new Document(info.initial);
                    newRoot = tryReparseAgain(info, info.parseErr);
                }
            }
            return new Object[]{newRoot, parseErr};
            
        
        } catch (TokenMgrError tokenErr) {
            SimpleNode newRoot = null;
            
            if(info.tryReparse){
                if (info.stillTryToChangeCurrentLine){
                    newRoot = tryReparseAgain(info, tokenErr);
                }
            }
            
            return new Object[]{newRoot, tokenErr};

        } catch (Exception e) {
            Log.log(e);
            return new Object[]{null, null};
        
        } catch (Throwable e) {
			//PythonGrammar$LookaheadSuccess error: this happens sometimes when the file is
			//not parseable
			if(e.getClass().getName().indexOf("LookaheadSuccess") != -1){
				//don't log this kind of error...
			}else{
				Log.log(e);
			}
			return new Object[]{null, null};
        }
    }

    /**
     * @param tokenErr
     */
    private static SimpleNode tryReparseAgain(ParserInfo info, TokenMgrError tokenErr) {
        int line = -1;
        
        if(info.currentLine > -1){
            line = info.currentLine;
        }else{
            line = tokenErr.errorLine;
        }
        
        return tryReparseChangingLine(info, line);
    }

    /**
     * This method tries to reparse the code again, changing the current line to
     * a 'pass'
     * 
     * Any new errors are ignored, and the error passed as a parameter is fired
     * anyway, so, the utility of this function is trying to make a real model
     * without any problems, so that we can update the outline and ModelUtils
     * with a good aproximation of the code.
     * 
     * @param tokenErr
     */
    private static SimpleNode tryReparseAgain(ParserInfo info, ParseException tokenErr) {
        int line = -1;
        
        if(info.currentLine > -1){
            line = info.currentLine;
        
        }else{
            if(tokenErr.currentToken != null){
                line = tokenErr.currentToken.beginLine-2;
            
    	        boolean okToGo = false;
    	        
    	        while(! okToGo){
    		        if(! lineIn(info.linesChanged, line)){
    		            info.linesChanged.add(new Integer(line));
    		            okToGo = true;
    		            
    		        } else if(info.linesChanged.size() < 10){
    		            line += 1;
    		            
    		        } else{
    		            return null;
    		        }
                }
	        }else{
             return null;   
            }
        }

        return tryReparseChangingLine(info, line);
    }

    /**
     * @param linesChanged
     * @param line
     * @return
     */
    private static boolean lineIn(List linesChanged, int line) {
        for (Iterator iter = linesChanged.iterator(); iter.hasNext();) {
            Integer i = (Integer) iter.next();
            if (i.intValue() == line){
                return true;
            }
        }
        return false;
    }

    /**
     * Try a reparse changing the offending line.
     * 
     * @param document: this is the document to be changed
     * @param line: offending line to be changed to try a reparse.
     * 
     */
    private static SimpleNode tryReparseChangingLine(ParserInfo info, int line) {
        String docToParse = DocUtils.getDocToParseFromLine(info.document, line);
        if(docToParse != null){

            Document doc = new Document(docToParse);
            info.document = doc;
            info.stillTryToChangeCurrentLine = false;
	        return (SimpleNode) reparseDocument(info)[0];
        }
        return null;
    }

    public void reset(boolean useAnalysisOnlyOnDocSave, int elapseMillisBeforeAnalysis) {
        this.useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave;
        this.elapseMillisBeforeAnalysis = elapseMillisBeforeAnalysis;
    }

    public int getIdleTimeRequested() {
        return this.elapseMillisBeforeAnalysis;
    }

    
}

