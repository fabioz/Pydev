/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.parser.preferences.PyDevBuilderPreferences;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.parsing.BaseParserManager;
import org.python.pydev.shared_core.parsing.IParser;

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
public class PyParserManager extends BaseParserManager {

    // -------------------------------------------------------------------------------------------- preferences stuff...
    public static final String USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE = "USE_PYDEV_ONLY_ON_DOC_SAVE";
    public static final String PYDEV_ELAPSE_BEFORE_ANALYSIS = "PYDEV_ELAPSE_BEFORE_ANALYSIS";

    private IEclipsePreferences prefs;

    // ---------------------------------------------------------------------------------------------- singleton stuff...
    private static PyParserManager pyParserManager;

    public static synchronized PyParserManager getPyParserManager(IEclipsePreferences prefs) {
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
    private PyParserManager(IEclipsePreferences prefs) {
        super();
        Assert.isNotNull(prefs); //in this constructor the prefs may never be null!

        //Override the default to deal with pydev preferences.
        this.prefs = prefs;
        this.millisBeforeAnalysis = prefs.getInt(PYDEV_ELAPSE_BEFORE_ANALYSIS,
                PyDevBuilderPreferences.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);

        this.useOnlyOnSave = prefs.getBoolean(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
                PyDevBuilderPreferences.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);

        //singleton: private constructor
        IEclipsePreferences.IPreferenceChangeListener prefListener = new IEclipsePreferences.IPreferenceChangeListener() {

            @Override
            public void preferenceChange(PreferenceChangeEvent event) {
                String property = event.getKey();
                if (property.equals(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE)
                        || property.equals(PYDEV_ELAPSE_BEFORE_ANALYSIS)) {
                    //reset the caches
                    millisBeforeAnalysis = PyParserManager.this.prefs.getInt(PYDEV_ELAPSE_BEFORE_ANALYSIS,
                            PyDevBuilderPreferences.DEFAULT_PYDEV_ELAPSE_BEFORE_ANALYSIS);

                    useOnlyOnSave = PyParserManager.this.prefs.getBoolean(USE_PYDEV_ANALYSIS_ONLY_ON_DOC_SAVE,
                            PyDevBuilderPreferences.DEFAULT_USE_PYDEV_ONLY_ON_DOC_SAVE);

                    //and set the needed parsers
                    boolean useAnalysisOnlyOnDocSave = useAnalysisOnlyOnDocSave();

                    synchronized (lock) {
                        for (IParser parser : parsers.keySet()) {
                            parser.resetTimeoutPreferences(useAnalysisOnlyOnDocSave);
                        }
                    }
                }
            }
        };
        this.prefs.addPreferenceChangeListener(prefListener);
    }

    @Override
    protected IParser createParser(IBaseEditor edit) {
        return new PyParser((IPyEdit) edit);
    }
}
