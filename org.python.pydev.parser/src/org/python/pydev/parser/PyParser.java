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
    
    /**
     * just for tests, when we don't have any editor
     */
    static boolean ACCEPT_NULL_EDITOR = false; 

    IDocument document;

    IPyEdit editorView;

    SimpleNode root = null; // Document root

    IDocumentListener documentListener; // listens to changes in the document

    ArrayList parserListeners; // listeners that get notified

    final static int PARSE_LATER_INTERVAL = 20; // 20 = 2 seconds

    static boolean ENABLE_TRACING = false;

    boolean parseNow = false; // synchronized access by ParsingThread

    
    /*
     * counter how to parse. 0 means do not parse, > 0 means wait this many
     * loops in main thread
     */
    int parseLater = 0; // synchronized access by ParsingThread

    /**
     * should only be called for testing. does not register as a thread
     */
    PyParser() {
        parserListeners = new ArrayList();
    }
    
    public PyParser(IPyEdit editorView) {
        this();
        this.editorView = editorView;
        ParsingThread.getParsingThread().register(this);
    }

    public void parseNow() {
        parseNow = true;
    }

    public void parseLater() {
        parseNow = false;
        parseLater = PARSE_LATER_INTERVAL; // delay of 1 second
    }

    public void dispose() {
        // remove the listeners
        if (document != null)
            document.removeDocumentListener(documentListener);
        parserListeners.clear();
        ParsingThread.getParsingThread().unregister(this);
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
        document.addDocumentListener(documentListener);
        // Reparse document on the initial set
        parseNow();
    }

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
        public boolean changedCurrentLine=true; 
        public IPythonNature nature;
        public int currentLine=-1;
        public String initial = null;
        public List linesChanged = new ArrayList();
        
        public ParserInfo(IDocument document, boolean changedCurrentLine, IPythonNature nature){
            this.document = document;
            this.changedCurrentLine = changedCurrentLine;
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
            
            if (info.changedCurrentLine){
                newRoot = tryReparseAgain(info, parseErr);
            } else {
                info.currentLine = -1;
                info.document = new Document(info.initial);
                newRoot = tryReparseAgain(info, parseErr);
            }
            
            return new Object[]{newRoot, parseErr};
            
        
        } catch (TokenMgrError tokenErr) {
            SimpleNode newRoot = null;
    
            if (info.changedCurrentLine){
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
                line = tokenErr.currentToken.beginLine-1;
            
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
            info.changedCurrentLine = false;
	        return (SimpleNode) reparseDocument(info)[0];
        }
        return null;
    }
    
}

/**
 * Utility thread that reparses document as needed.
 * 
 * Singleton.
 * 
 * Current algorithm is: - if parseLater is called, parse 10 main loops later -
 * if parseNow is called, parse immediately
 */

class ParsingThread extends Thread {

    private static ParsingThread thread = null;

    private static ArrayList parsers = new ArrayList(); // synchronized access

    // only

    private boolean done = false;

    private ParsingThread() {
        setName("Pydev parsing thread");
    }

    static public ParsingThread getParsingThread() {
        synchronized (ParsingThread.class) {
            if (thread == null) {
                thread = new ParsingThread();
                thread.start();
            }
            return thread;
        }
    }

    public void register(PyParser parser) {
        synchronized (parsers) {
            parsers.add(parser);
        }
    }

    public void unregister(PyParser parser) {
        synchronized (parsers) {
            parser.parseNow = false;
            parser.parseLater = 0;
            parsers.remove(parser);
            if (parsers.size() == 0) {
                done = true;
                thread = null;
            }
        }
    }

    public void run() {
        // wait for document change, and reparse
        try {
            while (!done) {
                try {
                    ArrayList parseUs = new ArrayList();

                    // Populate the list of parsers waiting to be parsed
                    synchronized (parsers) {
                        Iterator i = parsers.iterator();
                        while (i.hasNext()) {
                            PyParser p = (PyParser) i.next();
                            p.parseLater--;
                            if (p.parseLater == 1)
                                p.parseNow = true;
                            if (p.parseNow)
                                parseUs.add(p);
                        }
                    }

                    // Now parse the queue
                    Iterator i = parseUs.iterator();
                    while (i.hasNext()) {
                        PyParser p = (PyParser) i.next();
                        if (p.parseNow) {
                            p.parseNow = false;
                            p.parseLater = 0;
                            p.reparseDocument();
                        }
                    }
                    sleep(100); // sleep a bit, to avoid flicker
                } catch (Exception e) {
                    Log.log(e);
                }
            }
        } finally {
            if (thread == this)
                thread = null;
        }
    }
}