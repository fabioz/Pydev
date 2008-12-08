/*
 * Created on Sep 17, 2006
 * @author Fabio
 */
package org.python.pydev.core;

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
    public static final int LATEST_GRAMMAR_VERSION = GRAMMAR_PYTHON_VERSION_2_6;
    
    /**
     * Just in case you're wondering, Version 3.0 is not the latest... it's as if it's a new grammar entirely.
     */
    public static final int GRAMMAR_PYTHON_VERSION_3_0 = 99;
    
    /**
     * @return the version of the grammar as defined in IPythonNature.GRAMMAR_PYTHON_VERSION...
     */
    public int getGrammarVersion();

}
