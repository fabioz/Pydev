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
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.python.parser.IParserHost;
import org.python.parser.ParseException;
import org.python.parser.PythonGrammar;
import org.python.parser.ReaderCharStream;
import org.python.parser.SimpleNode;
import org.python.parser.TokenMgrError;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.DocUtils;
import org.python.pydev.core.log.Log;

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
    private static class ParsingThread extends Thread {
        boolean okToGo;

        private PyParser parser;

        private ParsingThread(PyParser parser) {
            super();
            this.parser = parser;
        }

        public void run() {
            try {
                makeOkAndSleepUntilIdleTimeElapses();
                while(!okToGo){
                    makeOkAndSleepUntilIdleTimeElapses();
                }

                //ok, now we parse it...
                try {
                    parser.reparseDocument();
                } catch (Exception e) {
                    Log.log(e);
                }
                //reset the state
                parser.state = STATE_WAITING;
                
            } finally{
                parser.parseThread = null;
            }
        }

        private void makeOkAndSleepUntilIdleTimeElapses() {
            try {
                okToGo = true;
                sleep(getIdleTimeRequested()); //one sec
            } catch (Exception e) {
            }
        }

        /**
         * @return the idle time to make a parse... this should probably be on the interface
         */
        private int getIdleTimeRequested() {
            return 500;
        }
    }
    
    /**
     * just for tests, when we don't have any editor
     */
    static boolean ACCEPT_NULL_EDITOR = false; 

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
    private ArrayList<IParserListener> parserListeners; 

    /**
     * used to do parsings in a thread
     */
    private ParsingThread parsingThread; 
    
    /**
     * indicates that currently nothing is happening
     */
    public static final int STATE_WAITING = 0; 
    
    /**
     * indicates whether some parse later has been requested
     */
    public static final int STATE_PARSE_LATER = 1; 

    /**
     * indicates if a thread is currently doing a parse action
     */
    public static final int STATE_DOING_PARSE = 2;

    /**
     * 5 seconds
     */
    protected static final long TIME_TO_PARSE_LATER = 5000;
    
    /**
     * initially we're waiting
     */
    private int state = STATE_WAITING;
    
    /**
     * this is the exact time a parse later was requested
     */
    private long timeParseLaterRequested = 0;
    
    /**
     * this is the exact time the last parse was requested
     */
    private long timeLastParse = 0;

    /**
     * this thread is only created at parse time (and on the end of the parse it is put as null)
     */
    private Thread parseThread;
    
    /**
     * used to enable tracing in the grammar
     */
    static boolean ENABLE_TRACING = false;

    /**
     * should only be called for testing. does not register as a thread
     */
    PyParser() {
        parserListeners = new ArrayList<IParserListener>();

        parsingThread = new ParsingThread(this);

        parsingThread.setPriority(Thread.MIN_PRIORITY);
        documentListener = new IDocumentListener() {

            public void documentChanged(DocumentEvent event) {
                if (event == null || event.getText() == null || event.getText().indexOf("\n") == -1) {
                    // carriage return in changed text means parse now, anything
                    // else means parse later
                    parseLater();
                } else {
                    parseNow();
                }
            }

            public void documentAboutToBeChanged(DocumentEvent event) {
            }
        };

    }

    private void parseNow() {
        if(state != STATE_DOING_PARSE){
            state = STATE_DOING_PARSE; // the parser will reset it later
            timeLastParse = System.currentTimeMillis();
            if(parseThread == null){
                parseThread = new Thread(parsingThread);
                parseThread.setPriority(Thread.MIN_PRIORITY); //parsing is low priority
                parseThread.start();
            }
        }else{
            //another request... we keep waiting until the user stops adding requests
            parsingThread.okToGo = false;
        }
    }
    
    private void parseLater() {
        if(state != STATE_DOING_PARSE && state != STATE_PARSE_LATER){
            state = STATE_PARSE_LATER;
            //ok, the time for this request is:
            timeParseLaterRequested = System.currentTimeMillis();
            new Thread(){
                @Override
                public void run() {
                    try {
                        sleep(TIME_TO_PARSE_LATER);
                    } catch (Exception e) {
                        //that's ok
                    }
                    //ok, no parse happened while we were sleeping
                    if( state == STATE_PARSE_LATER && timeLastParse < timeParseLaterRequested){
                        parseNow();
                    }
                }
            }.start();
        }
        
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
        if (document != null)
            document.removeDocumentListener(documentListener);
        parserListeners.clear();
    }

    public SimpleNode getRoot() {
        return root;
    }

    public void setDocument(IDocument document) {
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
        // Reparse document on the initial set
        parseNow();
    }

    // ---------------------------------------------------------------------------- listeners
    /** stock listener implementation */
    public void addParseListener(IParserListener listener) {
        Assert.isNotNull(listener);
        if (!parserListeners.contains(listener))
            parserListeners.add(listener);
    }

    /** stock listener implementation */
    public void removeParseListener(IParserListener listener) {
        Assert.isNotNull(listener);
        parserListeners.remove(listener);
    }

    
    // ---------------------------------------------------------------------------- notifications
    /**
     * stock listener implementation event is fired whenever we get a new root
     */
    protected void fireParserChanged(SimpleNode root) {
        this.root = root;
        if (parserListeners.size() > 0) {
            ArrayList list = new ArrayList(parserListeners);
            Iterator e = list.iterator();
            while (e.hasNext()) {
                IParserListener l = (IParserListener) e.next();
                l.parserChanged(root);
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     */
    protected void fireParserError(Throwable error) {
        if (parserListeners.size() > 0) {
            ArrayList list = new ArrayList(parserListeners);
            Iterator e = list.iterator();
            while (e.hasNext()) {
                IParserListener l = (IParserListener) e.next();
                l.parserError(error);
            }
        }
    }

    // ---------------------------------------------------------------------------- parsing
    /**
     * reparses the document getting the nature associated to the corresponding editor 
     * @return
     */
    Object[] reparseDocument() {
        return reparseDocument(editorView.getPythonNature());
    }
    /**
     * Parses the document, generates error annotations
     * 
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    Object[] reparseDocument(IPythonNature nature) {
        
        //get the document ast and error in object
        Object obj[] = reparseDocument(new ParserInfo(document, true, nature, -1));
        
        if(obj[0] != null && obj[0] instanceof SimpleNode){
            //ok, reparse succesful, lets erase the markers that are in the editor we just parsed
            if(editorView != null){
                IEditorInput input = editorView.getEditorInput();
                if (input == null){
                    return null;
                }
                
                IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
                if (original != null){
                    try {
                        original.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                    } catch (CoreException e) {
                        Log.log(e);
                    }
                }
                fireParserChanged((SimpleNode) obj[0]);
            }else{
                //ok, we have no editor view
                if (!PyParser.ACCEPT_NULL_EDITOR){
                    throw new RuntimeException("Null editor received in parser!");
                }
            }
        }
        
        if(obj[1] != null && obj[1] instanceof ParseException){
            fireParserError((ParseException) obj[1]);
        }
        
        if(obj[1] != null && obj[1] instanceof TokenMgrError){
            fireParserError((TokenMgrError) obj[1]);
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
        public List linesChanged = new ArrayList();
        public ParseException parseErr;
        
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
        String initialDoc = info.document.get();
        
        if(info.initial == null){
            info.initial = initialDoc;
        }
        
        StringReader inString = new StringReader(initialDoc);
        ReaderCharStream in = new ReaderCharStream(inString);
        IParserHost host = new CompilerAPI();
        PythonGrammar grammar = new PythonGrammar(in, host);
        if(ENABLE_TRACING){
            //grammar has to be generated with debugging info for this to make a difference
            grammar.enable_tracing();
        }


        try {
            SimpleNode newRoot = grammar.file_input(); // parses the file
            return new Object[]{newRoot,null};


        } catch (ParseException parseErr) {
            SimpleNode newRoot = null;

            if(info.parseErr == null){
                info.parseErr = parseErr;
            }
            
            if (info.stillTryToChangeCurrentLine){
                newRoot = tryReparseAgain(info, info.parseErr);
            } else {
                info.currentLine = -1;
                info.document = new Document(info.initial);
                newRoot = tryReparseAgain(info, info.parseErr);
            }
            
            return new Object[]{newRoot, parseErr};
            
        
        } catch (TokenMgrError tokenErr) {
            SimpleNode newRoot = null;
    
            if (info.stillTryToChangeCurrentLine){
                newRoot = tryReparseAgain(info, tokenErr);
            }
            
            return new Object[]{newRoot, tokenErr};

        } catch (Exception e) {
            Log.log(e);
            return null;
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
    
}

