/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
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

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyCodeScannerTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        detector = new PyCodeScanner.NumberDetector();
    }

    /*
     * @see TestCase#tearDown()
     */
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
    
    
    public void testScanner() throws Exception{
        String str= 
            "class Example(object):             \n"+
            "                                   \n"+
            "    def Call(self, param1=None):   \n"+
            "        return param1 + 10 * 10    \n"+
            "                                   \n"+
            "    def Call2(self):               \n"+
            "        return self.Call(param1=10)"+
            "";
        
        PreferenceStore store = new PreferenceStore();
        store.putValue(PydevEditorPrefs.KEYWORD_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.SELF_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.CODE_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.DECORATOR_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.NUMBER_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.FUNC_NAME_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.CLASS_NAME_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.OPERATORS_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        store.putValue(PydevEditorPrefs.PARENS_COLOR, StringConverter.asString(new RGB(255, 0, 0)));
        
        ColorAndStyleCache colorCache = new ColorAndStyleCache(store);
        PyCodeScanner scanner = new PyCodeScanner(colorCache);
        scanner.setRange(new Document(str), 0, str.length());
        IToken nextToken = scanner.nextToken();
        while(!nextToken.isEOF()){
            scanner.getTokenOffset();
            scanner.getTokenLength();
            nextToken = scanner.nextToken();
        }
    }
}
