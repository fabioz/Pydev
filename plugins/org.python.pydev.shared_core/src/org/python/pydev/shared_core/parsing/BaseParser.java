/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>       - initial API and implementation
*     Alexander Kurtakov <akurtako@redhat.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_core.parsing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.Tuple;

/**
 * PyParser uses org.python.parser to parse the document (lexical analysis) It
 * is attached to PyEdit (a view), and it listens to document changes On every
 * document change, the syntax tree is regenerated The reparsing of the document
 * is done on a ParsingThread
 * 
 * Clients that need to know when new parse tree has been generated should
 * register as parseListeners.
 */

public abstract class BaseParser implements IParser {

    /**
     * just for tests, when we don't have any editor
     */
    public static boolean ACCEPT_NULL_INPUT_EDITOR = false;

    /**
     * this is the document we should parse 
     */
    protected volatile IDocument document;

    /**
     * ast for the last successful parsing
     */
    protected ISimpleNode root = null;

    /**
     * listens to changes in the document
     */
    protected IDocumentListener documentListener;

    /**
     * listeners that get notified of successful or unsuccessful parser achievements
     */
    protected ArrayList<IParserObserver> parserListeners;

    /**
     * this is the object that will keep parser schedules for us (and will call us for doing parsing when requested)
     */
    protected ParserScheduler scheduler;

    /**
     * indicates we should do analysis only on doc save
     */
    protected boolean useAnalysisOnlyOnDocSave;

    /**
     * Identifies whether this parser is disposed.
     */
    protected volatile boolean disposed = false;

    /**
     * Should only be called for testing. Does not register as a thread.
     */
    protected BaseParser(BaseParserManager parseManager) {

        parserListeners = new ArrayList<IParserObserver>();
        scheduler = new ParserScheduler(this, parseManager);

        documentListener = new IDocumentListener() {

            public void documentChanged(DocumentEvent event) {
                if (useAnalysisOnlyOnDocSave) {
                    //if we're doing analysis only on doc change, the parser will not give any changes
                    //to the scheduler, so, we won't have any parse events to respond to
                    return;

                }
                String text = event.getText();

                boolean parseNow = true;
                if (event == null || text == null) {
                    parseNow = false;
                }
                if (parseNow) {
                    if (text.indexOf("\n") == -1 && text.indexOf("\r") == -1) {
                        parseNow = false;

                    }
                }

                if (!parseNow) {
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
     * should be called when the editor is disposed
     */
    public void dispose() {
        this.disposed = true;
        this.scheduler.dispose();

        // remove the listeners
        if (document != null) {
            document.removeDocumentListener(documentListener);
        }
        synchronized (parserListeners) {
            parserListeners.clear();
        }
    }

    public ISimpleNode getRoot() {
        return root;
    }

    public void notifySaved() {
        //force parse on save
        forceReparse();
    }

    /**
     * @return false if we asked a reparse and it will not be scheduled because a reparse is already in action.
     */
    public boolean forceReparse(Object... argsToReparse) {
        if (disposed) {
            return true; //reparse didn't happen, but no matter what happens, it won't happen anyways
        }
        return scheduler.parseNow(true, argsToReparse);
    }

    /**
     * This is the input from the editor that we're using in the parse
     */
    protected/*IEditorInput*/Object input;

    public void setDocument(IDocument document, Object input) {
        setDocument(document, true, input);
    }

    public synchronized void setDocument(IDocument doc, boolean addToScheduler, Object input) {
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

        if (addToScheduler) {
            // Reparse document on the initial set (force it)
            scheduler.parseNow(true);
        }
    }

    // ---------------------------------------------------------------------------- listeners
    /** stock listener implementation */
    public void addParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized (parserListeners) {
            if (!parserListeners.contains(listener)) {
                parserListeners.add(listener);
            }
        }
    }

    /** stock listener implementation */
    public void removeParseListener(IParserObserver listener) {
        Assert.isNotNull(listener);
        synchronized (parserListeners) {
            parserListeners.remove(listener);
        }
    }

    // ---------------------------------------------------------------------------- notifications
    /**
     * stock listener implementation event is fired whenever we get a new root
     * @param original 
     */
    protected void fireParserChanged(ChangedParserInfoForObservers info) {
        this.root = info.root;
        List<IParserObserver> temp;
        synchronized (parserListeners) {
            temp = new ArrayList<IParserObserver>(parserListeners);
        }

        for (IParserObserver l : temp) {
            try {
                //work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
                if (l instanceof IParserObserver3) {
                    ((IParserObserver3) l).parserChanged(info);

                } else if (l instanceof IParserObserver2) {
                    ((IParserObserver2) l).parserChanged(info.root, info.file, info.doc, info.argsToReparse);

                } else {
                    l.parserChanged(info.root, info.file, info.doc, info.docModificationStamp);
                }
            } catch (Exception e) {
                Log.log(e);
            }
        }
    }

    /**
     * stock listener implementation event is fired when parse fails
     * @param original 
     */
    protected void fireParserError(ErrorParserInfoForObservers info) {
        List<IParserObserver> temp;
        synchronized (parserListeners) {
            temp = new ArrayList<IParserObserver>(parserListeners);
        }
        for (IParserObserver l : temp) {//work on a copy (because listeners may want to remove themselves and we cannot afford concurrent modifications here)
            if (l instanceof IParserObserver3) {
                ((IParserObserver3) l).parserError(info);

            } else if (l instanceof IParserObserver2) {
                ((IParserObserver2) l).parserError(info.error, info.file, info.doc, info.argsToReparse);

            } else {
                l.parserError(info.error, info.file, info.doc);
            }
        }
    }

    // ---------------------------------------------------------------------------- parsing

    public static class ParseOutput {

        public final long modificationStamp;
        public final Throwable error;
        public final ISimpleNode ast;

        public ParseOutput(Tuple<ISimpleNode, Throwable> astInfo, long modificationStamp) {
            this.ast = astInfo.o1;
            this.error = astInfo.o2;
            this.modificationStamp = modificationStamp;
        }

        public ParseOutput(ISimpleNode ast, Throwable error, long modificationStamp) {
            this.ast = ast;
            this.error = error;
            this.modificationStamp = modificationStamp;
        }

        public ParseOutput() {
            this.ast = null;
            this.error = null;
            this.modificationStamp = -1;
        }

    }

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
    public abstract ParseOutput reparseDocument(Object... argsToReparse);

    /**
     * This function will remove the markers related to errors.
     * @param resource the file that should have the markers removed
     * @throws CoreException
     */
    public static void deleteErrorMarkers(IResource resource) throws CoreException {
        IMarker[] markers = resource.findMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        if (markers.length > 0) {
            resource.deleteMarkers(IMarker.PROBLEM, false, IResource.DEPTH_ZERO);
        }
    }

    public void resetTimeoutPreferences(boolean useAnalysisOnlyOnDocSave) {
        this.useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave;
    }

    public List<IParserObserver> getObservers() {
        synchronized (parserListeners) {
            return new ArrayList<IParserObserver>(this.parserListeners);
        }
    }

}
