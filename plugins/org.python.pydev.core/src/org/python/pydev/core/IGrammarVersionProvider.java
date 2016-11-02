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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    public static final int GRAMMAR_PYTHON_VERSION_3_6 = 100;
    public static final int LATEST_GRAMMAR_PY3_VERSION = GRAMMAR_PYTHON_VERSION_3_6;

    /**
     * So, no specific reason for the 777 number (just wanted something unique that wouldn't be close to the other grammars).
     */
    public static final int GRAMMAR_PYTHON_VERSION_CYTHON = 777;

    /**
     * @return the version of the grammar as defined in IPythonNature.GRAMMAR_PYTHON_VERSION...
     * @throws MisconfigurationException 
     */
    public int getGrammarVersion() throws MisconfigurationException;

    /**
     * @return may be null
     * @throws MisconfigurationException
     */
    public AdditionalGrammarVersionsToCheck getAdditionalGrammarVersions() throws MisconfigurationException;

    public static List<Integer> grammarVersions = GrammarsIterator.createList();

    public static Map<Integer, String> grammarVersionToRep = GrammarsIterator.createDict();

    public static Map<String, Integer> grammarRepToVersion = GrammarsIterator.createStrToInt();

    public static List<String> grammarVersionsRep = GrammarsIterator.createStr();

    public static class AdditionalGrammarVersionsToCheck {

        private final Set<Integer> grammarVersionsToCheck = new HashSet<>();

        public void add(int grammarVersion) {
            this.grammarVersionsToCheck.add(grammarVersion);
        }

        public Set<Integer> getGrammarVersions() {
            return grammarVersionsToCheck;
        }

    }

}

/**
 * Just create a new class to initialize those values (we cannot do it in the interface)
 */
class GrammarsIterator {

    public static List<Integer> createList() {
        List<Integer> grammarVersions = new ArrayList<>();
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6);
        return Collections.unmodifiableList(grammarVersions);
    }

    public static List<String> createStr() {
        List<String> grammarVersions = new ArrayList<>();
        grammarVersions.add("2.4");
        grammarVersions.add("2.5");
        grammarVersions.add("2.6");
        grammarVersions.add("2.7");
        grammarVersions.add("3.0");
        grammarVersions.add("3.6");
        return Collections.unmodifiableList(grammarVersions);
    }

    public static Map<Integer, String> createDict() {
        HashMap<Integer, String> ret = new HashMap<>();
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4, "2.4");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5, "2.5");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6, "2.6");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7, "2.7");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0, "3.0");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6, "3.6");
        return Collections.unmodifiableMap(ret);
    }

    public static Map<String, Integer> createStrToInt() {
        HashMap<String, Integer> ret = new HashMap<>();
        ret.put("2.4", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4);
        ret.put("2.5", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_5);
        ret.put("2.6", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_6);
        ret.put("2.7", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7);
        ret.put("3.0", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_0);
        ret.put("3.6", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6);
        return Collections.unmodifiableMap(ret);
    }
}
