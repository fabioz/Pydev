/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.parser.IPyParser;

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
public class PyParserManager {

    private static final boolean DEBUG = false;

    private Object lock = new Object();

    private static final String KEY_IN_PYEDIT_CACHE = "PyParserManager_PyParser";

    // -------------------------------------------------------------------------------------------- preferences stuff...
    public static final String USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE = "USE_PYDEV_ONLY_ON_DOC_SAVE";
    public static final String PYDEV_ELAPSE_BEFORE_ANALYSIS = "PYDEV_ELAPSE_BEFORE_ANALYSIS";

    private IPreferenceStore prefs;
    private int millisBeforeAnalysis;
    private boolean useOnlyOnSave;

    public int getElapseMillisBeforeAnalysis() {
        return millisBeforeAnalysis;
    }

    public boolean useAnalysisOnlyOnDocSave() {
        return useOnlyOnSave;
    }

    // ---------------------------------------------------------------------------------------------- singleton stuff...
    private static PyParserManager pyParserManager;

    public static synchronized PyParserManager getPyParserManager(IPreferenceStore prefs) {
        if (pyParserManager == null) {
            pyParserManager = new PyParserManager(prefs);
        }
        return pyParserManager;
    }

    public static synchronized void setPyParserManager(PyParserManager pyParserManager) {
        PyParserManager.pyParserManager = pyParserManager;
    }

    /**
     * Constructor
     * 
     * @param prefs the prefs to get to the parser
     */
    private PyParserManager(IPreferenceStore prefs) {
        Assert.isNotNull(prefs); //in this constructor the prefs may never be null!

        this.prefs = prefs;
        this.millisBeforeAnalysis = prefs.getInt(PYDEV_ELAPSE_BEFORE_ANALYSIS);
        this.useOnlyOnSave = prefs.getBoolean(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE);

        //singleton: private constructor
        IPropertyChangeListener prefListener = new IPropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent event) {
                String property = event.getProperty();
                if (property.equals(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE)
                        || property.equals(PYDEV_ELAPSE_BEFORE_ANALYSIS)) {
                    //reset the caches
                    millisBeforeAnalysis = PyParserManager.this.prefs.getInt(PYDEV_ELAPSE_BEFORE_ANALYSIS);
                    useOnlyOnSave = PyParserManager.this.prefs.getBoolean(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE);

                    //and set the needed parsers
                    boolean useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave();

                    synchronized (lock) {
                        for (IPyParser parser : parsers.keySet()) {
                            parser.resetTimeoutPreferences(useAnalysisOnlyOnDocSave);
                        }
                    }
                }
            }
        };
        this.prefs.addPropertyChangeListener(prefListener);
    }

    // ---------------------------------------------------------------------------------------------- parser control....
    private volatile Map<IPyParser, List<IPyEdit>> parsers = new HashMap<IPyParser, List<IPyEdit>>();

    public synchronized List<IPyParser> getParsers() {
        synchronized (lock) {
            ArrayList<IPyParser> ret = new ArrayList<IPyParser>(parsers.keySet());
            return ret;
        }
    }

    /**
     * This method attaches a parser to an editor.
     * 
     * It should:
     * 1. Set the parser attribute in the IPyEdit
     * 2. Add the IPyEdit as a listener to the new parser
     * 
     * @param edit this is the editor to which a parser should be attached.
     */
    public synchronized void attachParserTo(IPyEdit edit) {
        synchronized (lock) {
            //remove previous...
            IPyParser existingParser = getParser(edit);
            if (existingParser != null) {
                //it was already bounded to a parser, so, we have to remove that one before
                //attaching a new one
                notifyEditorDisposed(edit);
            }

            for (Map.Entry<IPyParser, List<IPyEdit>> entry : parsers.entrySet()) {
                for (IPyEdit curr : entry.getValue()) {
                    if (curr.hasSameInput(edit)) {
                        //do nothing, as it is already binded to a similar document (just force a reparse
                        //and add it to the list of edits for that parser)
                        IPyParser p = getParser(curr);

                        makeParserAssociations(edit, p);

                        p.forceReparse();
                        return;
                    }
                }
            }
            if (DEBUG) {
                System.out.println("Creating new parser.");
            }

            IPyParser pyParser = new PyParser(edit);

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

    /**
     * Makes the needed associations between the editor and a parser.
     * 
     * Meaning: 
     * the edit is put in the map (parser > edits)
     * the edit is added as a listener for parser events
     * the parser is set as the parser to be used in the editor
     */
    private synchronized void makeParserAssociations(IPyEdit edit, IPyParser pyParser) {
        synchronized (lock) {
            List<IPyEdit> lst = this.parsers.get(pyParser);
            if (lst == null) {
                lst = new ArrayList<IPyEdit>();
                this.parsers.put(pyParser, lst);
            }
            lst.add(edit);

            pyParser.addParseListener(edit);
            edit.getCache().put(KEY_IN_PYEDIT_CACHE, pyParser);
        }
    }

    public synchronized void notifySaved(IPyEdit edit) {
        synchronized (lock) {
            if (DEBUG) {
                System.out.println("Notifying save.");
            }
            getParser(edit).notifySaved();
        }
    }

    public synchronized void notifyEditorDisposed(IPyEdit edit) {
        synchronized (lock) {
            //remove the listener from the parser
            IPyParser parser = getParser(edit);

            //External editors may not have a parser...
            if (parser != null) {

                parser.removeParseListener(edit);

                //from the internal list from the parsers to the editors
                List<IPyEdit> lst = parsers.get(parser);
                //we always have the list here (because we must have created it before disposing it)
                lst.remove(edit);

                //and from the edit itself
                edit.getCache().remove(KEY_IN_PYEDIT_CACHE);

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
                    IPyEdit pyEdit = lst.get(0);
                    IDocument doc = pyEdit.getDocument();
                    parser.setDocument(doc, pyEdit.getEditorInput());
                }
            }

        }
    }

    public synchronized IPyParser getParser(IPyEdit edit) {
        synchronized (lock) {
            return (IPyParser) edit.getCache().get(KEY_IN_PYEDIT_CACHE);
        }
    }

}
