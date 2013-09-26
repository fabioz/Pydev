/******************************************************************************
* Copyright (C) 2007-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.parsing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.editor.IBaseEditor;

/**
 * This is the class that manager the PyParser and its interaction with the PyEdit
 * 
 * It's available from 1.3.2 onwards because a single parser may be bounded to multiple editors, so, when the input
 * of a given editor changes, its parser may become the same parser that another editor already contained.
 * 
 * This class needs to know about:
 * 1. When an editor has its input set
 * 2. When an editor is disposed (so, its input no longer exists)
 * 
 * The idea is that a PyParser only exists if it has some input binded, and if there's no input bounded, it is disposed.
 * 
 * It's a singleton because it needs to manage multiple editors and their inputs. It is responsible for setting the
 * parser in each PyEdit.
 * 
 * @author Fabio
 */
public abstract class BaseParserManager {

    private static final boolean DEBUG = false;

    protected final Object lock = new Object();

    private static final String KEY_IN_CACHE = "ParserManager_Parser";

    // -------------------------------------------------------------------------------------------- preferences stuff...
    public static final String USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE = "USE_PYDEV_ONLY_ON_DOC_SAVE";
    public static final String PYDEV_ELAPSE_BEFORE_ANALYSIS = "PYDEV_ELAPSE_BEFORE_ANALYSIS";

    protected int millisBeforeAnalysis;
    protected boolean useOnlyOnSave;

    public int getElapseMillisBeforeAnalysis() {
        return millisBeforeAnalysis;
    }

    public boolean useAnalysisOnlyOnDocSave() {
        return useOnlyOnSave;
    }

    protected BaseParserManager() {
        this.millisBeforeAnalysis = 3000;
        this.useOnlyOnSave = false;
    }

    // ---------------------------------------------------------------------------------------------- parser control....
    protected volatile Map<IParser, List<IBaseEditor>> parsers = new HashMap<IParser, List<IBaseEditor>>();

    public synchronized List<IParser> getParsers() {
        synchronized (lock) {
            ArrayList<IParser> ret = new ArrayList<IParser>(parsers.keySet());
            return ret;
        }
    }

    /**
     * This method attaches a parser to an editor.
     * 
     * It should:
     * 1. Set the parser attribute in the IBaseEditor
     * 2. Add the IBaseEditor as a listener to the new parser
     * 
     * @param edit this is the editor to which a parser should be attached.
     */
    public synchronized void attachParserTo(IBaseEditor edit) {
        synchronized (lock) {
            //remove previous...
            IParser existingParser = getParser(edit);
            if (existingParser != null) {
                //it was already bounded to a parser, so, we have to remove that one before
                //attaching a new one
                notifyEditorDisposed(edit);
            }

            for (Map.Entry<IParser, List<IBaseEditor>> entry : parsers.entrySet()) {
                for (IBaseEditor curr : entry.getValue()) {
                    if (curr.hasSameInput(edit)) {
                        //do nothing, as it is already binded to a similar document (just force a reparse
                        //and add it to the list of edits for that parser)
                        IParser p = getParser(curr);

                        makeParserAssociations(edit, p);

                        p.forceReparse();
                        return;
                    }
                }
            }
            if (DEBUG) {
                System.out.println("Creating new parser.");
            }

            IParser pyParser = createParser(edit);

            boolean useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave();
            pyParser.resetTimeoutPreferences(useAnalysisOnlyOnDocSave);

            makeParserAssociations(edit, pyParser);
            IDocument doc = edit.getDocument();
            pyParser.setDocument(doc, edit.getEditorInput());

            if (DEBUG) {
                System.out.println("Available parsers:" + this.parsers.size());
            }
        }
    }

    protected abstract IParser createParser(IBaseEditor edit);

    /**
     * Makes the needed associations between the editor and a parser.
     * 
     * Meaning: 
     * the edit is put in the map (parser > edits)
     * the edit is added as a listener for parser events
     * the parser is set as the parser to be used in the editor
     */
    private synchronized void makeParserAssociations(IBaseEditor edit, IParser pyParser) {
        synchronized (lock) {
            List<IBaseEditor> lst = this.parsers.get(pyParser);
            if (lst == null) {
                lst = new ArrayList<IBaseEditor>();
                this.parsers.put(pyParser, lst);
            }
            lst.add(edit);

            pyParser.addParseListener(edit);
            edit.getCache().put(KEY_IN_CACHE, pyParser);
        }
    }

    public synchronized void notifySaved(IBaseEditor edit) {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Notifying save.");
            }
            getParser(edit).notifySaved();
        }
    }

    public synchronized void notifyEditorDisposed(IBaseEditor edit) {
        synchronized (lock) {
            //remove the listener from the parser
            IParser parser = getParser(edit);

            //External editors may not have a parser...
            if (parser != null) {

                parser.removeParseListener(edit);

                //from the internal list from the parsers to the editors
                List<IBaseEditor> lst = parsers.get(parser);
                //we always have the list here (because we must have created it before disposing it)
                lst.remove(edit);

                //and from the edit itself
                edit.getCache().remove(KEY_IN_CACHE);

                //now, if there's no one in that parsers list anymore, lets dispose the parser
                //and remove it from our references
                boolean dispose = lst.size() == 0;

                if (dispose) {
                    if (DEBUG) {
                        System.out.println("Disposing parser.");
                    }
                    parser.dispose();
                    this.parsers.remove(parser);
                    if (DEBUG) {
                        System.out.println("Available parsers:" + this.parsers.size());
                    }
                } else {
                    //otherwise, just set its new input
                    IBaseEditor pyEdit = lst.get(0);
                    IDocument doc = pyEdit.getDocument();
                    parser.setDocument(doc, pyEdit.getEditorInput());
                }
            }

        }
    }

    public synchronized IParser getParser(IBaseEditor edit) {
        synchronized (lock) {
            return (IParser) edit.getCache().get(KEY_IN_CACHE);
        }
    }

}
