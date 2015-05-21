/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codefolding;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.text.Document;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.parser.PyParser;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

import junit.framework.TestCase;

public class CodeFoldingSetterTest extends TestCase {

    public static boolean DEBUG = true;

    public static String largeDoc = "import signal\n" + //0                                         
            "from __future__ import with_statement\n" + //1
            "\n" + //2   
            "def foo():\n" + //3
            "    try:\n" + //4       
            "        print 'trythis'\n" + //5                   
            "        print 'tryagain'\n" + //6                       
            "        while True:\n" + //7                       
            "            print 'embedded while'\n" + //8               
            "    except(Exception):\n" + //9                
            "        print 'exception'\n" + //10                     
            "        print 'exception again'\n" + //11                                                             
            "    except(TypeError):\n" + //12                                                    
            "        print 'type error'\n" + //13                                                        
            "        print 'type error again'\n" + //14                                                              
            "        while True:\n" + //15                                                 
            "            print 'loop'\n" + //16                                                      
            "            print 'loop again'\n" + //17                                                            
            "    else:\n" + //18                                       
            "        print 'else'\n" + //19                                                  
            "        while True:\n" + //20                                                 
            "            print 'embedded while'\n" + //21                                                                
            "    finally:\n" + //22                                          
            "        print 'finally'\n" + //23                                                     
            "        print 'finally2'\n" + //24                                                      
            "        while True:\n" + //25                                                 
            "            print 'embedded while'\n" + //26                                                                
            "\n" + //27                                          
            "    try:\n" + //28                                      
            "        print 'small try'\n" + //29                                                       
            "    finally:\n" + //30                                          
            "        print 'small finally'\n" + //31
            "def PrintThis():\n" + //32                                              
            "    with signal.blocked():\n" + //33                                                        
            "        print 'a'\n" + //34                                               
            "        print 'few'\n" + //35                                                 
            "        while True:\n" + //36                                                 
            "            print 'some more'\n" + //37                                                           
            "            print 'again'\n" + //38                                                       
            "    return 'some string'\n" + //39                                                      
            "\n" + //40                              
            "def PrintThat():\n" + //41                                              
            "    x = 4\n" + //42                                       
            "    while True:\n" + //43                                             
            "        print 'something'\n" + //44                                                       
            "        while False:\n" + //45                                                  
            "            print 'comp'\n" + //46                                                      
            "            for x in range(5):\n" + //47                                                            
            "                print x\n" + //48                                                     
            "                print x\n" + //49                                                     
            "            else:\n" + //50                                               
            "                print 'nge'\n" + //51                                                         
            "            print 'other'\n" + //52                                                       
            "\n" + //53                                  
            "    return 'some string'\n" + //54                                                      
            "\n" + //55                              
            "\n" + //56                              
            "if __name__ == '__main__':\n" + //57                                                        
            "    print 'this is a simple file'\n" + //58                                                               
            "    while True:\n" + //59                                             
            "        print 'blah'\n" + //60                                                  
            "    else:\n" + //61                                       
            "        print 'nothing'\n" + //62                                                     
            "elif True:\n" + //63                                        
            "    print 'rando'\n" + //64                                               
            "    print 'other'\n" + //65                                               
            "else:\n" + //66                                   
            "    print 'something'\n" + //67                                                   
            "    if True:\n" + //68                                          
            "        print 'me'\n" + //69                                                
            "    elif False:\n" + //70                                             
            "        for x in range(5):\n" + //71                                                        
            "            print x\n" + //72                                                 
            "            print x\n" + //73                                                 
            "        else:\n" + //74                                           
            "            print 'nge'\n" + //75                                                     
            "        print 'metoo'\n" + //76                                                   
            "    else:\n" + //77                                       
            "        print 'nothing'\n" + //78                                                     
            "        print 'other'" //79                   
            ;

    public static void main(String[] args) {
        try {
            CodeFoldingSetterTest test = new CodeFoldingSetterTest();
            test.setUp();
            test.testTryFinally();
            test.tearDown();
            junit.textui.TestRunner.run(CodeFoldingSetterTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private IPreferenceStore preferences;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        DEBUG = false;
        preferences = new PreferenceStore();//PydevPlugin.getDefault().getPluginPreferences();
        CodeFoldingSetter.setPreferences(preferences);
        setAllOptions(false);
    }

    /**
     * Sets all the code folding options to false
     */
    private void setAllOptions(boolean value) {
        preferences.setValue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_COMMENTS, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_COMMENTS, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_FOR, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_IF, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_IMPORTS, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_STRINGS, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_TRY, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_WHILE, value);
        preferences.setValue(PyDevCodeFoldingPrefPage.FOLD_WITH, value);
    }

    private void setOptionTrue(String option) {
        preferences.setValue(option, true);
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
        CodeFoldingSetter.setPreferences(null);
    }

    public void testMarksWhileEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_WHILE);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 7, 9, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 15, 18, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 20, 22, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 25, 27, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 36, 39, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 43, 53, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 45, 53, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 59, 61, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 61, 63, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarks2() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF);
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document("" +
                "class TestCase(unittest.TestCase):\n" +
                "    \n" +
                "    def setUp(self):\n"
                +
                "        unittest.TestCase.setUp(self)\n" +
                "        return 1\n" +
                "\n" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 0, 5, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 2, 5, null), it.next());
        assertTrue(it.hasNext() == false);

    }

    public void testExceptMarks() throws Exception {
        setAllOptions(true);
        Document doc = new Document("" +
                "def foo():\n" +
                "    try:\n" +
                "        print 'trythis'\n"
                +
                "    except(Exception):\n" +
                "        print 'exception'\n" +
                "    finally:\n"
                +
                "        print 'finally'\n" +
                "            \n" +
                "    try:\n" +
                "        print 'small try'\n"
                +
                "    finally:\n" +
                "        print 'small finally'\n" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 0, 12, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 1, 3, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 3, 5, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 5, 7, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 8, 10, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 10, 12, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testExceptElse() throws Exception {
        setAllOptions(true);
        Document doc = new Document("" +
                "try:\n" +
                "    print 'trythis'\n" +
                "except(Exception):\n"
                +
                "    print 'exception'\n" +
                "except(Exception):\n" +
                "    print 'exception'\n" +
                "else:\n"
                +
                "    print 'finally'\n" +
                "finally:\n" +
                "    pass" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 0, 2, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 2, 4, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 4, 6, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 6, 8, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 8, 10, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testEndingWithMultiline() throws Exception {
        setAllOptions(true);
        Document doc = new Document("" +
                "while True:\n" +
                "    print '''htnh\n" +
                "    tnhueosn\n" +
                "    '''  \n"
                +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 0, 4, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STR, 1, 4, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testTryFinally() throws Exception {
        setAllOptions(true);
        Document doc = new Document("" +
                "try:\n" +
                "    pass\n" +
                "finally:\n" +
                "    pass" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 0, 2, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 2, 4, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testTryFinally2() throws Exception {
        setAllOptions(true);
        Document doc = new Document("" +
                "try:\n" +
                "    try:\n" +
                "        print 'trythis'\n"
                +
                "    except(Exception):\n" +
                "        pass\n" +
                "finally:\n" +
                "    print 'small finally'\n" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 0, 5, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 1, 3, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 3, 5, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 5, 7, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testIfMarks() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_IF);
        Document doc = new Document("" +
                "if a:\n" +
                "    print 1\n" +
                "    if b:\n" +
                "        print 2\n"
                +
                "    else:\n" +
                "        print 3\n" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 0, 6, null), it.next()); //end line not ok...
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 2, 4, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 4, 6, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testIfElifElseMarks() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_IF);
        Document doc = new Document("" +
                "if a:\n" +
                "    print 1\n" +
                "elif b:\n" +
                "    print 2\n" +
                "else:\n"
                +
                "    print 3\n" +
                "\n");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 0, 2, null), it.next()); //end line not ok...
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 2, 4, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 4, 6, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    private List<FoldingEntry> getMarks(Document doc) {
        return getMarks(doc, IPythonNature.LATEST_GRAMMAR_VERSION);
    }

    private List<FoldingEntry> getMarks(Document doc, int grammarVersion) {
        ParseOutput r = PyParser.reparseDocument(new PyParser.ParserInfo(doc, grammarVersion));
        List<FoldingEntry> marks = CodeFoldingSetter.getMarks(doc, (SimpleNode) r.ast, true);
        if (DEBUG) {
            for (FoldingEntry entry : marks) {
                System.out.println(entry);
            }
        }
        return marks;
    }

    public void testMarks() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_STRINGS);
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF);
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_COMMENTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_COMMENTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_IMPORTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document("" +
                "import foo\n" +
                "from foo import (x,\n" +
                "                 y,\n"
                +
                "                 )\n" +
                "import foo\n" +
                "#comment1\n" +
                "#comment2\n" +
                "#comment3\n"
                +
                "def m1():\n" +
                "    '''start\n" +
                "    end'''" +
                "");

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_IMPORT, 0, 5, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_COMMENT, 5, 8, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 8, 11, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STR, 9, 11, null), it.next());
        assertTrue(it.hasNext() == false);
    }

    public void testMarksClassDefEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_CLASSDEF);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertTrue(it.hasNext() == false);
    }

    public void testMarksFuncDefEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_FUNCTIONDEF);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 3, 32, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 32, 40, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 41, 55, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksForEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_FOR);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 47, 50, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 50, 52, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 71, 74, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 74, 76, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksCommentsEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_COMMENTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.INITIALLY_FOLD_COMMENTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertTrue(it.hasNext() == false);
    }

    public void testMarksIfEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_IF);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 57, 63, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 63, 66, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 66, 80, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 68, 70, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 70, 77, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 77, 80, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksImportsEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_IMPORTS);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_IMPORT, 0, 2, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksStringsEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_STRINGS);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertTrue(it.hasNext() == false);
    }

    public void testMarksTryEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_TRY);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 4, 9, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 9, 12, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 12, 18, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 18, 22, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 22, 27, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 28, 30, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 30, 32, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksWithEnabled() throws Exception {
        setOptionTrue(PyDevCodeFoldingPrefPage.FOLD_WITH);
        setOptionTrue(PyDevCodeFoldingPrefPage.USE_CODE_FOLDING);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 33, 39, null), it.next());

        assertTrue(it.hasNext() == false);
    }

    public void testMarksAllEnabled() throws Exception {
        setAllOptions(true);
        Document doc = new Document(largeDoc);

        List<FoldingEntry> marks = getMarks(doc);

        Iterator<FoldingEntry> it = marks.iterator();
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_IMPORT, 0, 2, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 3, 32, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 4, 9, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 7, 9, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 9, 12, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_EXCEPT, 12, 18, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 15, 18, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 18, 22, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 20, 22, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 22, 27, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 25, 27, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 28, 30, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_FINALLY, 30, 32, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 32, 40, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 33, 39, null), it.next());

        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 36, 39, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_DEF, 41, 55, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 43, 53, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 45, 53, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 47, 50, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 50, 52, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 57, 63, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 59, 61, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 61, 63, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 63, 66, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 66, 80, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 68, 70, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 70, 77, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 71, 74, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_ELSE, 74, 76, null), it.next());
        assertEquals(new FoldingEntry(FoldingEntry.TYPE_STATEMENT, 77, 80, null), it.next());

        assertTrue(it.hasNext() == false);
    }
}
