package com.python.pydev.analysis.refactoring.quick_fixes;

import org.eclipse.jface.text.Document;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.core.log.Log;

import com.python.pydev.analysis.refactoring.quick_fixes.AddTokenAndImportStatement.ComputedInfo;

import junit.framework.TestCase;

public class AddTokenAndImportStatementTest extends TestCase {

    public String checkImport(String test_document, String test_realImportRep, int test_maxCols) throws Exception {

        Document document = new Document(test_document);

        char trigger = '\n';
        int offset = 0;
        boolean addLocalImport = false;
        boolean addLocalImportsOnTopOfMethod = false;
        boolean groupImports = true;
        int maxCols = test_maxCols;

        AddTokenAndImportStatement stmt = new AddTokenAndImportStatement(document, trigger, offset, addLocalImport,
                addLocalImportsOnTopOfMethod,
                groupImports, maxCols);

        String realImportRep = test_realImportRep;
        int fReplacementOffset = document.getLineLength(0);
        int fLen = 0;
        String indentString = "";
        String fReplacementString = "";
        boolean appliedWithTrigger = false;
        int importLen = 0;

        ComputedInfo computedInfo = new ComputedInfo(realImportRep, fReplacementOffset, fLen, indentString,
                fReplacementString, appliedWithTrigger, importLen, document);
        stmt.createTextEdit(computedInfo);

        for (ReplaceEdit edit : computedInfo.replaceEdit) {
            try {
                edit.apply(document);
            } catch (Exception e) {
                Log.log(e);
            }
        }

        return document.get();

    }

    // normal test
    public void test1() throws Exception {
        String test_document = "from math import ceil";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import ceil, sqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comma
    public void test2() throws Exception {
        String test_document = "from math import ceil,";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import ceil, sqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comment
    public void test3() throws Exception {
        String test_document = "from math import ceil #foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import ceil #foofrom math import sqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis and comment
    public void test4() throws Exception {
        String test_document = "from math import (ceil) #foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import (ceil) #foofrom math import sqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis, comma and comment
    public void test5() throws Exception {
        String test_document = "from math import (ceil,) #foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import (ceil,) #foofrom math import sqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis
    public void test6() throws Exception {
        String test_document = "from math import (ceil)";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import (ceil, sqrt)";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis and comma
    public void test7() throws Exception {
        String test_document = "from math import (ceil,)";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 80);

        String expected = "from math import (ceil, sqrt)";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with cols limit exceeded
    public void test8() throws Exception {
        String test_document = "from math import ceil";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import ceil,\\\nsqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comma and cols limit exceeded
    public void test9() throws Exception {
        String test_document = "from math import ceil,";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import ceil,\\\nsqrt";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis and cols limit exceeded
    public void test10() throws Exception {
        String test_document = "from math import (ceil)";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import (ceil,\nsqrt)";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with parenthesis, comma and cols limit exceeded
    public void test11() throws Exception {
        String test_document = "from math import (ceil,)";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import (ceil,\nsqrt)";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comments after cols limit exceeded 
    public void test12() throws Exception {
        String test_document = "from math import ceil\n#foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import ceil,\\\nsqrt\n#foo";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comments after comma and cols limit exceeded
    public void test13() throws Exception {
        String test_document = "from math import ceil,\n#foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import ceil,\\\nsqrt\n#foo";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comments after parenthesis and cols limit exceeded
    public void test14() throws Exception {
        String test_document = "from math import (ceil)\n#foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import (ceil,\nsqrt)\n#foo";
        String actual = doc;
        assertEquals(expected, actual);
    }

    // test with comments after parenthesis, comma and cols limit exceeded
    public void test15() throws Exception {
        String test_document = "from math import (ceil,)\n#foo";
        String test_realImportRep = "from math import sqrt";
        String doc = checkImport(test_document, test_realImportRep, 20);

        String expected = "from math import (ceil,\nsqrt)\n#foo";
        String actual = doc;
        assertEquals(expected, actual);
    }

    public void testall() throws Exception {
        test1();
        test2();
        test3();
        test4();
        test5();
        test6();
        test7();
        test8();
        test9();
        test10();
        test11();
        test12();
        test13();
        test14();
        test15();
    }

}