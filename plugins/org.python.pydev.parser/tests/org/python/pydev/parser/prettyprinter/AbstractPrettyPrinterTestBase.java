/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinter;

import java.io.File;
import java.io.IOException;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.prettyprinterv2.IPrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinterv2.MakeAstValidForPrettyPrintingVisitor;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.comparator.DifferException;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator;
import org.python.pydev.shared_core.io.FileUtils;

public class AbstractPrettyPrinterTestBase extends PyParserTestBase {

    public static boolean DEBUG = false;

    protected IPrettyPrinterPrefs prefs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefsV2("\n", "    ", versionProvider);
    }

    public SimpleNode checkPrettyPrintEqual(String s, String expected) throws Error {
        return checkPrettyPrintEqual(s, prefs, expected, expected, expected);
    }

    public SimpleNode checkPrettyPrintEqual(String s, String expected, String v2) throws Error {
        return checkPrettyPrintEqual(s, prefs, expected, v2, v2);
    }

    public SimpleNode checkPrettyPrintEqual(String s, String expected, String v2, String v3) throws Error {
        return checkPrettyPrintEqual(s, prefs, expected, v2, v3);
    }

    public SimpleNode checkPrettyPrintEqual(String s) throws Error {
        return checkPrettyPrintEqual(s, s);
    }

    /**
     * @param s
     * @return 
     * @throws Exception
     * @throws IOException
     */
    public static SimpleNode checkPrettyPrintEqual(String s, IPrettyPrinterPrefs prefs, String withLinesAndCols,
            String withoutSpecials, String scrambledLines) throws Error {
        SimpleNode node = parseLegalDocStr(s);

        //Scramble the lines/columns
        SimpleNode copy = node.createCopy();
        MessLinesAndColumnsVisitor messLinesAndColumnsVisitor = new MessLinesAndColumnsVisitor();
        try {
            copy.accept(messLinesAndColumnsVisitor);
            MakeAstValidForPrettyPrintingVisitor.makeValid(copy);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        assertEquals(scrambledLines, makePrint(prefs, copy));

        //Without specials: When creating a copy, the specials won't go along.
        assertEquals(withoutSpecials, makePrint(prefs, node.createCopy()));

        //Regular
        assertEquals(withLinesAndCols, makePrint(prefs, node));
        return node;
    }

    /**
     * @param prefs
     * @param node
     * @return
     * @throws Exception
     */
    public static String makePrint(IPrettyPrinterPrefs prefs, SimpleNode node) throws Error {
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = "";
        try {
            result = printer.print(node);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if (DEBUG) {
            System.out.println("\n\nResult:\n");
            System.out.println("'" + result + "'");
            //            System.out.println("'"+result.replace(' ', '.').replace('\t', '^')+"'");
        }
        return result;
    }

    /**
     * @param file
     * @throws Exception 
     */
    protected void parseAndReparsePrettyPrintedFilesInDir(File file) throws Exception {
        assertTrue("Dir does not exist: " + file, file.exists());
        assertTrue(file.isDirectory());
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            parseAndPrettyPrintFile(f);
        }
    }

    protected void parseAndPrettyPrintFile(File f) throws Error, Exception {
        String lowerCase = f.getAbsolutePath().toLowerCase();
        if (lowerCase.endsWith("decimal.py")) {
            String fileContents = FileUtils.getFileContents(f);
            parseAndPrettyPrintFile(f, fileContents);

        }
    }

    protected void parseAndPrettyPrintFile(File f, String fileContents) throws Exception {
        SimpleNode original = parseLegalDocStr(fileContents, f);
        if (original == null) {
            fail("Error\nUnable to generate the AST for the file:" + f);
        }
        String result = null;
        SimpleNode node = null;
        try {
            result = PrettyPrinterTest.makePrint(prefs, original);
            node = parseLegalDocStr(result);
        } catch (Throwable e) {
            printErrorAndFail(f, original, result, e);
        }
        makeCompare(f, original, node);

        String result2 = null;
        SimpleNode nodePrintingWithoutSpecials = null;
        try {
            //Ok, first print done... go on and create a version without the specials. 
            SimpleNode node2 = node.createCopy();
            result2 = PrettyPrinterTest.makePrint(prefs, node2);
            nodePrintingWithoutSpecials = parseLegalDocStr(result2);
        } catch (Throwable e) {
            printErrorAndFail(f, original, result2, e);
        }
        makeCompare(f, original, nodePrintingWithoutSpecials);
    }

    private void makeCompare(File f, SimpleNode original, SimpleNode node) throws Exception {
        SimpleNodeComparator comparator = new SimpleNodeComparator();
        try {
            comparator.compare(original, node);
        } catch (DifferException e) {
            System.out.println("Compare did not suceed:" + f);
        }
    }

    private void printErrorAndFail(File f, SimpleNode original, String result2, Throwable e) {
        System.out.println("\n\n\n----------------- Initial contents:-------------------------\n");
        System.out.println(original);
        System.out.println("\n\n--------------Pretty-printed contents:------------------\n");
        System.out.println(result2);
        System.out.println("\n\n\n");
        System.out.println("File: " + f);
        e.printStackTrace();

        fail("Error\nUnable to pretty-print regenerated file:" + f);
    }

}
