/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;

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
import org.python.parser.CharStream;
import org.python.parser.IParserHost;
import org.python.parser.ParseException;
import org.python.parser.PythonGrammar;
import org.python.parser.ReaderCharStream;
import org.python.parser.SimpleNode;
import org.python.parser.TokenMgrError;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codecompletion.PyCodeCompletion;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.plugin.PythonNature;

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

    IDocument document;

    PyEdit editorView;

    SimpleNode root = null; // Document root

    IDocumentListener documentListener; // listens to changes in the document

    ArrayList parserListeners; // listeners that get notified

    final static int PARSE_LATER_INTERVAL = 20; // 20 = 2 seconds

    boolean parseNow = false; // synchronized access by ParsingThread

    
    /*
     * counter how to parse. 0 means do not parse, > 0 means wait this many
     * loops in main thread
     */
    int parseLater = 0; // synchronized access by ParsingThread

    public PyParser(PyEdit editorView) {
        this.editorView = editorView;
        parserListeners = new ArrayList();
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
        final PyParser parser = this;
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
     * Parses the document, generates error annotations
     */
    void reparseDocument() {
        
        //get the document ast and error in object
        Object obj[] = reparseDocument(document, true, editorView.getPythonNature());
        
        if(obj[0] != null && obj[0] instanceof SimpleNode){
            IEditorInput input = editorView.getEditorInput();
            if (input == null)
                return;
            IFile original = (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
            if (original != null){
                try {
                    original.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
                } catch (CoreException e) {
                    PydevPlugin.log(e);
                }
            }
            fireParserChanged((SimpleNode) obj[0]);
        }
        
        if(obj[1] != null && obj[1] instanceof ParseException){
            fireParserError((ParseException) obj[1]);
        }
        
        if(obj[1] != null && obj[1] instanceof TokenMgrError){
            fireParserError((TokenMgrError) obj[1]);
        }
    }
    

    
    //static methods that can be used to get the ast (and error if any) --------------------------------------

    /**
     * 
     * @param document the document that should be parsed.
     * 
     * @param reparseIfErrorFound boolean indicating that another reparse should
     * 		  be attempted, changing the current text line for a 'pass', so that we can 
     * 		  give outline and some feedback.
     * 		  (Maybe a good idea would be a fast parser that is error aware and that finds
     * 		  the information we need, as class and function definitions).
     * 
     * @return a tuple with the SimpleNode root(if parsed) and the error (if any).
     *         if we are able to recover from a reparse, we have both, the root and the error.
     */
    public static Object[] reparseDocument(IDocument document, boolean reparseIfErrorFound, PythonNature nature) {
        // create a stream with document's data
        StringReader inString = new StringReader(document.get());
        ReaderCharStream in = new ReaderCharStream(inString);

        IParserHost host = new CompilerAPI();

        PythonGrammar g1 = new PythonGrammar((CharStream) null, (IParserHost) null);

        PythonGrammar grammar = new PythonGrammar(in, host);

        try {
            SimpleNode newRoot = grammar.file_input(); // parses the file
            return new Object[]{newRoot,null};


        } catch (ParseException parseErr) {
            SimpleNode newRoot = null;
            
            if (reparseIfErrorFound){
                newRoot = tryReparseAgain(document, parseErr, nature);
            }
            
            return new Object[]{newRoot, parseErr};
            
        
        } catch (TokenMgrError tokenErr) {
            SimpleNode newRoot = null;
    
            //lets try to make it 2.4 compatible
            //TODO: (HACK) this is a hack, once the grammar is compatible to 2.4, remove it!
            float f = Float.parseFloat(nature.getVersion());
            if(nature != null && f >= 2.4){
	            if(tokenErr.curChar.equals("@") && tokenErr.errorCode == TokenMgrError.LEXICAL_ERROR){
	                int line = tokenErr.errorLine;
	                String docToParse = PyCodeCompletion.getDocToParseFromLine(document, line-1);
	                if(docToParse != null){
	
	                    Document doc = new Document(docToParse);
	                    return reparseDocument(doc, true, nature);
	                }
	            }
            }
            //END: HACK
            
            if (reparseIfErrorFound){
                newRoot = tryReparseAgain(document, tokenErr, nature);
            }
            
            return new Object[]{newRoot, tokenErr};

        } catch (Exception e) {
            PydevPlugin.log(e);
            return null;
        }
    }
    /**
     * @param tokenErr
     */
    private static SimpleNode tryReparseAgain(IDocument document, TokenMgrError tokenErr, PythonNature nature) {
        int line = tokenErr.errorLine;
        
        return tryReparseChangingLine(document, line, nature);
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
    private static SimpleNode tryReparseAgain(IDocument document, ParseException tokenErr, PythonNature nature) {
        int line = 0;
        if(tokenErr.currentToken.image.equals(".") || tokenErr.currentToken.image.equals("(")){
            line = tokenErr.currentToken.beginLine-1;
        }else{
            line = tokenErr.currentToken.beginLine;
        }
        
        return tryReparseChangingLine(document, line, nature);
    }

    /**
     * Try a reparse changing the offending line.
     * 
     * @param document: this is the document to be changed
     * @param line: offending line to be changed to try a reparse.
     * 
     */
    private static SimpleNode tryReparseChangingLine(IDocument document, int line, PythonNature nature) {
        String docToParse = PyCodeCompletion.getDocToParseFromLine(document, line);
        if(docToParse != null){

            Document doc = new Document(docToParse);
	        return (SimpleNode) reparseDocument(doc, false, nature)[0];
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
            }
        } catch (InterruptedException e) {
        } finally {
            if (thread == this)
                thread = null;
        }
    }
}