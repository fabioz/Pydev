/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Sep 17, 2006
 * @author Fabio
 */
package org.python.pydev.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface IGrammarVersionProvider {
    /**
     * Constants for knowing the version of a grammar (so that jython 2.1 and python 2.1 can be regarded
     * as GRAMMAR_PYTHON_VERSION_2_1), so, in short, it does not differentiate among the many flavors of python
     * 
     * They don't start at 0 because we don't want any accidents... ;-)
     */
    public static final int GRAMMAR_PYTHON_VERSION_2_4 = 10;
    public static final int GRAMMAR_PYTHON_VERSION_2_5 = 11;
    public static final int GRAMMAR_PYTHON_VERSION_2_6 = 12;
    public static final int GRAMMAR_PYTHON_VERSION_2_7 = 13;
    public static final int LATEST_GRAMMAR_VERSION = GRAMMAR_PYTHON_VERSION_2_7;

    /**
     * Just in case you're wondering, Version 3.0 is not the latest... it's as if it's a new grammar entirely.
     */
    public static final int GRAMMAR_PYTHON_VERSION_3_0 = 99;

    /**
     * So, no specific reason for the 777 number (just wanted something unique that wouldn't be close to the other grammars).
     */
    public static final int GRAMMAR_PYTHON_VERSION_CYTHON = 777;

    /**
     * @return the version of the grammar as defined in IPythonNature.GRAMMAR_PYTHON_VERSION...
     * @throws MisconfigurationException 
     */
    public int getGrammarVersion() throws MisconfigurationException;

    public static List<Integer> grammarVersions = GrammarsIterator.createList();

    public static Map<Integer, String> grammarVersionToRep = GrammarsIterator.createDict();

}

/**
 * Just create a new class to initialize those values (we cannot do it in the interface)
 */
class GrammarsIterator {

    public static List<Integer> createList() {
        List<Integer> grammarVersions = new ArrayList<Integer>();
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        return grammarVersions;
    }

    public static Map<Integer, String> createDict() {
        HashMap<Integer, String> ret = new HashMap<Integer, String>();
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4, "2.4");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5, "2.5");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6, "2.6");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7, "2.7");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0, "3.0");
        return ret;
    }
}
