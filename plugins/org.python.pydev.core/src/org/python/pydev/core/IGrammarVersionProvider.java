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
    public static final int GRAMMAR_PYTHON_VERSION_3_5 = 99;
    public static final int GRAMMAR_PYTHON_VERSION_3_6 = 100;
    public static final int GRAMMAR_PYTHON_VERSION_3_7 = 101;
    public static final int GRAMMAR_PYTHON_VERSION_3_8 = 102;
    public static final int GRAMMAR_PYTHON_VERSION_3_9 = 103;
    public static final int GRAMMAR_PYTHON_VERSION_3_10 = 104;
    public static final int GRAMMAR_PYTHON_VERSION_3_11 = 105;
    public static final int LATEST_GRAMMAR_PY3_VERSION = GRAMMAR_PYTHON_VERSION_3_11;
    public static final int LATEST_GRAMMAR_PY2_VERSION = GRAMMAR_PYTHON_VERSION_3_11;

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

    public static Map<Integer, String> grammarVersionToRep = GrammarsIterator.createDict();

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
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_7);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_8);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_9);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_10);
        grammarVersions.add(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_11);
        return Collections.unmodifiableList(grammarVersions);
    }

    public static List<String> createStr() {
        List<String> grammarVersions = new ArrayList<>();
        grammarVersions.add("3.5");
        grammarVersions.add("3.6");
        grammarVersions.add("3.7");
        grammarVersions.add("3.8");
        grammarVersions.add("3.9");
        grammarVersions.add("3.10");
        grammarVersions.add("3.11");
        return Collections.unmodifiableList(grammarVersions);
    }

    public static Map<Integer, String> createDict() {
        HashMap<Integer, String> ret = new HashMap<>();
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5, "3.5");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6, "3.6");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_7, "3.7");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_8, "3.8");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_9, "3.9");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_10, "3.10");
        ret.put(IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_11, "3.11");
        return Collections.unmodifiableMap(ret);
    }

    public static Map<String, Integer> createStrToInt() {
        HashMap<String, Integer> ret = new HashMap<>();
        ret.put("3.5", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_5);
        ret.put("3.6", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_6);
        ret.put("3.7", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_7);
        ret.put("3.8", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_8);
        ret.put("3.9", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_9);
        ret.put("3.10", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_10);
        ret.put("3.11", IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_3_11);
        return Collections.unmodifiableMap(ret);
    }
}
