/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 21, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor;

import junit.framework.TestCase;

import org.eclipse.jface.preference.PreferenceStore;
import org.eclipse.jface.resource.StringConverter;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.rules.IToken;
import org.eclipse.swt.graphics.RGB;
import org.python.pydev.editor.PyCodeScanner.NumberDetector;
import org.python.pydev.editor.preferences.PydevEditorPrefs;
import org.python.pydev.ui.ColorAndStyleCache;

/**
 * @author Fabio Zadrozny
 */
public class PyCodeScannerTest extends TestCase {

    private NumberDetector detector;
    private ColorAndStyleCache colorCache;

    public static void main(String[] args) {
        PyCodeScannerTest test = new PyCodeScannerTest();
        try {
            test.setUp();
            test.testScanner5();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PyCodeScannerTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * @see TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        detector = new PyCodeScanner.NumberDetector();
    }

    /*
     * @see TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testNumber() {
        assertTrue(detector.isWordStart('1'));
        assertTrue(detector.isWordPart('.'));
        assertTrue(detector.isWordPart('2'));
        assertTrue(detector.isWordPart('e'));
        assertTrue(detector.isWordPart('5'));
        assertFalse(detector.isWordPart(' '));
    }

    public void testHexa() {
        assertTrue(detector.isWordStart('0'));
        assertTrue(detector.isWordPart('x'));
        assertTrue(detector.isWordPart('F'));
        assertTrue(detector.isWordPart('F'));
        assertFalse(detector.isWordPart(' '));
    }

    private PyCodeScanner createCodeScanner() {
        PreferenceStore store = new PreferenceStore();
        store.putValue(PydevEditorPrefs.KEYWORD_COLOR, StringConverter.asString(new RGB(1, 0, 0)));
        store.putValue(PydevEditorPrefs.SELF_COLOR, StringConverter.asString(new RGB(2, 0, 0)));
        store.putValue(PydevEditorPrefs.CODE_COLOR, StringConverter.asString(new RGB(3, 0, 0)));
        store.putValue(PydevEditorPrefs.DECORATOR_COLOR, StringConverter.asString(new RGB(4, 0, 0)));
        store.putValue(PydevEditorPrefs.NUMBER_COLOR, StringConverter.asString(new RGB(5, 0, 0)));
        store.putValue(PydevEditorPrefs.FUNC_NAME_COLOR, StringConverter.asString(new RGB(6, 0, 0)));
        store.putValue(PydevEditorPrefs.CLASS_NAME_COLOR, StringConverter.asString(new RGB(7, 0, 0)));
        store.putValue(PydevEditorPrefs.OPERATORS_COLOR, StringConverter.asString(new RGB(8, 0, 0)));
        store.putValue(PydevEditorPrefs.PARENS_COLOR, StringConverter.asString(new RGB(9, 0, 0)));

        this.colorCache = new ColorAndStyleCache(store);
        PyCodeScanner scanner = new PyCodeScanner(colorCache);
        return scanner;
    }

    public void testScanner() throws Exception {
        String str = "class Example(object):             \n" +
                "                                   \n"
                +
                "    def Call(self, param1=None):   \n" +
                "        return param1 + 10 * 10    \n"
                +
                "                                   \n" +
                "    def Call2(self):               \n"
                +
                "        return self.Call(param1=10)" +
                "";

        PyCodeScanner scanner = createCodeScanner();
        scanner.setRange(new Document(str), 0, str.length());
        IToken nextToken = scanner.nextToken();
        while (!nextToken.isEOF()) {
            scanner.getTokenOffset();
            scanner.getTokenLength();
            nextToken = scanner.nextToken();
        }
    }

    public void testScanner2() throws Exception {
        PyCodeScanner scanner = createCodeScanner();
        String str = "def Method():pass";
        scanner.setRange(new Document(str), 0, str.length());
        assertToken(scanner, 0, 3, colorCache.getKeywordTextAttribute());//def
        assertToken(scanner, 3, 1, colorCache.getCodeTextAttribute()); //whitespace
        assertToken(scanner, 4, 6, colorCache.getFuncNameTextAttribute()); //Method 
        assertToken(scanner, 10, 1, colorCache.getParensTextAttribute()); //( 
        assertToken(scanner, 11, 1, colorCache.getParensTextAttribute()); //)
        assertToken(scanner, 12, 1, colorCache.getCodeTextAttribute()); //:
        assertToken(scanner, 13, 4, colorCache.getKeywordTextAttribute()); //pass
    }

    public void testScanner3() throws Exception {
        PyCodeScanner scanner = createCodeScanner();
        String str = "a=1+2";
        scanner.setRange(new Document(str), 0, str.length());
        assertToken(scanner, 0, 1, colorCache.getCodeTextAttribute());//a
        assertToken(scanner, 1, 1, colorCache.getOperatorsTextAttribute()); //=
        assertToken(scanner, 2, 1, colorCache.getNumberTextAttribute()); //1 
        assertToken(scanner, 3, 1, colorCache.getOperatorsTextAttribute()); //+
        assertToken(scanner, 4, 1, colorCache.getNumberTextAttribute()); //2
    }

    public void testScanner4() throws Exception {
        PyCodeScanner scanner = createCodeScanner();
        String str = "class Clas():pass";
        scanner.setRange(new Document(str), 0, str.length());
        assertToken(scanner, 0, 5, colorCache.getKeywordTextAttribute());//class
        assertToken(scanner, 5, 1, colorCache.getCodeTextAttribute()); //whitespace
        assertToken(scanner, 6, 4, colorCache.getClassNameTextAttribute()); //Class1 
        assertToken(scanner, 10, 1, colorCache.getParensTextAttribute()); //( 
        assertToken(scanner, 11, 1, colorCache.getParensTextAttribute()); //)
        assertToken(scanner, 12, 1, colorCache.getCodeTextAttribute()); //:
        assertToken(scanner, 13, 4, colorCache.getKeywordTextAttribute()); //pass
    }

    public void testScanner5() throws Exception {
        PyCodeScanner scanner = createCodeScanner();
        String str = "a=''; b";
        scanner.setRange(new Document(str), 0, str.length());
        assertToken(scanner, 0, 1, colorCache.getCodeTextAttribute());//a
        assertToken(scanner, 1, 1, colorCache.getOperatorsTextAttribute()); //=
        assertToken(scanner, 2, 1, colorCache.getCodeTextAttribute()); //'
        assertToken(scanner, 3, 1, colorCache.getCodeTextAttribute()); //'
        assertToken(scanner, 4, 1, colorCache.getCodeTextAttribute()); //;
        assertToken(scanner, 5, 1, colorCache.getCodeTextAttribute()); // 
        assertToken(scanner, 6, 1, colorCache.getCodeTextAttribute()); //b
        assertToken(scanner, 7, 0, colorCache.getCodeTextAttribute()); //EOF
    }

    private void assertToken(PyCodeScanner scanner, int offset, int len, TextAttribute data) {
        IToken token = scanner.nextToken();
        assertEquals(offset, scanner.getTokenOffset());
        assertEquals(len, scanner.getTokenLength());
        TextAttribute tokenData = (TextAttribute) token.getData();
        if (len > 0) {
            assertEquals(data.getForeground(), tokenData.getForeground());
        }
    }
}
