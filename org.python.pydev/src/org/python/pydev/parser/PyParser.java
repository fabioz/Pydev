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
import org.eclipse.jface.text.Assert;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IFileEditorInput;
import org.python.parser.ParseException;
import org.python.parser.PythonGrammar;
import org.python.parser.ReaderCharStream;
import org.python.parser.SimpleNode;
import org.python.parser.TokenMgrError;
import org.python.pydev.editor.PyEdit;


/**
 * PyParser uses org.python.parser to parse the document
 * (lexical analysis)
 * It is attached to PyEdit (a view), and it listens to document changes
 * On every document change, the syntax tree is regenerated
 * The reparsing of the document is done on a ParsingThread
 * 
 * Clients that need to know when new parse tree has been generated
 * should register as parseListeners.
 */


public class PyParser {
	
	IDocument document;
	PyEdit editorView;
	
	IDocumentListener documentListener; // listens to changes in the document
	ArrayList parserListeners;	// listeners that get notified 
	
	SimpleNode root; // root of the last PythonGrammar analysis

	static final boolean parseOnThread = true; // can turn of thread parsing for debugging
	ParsingThread parsingThread;	// thread that reparses the document
	
	public PyParser(PyEdit editorView) {
		this.editorView = editorView;
		root = null;
		parserListeners = new ArrayList();
		parsingThread = new ParsingThread(this);
		parsingThread.setName("Parsing thread");
	}
	
	public void dispose() {
		// remove the listeners
		if (document != null)
			document.removeDocumentListener(documentListener);
		parsingThread.diePlease();
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
				if (parseOnThread == true) {
					if (event.getText().indexOf("\n") == -1) 
						// carriage return in changed text means parse now, anything else means parse later
						parsingThread.parseLater();
					else
						parsingThread.parseNow();
				}
				else
					reparseDocument();
			}
			public void documentAboutToBeChanged(DocumentEvent event) {}		
		};
		document.addDocumentListener(documentListener);
		// Reparse document on the initial set
		parsingThread.start();
		if (parseOnThread == true)
			parsingThread.parseNow();
		else
			reparseDocument();
	}
	
	
	public SimpleNode getRoot() {
		return root;
	}
	
	/** stock listener implementation */
	public void addParseListener(IParserListener listener) {
		Assert.isNotNull(listener);
		if (! parserListeners.contains(listener))
			parserListeners.add(listener);
	}
	
	/** stock listener implementation */	
	public void removeParseListener(IParserListener listener) {
		Assert.isNotNull(listener);
		parserListeners.remove(listener);
	}

	/** 
	 * stock listener implementation 
	 * event is fired whenever we get a new root
	 */
	protected void fireParserChanged() {		
		if (parserListeners.size() > 0) {
			ArrayList list= new ArrayList(parserListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IParserListener l= (IParserListener) e.next();
				l.parserChanged(root);
			}
		}
	}
	
	/** 
	 * stock listener implementation 
	 * event is fired when parse fails
	 */
	protected void fireParserError(Throwable error) {
		if (parserListeners.size() > 0) {
			ArrayList list= new ArrayList(parserListeners);
			Iterator e= list.iterator();
			while (e.hasNext()) {
				IParserListener l= (IParserListener) e.next();
				l.parserError(error);
			}
		}
	}

	/**
	 * Parses the document, generates error annotations
	 */
	void reparseDocument() {
		StringReader inString = new StringReader(document.get());
		ReaderCharStream in = new ReaderCharStream(inString);
		PythonGrammar grammar = new PythonGrammar(in, new CompilerAPI());
		
		IEditorInput input = editorView.getEditorInput();
		if (input == null)
			return;
		IFile original= (input instanceof IFileEditorInput) ? ((IFileEditorInput) input).getFile() : null;
		try {
			SimpleNode newRoot = grammar.file_input(); // parses the file
			root = newRoot;
			original.deleteMarkers(IMarker.PROBLEM, false, 1);
			fireParserChanged();
		} catch (ParseException parseErr) {
			fireParserError(parseErr);
		} 
		catch (TokenMgrError tokenErr) {
			fireParserError(tokenErr);
		}catch (Exception e) {
			System.err.println("Unexpected parse error");
			e.printStackTrace();
		}
	}
}

/**
 * Utility thread that reparses document as needed.
 * 
 * Current algorithm is:
 * - if parseLater is called, parse 10 main loops later
 * - if parseNow is called, parse immediately
 */
class ParsingThread extends Thread {
	PyParser parser;
	boolean stayingAlive = true;
	boolean parseNow = false;
	/* counter how to parse. 
	 * 0 means do not parse, > 0 means wait this many loops in main thread
	 */
	int parseLater = 0;

	final static int PARSE_LATER_INTERVAL = 20; // 20 = 2 seconds
		
	ParsingThread(PyParser parser) {
		this.parser = parser;
	}

	/**
	 * 
	 * @throws InterruptedException
	 */
	public synchronized void waitForChange() throws InterruptedException {
		if (!parseNow && parseLater == 0)
			wait();
	}
	
	public synchronized void parseNow() {
		parseNow = true;
		notify();
	}
	
	public synchronized void parseLater() {
		parseLater = PARSE_LATER_INTERVAL; // delay of 1 second
		notify();
	}
	
	public synchronized void doNotParse() {
		parseLater = 0;
		parseNow = false;
	}
	
	public synchronized void diePlease() {
		stayingAlive = false;
		notify();
	}

	public void run() {
		// wait for document change, and reparse
		try {
			while (stayingAlive) {
				parseLater--;
				if (parseLater == 1)
					parseNow();
				if (parseNow) {
					doNotParse();
					if (stayingAlive)
						parser.reparseDocument();
				}
				sleep(100);  // sleep a bit, to avoid flicker
				waitForChange();
			}
		} catch (InterruptedException e) {
			return;
		}		
	}
}
