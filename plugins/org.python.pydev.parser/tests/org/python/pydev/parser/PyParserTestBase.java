/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.TokenMgrError;
import org.python.pydev.shared_core.callbacks.ICallback;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;
import org.python.pydev.shared_core.structure.Tuple;

public class PyParserTestBase extends TestCase {
    protected static PyParser parser;
    private static int defaultVersion;
    protected static IGrammarVersionProvider versionProvider = new IGrammarVersionProvider() {

        public int getGrammarVersion() {
            return defaultVersion;
        }
    };

    /**
     * @param defaultVersion the defaultVersion to set
     */
    protected static void setDefaultVersion(int defaultVersion) {
        PyParserTestBase.defaultVersion = defaultVersion;
    }

    /**
     * @return the defaultVersion
     */
    protected static int getDefaultVersion() {
        return defaultVersion;
    }

    @Override
    protected void setUp() throws Exception {
        PyParser.ACCEPT_NULL_INPUT_EDITOR = true;
        PyParser.ENABLE_TRACING = true;
        ParseException.verboseExceptions = true;
        parser = new PyParser(versionProvider);
        setDefaultVersion(IPythonNature.LATEST_GRAMMAR_VERSION);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        PyParser.ACCEPT_NULL_INPUT_EDITOR = false;
        PyParser.ENABLE_TRACING = false;
        super.tearDown();
    }

    /**
     * @param s
     * @return 
     */
    protected static SimpleNode parseLegalDocStr(String s, Object... additionalErrInfo) {
        Document doc = new Document(s);
        //by default always use the last version for parsing
        return parseLegalDoc(doc, additionalErrInfo, parser);
    }

    protected SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo) {
        return parseLegalDoc(doc, additionalErrInfo, parser);
    }

    protected Throwable parseILegalDocStr(String s) {
        return parseILegalDoc(new Document(s), true);
    }

    protected Throwable parseILegalDocStrWithoutTree(String s) {
        return parseILegalDoc(new Document(s), false);
    }

    protected Throwable parseILegalDoc(IDocument doc) {
        return parseILegalDoc(doc, true);
    }

    protected Throwable parseILegalDoc(IDocument doc, boolean generateTree) {
        ParseOutput objects;
        try {
            objects = PyParser.reparseDocument(new ParserInfo(doc, parser.getGrammarVersion(), generateTree));
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }

        Throwable err = objects.error;
        if (err == null) {
            fail("Expected a ParseException and the doc was successfully parsed.");
        }
        if (!(err instanceof ParseException) && !(err instanceof TokenMgrError)) {
            fail("Expected a ParseException and received:" + err.getClass());
        }
        return err;
    }

    protected Tuple<SimpleNode, Throwable> parseILegalDocSuccessfully(String doc) {
        ParseOutput ret = parseILegalDocSuccessfully(new Document(doc));

        return new Tuple<SimpleNode, Throwable>((SimpleNode) ret.ast, ret.error);
    }

    protected ParseOutput parseILegalDocSuccessfully(IDocument doc) {
        parser.setDocument(doc, false, null);
        ParseOutput objects = parser.reparseDocument();
        Throwable err = objects.error;
        if (err == null) {
            fail("Expected a ParseException and the doc was successfully parsed.");
        }
        if (!(err instanceof ParseException) && !(err instanceof TokenMgrError)) {
            fail("Expected a ParseException and received:" + err.getClass());
        }
        if (objects.ast == null) {
            fail("Expected the ast to be generated with the parse. Error: " + objects.error.getMessage());
        }
        return objects;
    }

    protected void parseLegalDocStrWithoutTree(String s, Object... additionalErrInfo) {
        try {
            parseLegalDoc(new Document(s), additionalErrInfo, parser.getGrammarVersion(), false);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, PyParser parser) {
        try {
            return parseLegalDoc(doc, additionalErrInfo, parser.getGrammarVersion(), true);
        } catch (MisconfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param additionalErrInfo can be used to add additional errors to the fail message if the doc is not parseable
     * @param parser the parser to be used to do the parsing.
     */
    protected static SimpleNode parseLegalDoc(IDocument doc, Object[] additionalErrInfo, int grammarVersion,
            boolean generateTree) {
        ParseOutput objects = PyParser.reparseDocument(new ParserInfo(doc, grammarVersion,
                generateTree));

        Object err = objects.error;
        if (err != null) {
            String s = "";
            for (int i = 0; i < additionalErrInfo.length; i++) {
                s += additionalErrInfo[i];
            }
            if (err instanceof ParseException) {
                ParseException parseErr = (ParseException) err;
                parseErr.printStackTrace();

                Token token = parseErr.currentToken;
                if (token != null) {
                    fail("Expected no error, received: " + parseErr.getMessage() + "\n" + s + "\nline:"
                            + token.beginLine + "\ncol:" + token.beginColumn);
                }
            }

            fail("Expected no error, received:\n" + err + "\n" + s);
        }
        if (generateTree) {
            if (objects.ast == null) {
                String s = "";
                for (int i = 0; i < additionalErrInfo.length; i++) {
                    s += additionalErrInfo[i];
                }
                fail("AST not generated! " + s);
            }
        }
        return (SimpleNode) objects.ast;
    }

    public void testEmpty() throws Throwable {
    }

    protected void parseFilesInDir(File dir, boolean recursive) {
        parseFilesInDir(dir, recursive, true);
    }

    /**
     * @param dir the directory that should have .py files found and parsed. 
     */
    protected void parseFilesInDir(File dir, boolean recursive, boolean generateTree) {
        assertTrue("Directory " + dir + " does not exist", dir.exists());
        assertTrue(dir.isDirectory());

        File[] files = dir.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            String name = f.getName().toLowerCase();
            if (name.endsWith(".py")) {
                //Used for stress-testing: parsing all files in Python installation.
                //            	try {
                //					if(name.equals("func_syntax_error.py")){
                //						continue;
                //					}
                //					if(name.equals("badsyntax_nocaret.py")){
                //						continue;
                //					}
                //					if(name.equals("py3_test_grammar.py") && parser.getGrammarVersion() < IPythonNature.GRAMMAR_PYTHON_VERSION_3_0){
                //						continue;
                //					}
                //					String absolute = f.getAbsolutePath().toLowerCase();
                //					if(absolute.contains("pylint") && absolute.contains("test")){
                //						continue;
                //					}
                //					if(absolute.contains("port_v3")){
                //						continue;
                //					}
                //				} catch (MisconfigurationException e) {
                //					throw new RuntimeException(e);
                //				}
                if (generateTree) {
                    parseLegalDocStr(FileUtils.getFileContents(f), f);
                } else {
                    parseLegalDocStrWithoutTree(FileUtils.getFileContents(f), f);
                }

            } else if (recursive && f.isDirectory()) {
                parseFilesInDir(f, recursive, generateTree);
            }
        }
    }

    /**
     * The parameter passed in the callback is an integer with the version of the grammar.
     * @param iCallback
     * @throws Throwable 
     */
    public void checkWithAllGrammars(ICallback<Boolean, Integer> iCallback) throws Throwable {
        for (Iterator<Integer> it = IGrammarVersionProvider.grammarVersions.iterator(); it.hasNext();) {
            //try with all the grammars
            final Integer i = it.next();
            boolean prev = PyParser.DEBUG_SHOW_PARSE_ERRORS;
            // Uncomment the following line to get debug info
            // We leave it off by default because it generates significant (MBs+) of
            // output which causes Travis CI to reject the build for having too many
            // logs
            //            PyParser.DEBUG_SHOW_PARSE_ERRORS = true;

            // Uncomment the following lines to test only the specific grammar
            //            if(i != IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_4){
            //                continue;
            //            }
            setDefaultVersion(i);
            try {
                iCallback.call(i);
            } catch (Throwable e) {
                System.out.println("\nFound error while parsing with version: "
                        + IGrammarVersionProvider.grammarVersionToRep.get(i));
                throw e;
            } finally {
                PyParser.DEBUG_SHOW_PARSE_ERRORS = prev;
            }
        }
    }

}
