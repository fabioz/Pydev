/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;

public class PyASTChangerTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PyASTChangerTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test1() throws Exception {
        Document doc = new Document("");
        PyASTChanger changer = new PyASTChanger(doc);
        changer.getAST();
    }
}
