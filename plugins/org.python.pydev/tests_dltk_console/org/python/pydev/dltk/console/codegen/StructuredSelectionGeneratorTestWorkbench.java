/*******************************************************************************
 * Copyright (C) 2011, 2013  Fabio Zadrozny and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Fabio Zadrozny <fabiofz@gmail.com>    - Initial API and implementation.
 *******************************************************************************/
package org.python.pydev.dltk.console.codegen;

import junit.framework.TestCase;

import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.shared_interactive_console.console.codegen.IScriptConsoleCodeGenerator;
import org.python.pydev.shared_interactive_console.console.codegen.PythonSnippetUtils;
import org.python.pydev.shared_interactive_console.console.codegen.SafeScriptConsoleCodeGenerator;
import org.python.pydev.shared_interactive_console.console.codegen.StructuredSelectionScriptConsoleCodeGenerator;

public class StructuredSelectionGeneratorTestWorkbench extends TestCase {

    /** An object that can be put into a Selection within this test */
    private static class TestSelectableObject implements IScriptConsoleCodeGenerator {
        private final String pyCode;
        private final boolean hasPyCode;

        /**
         * A version that has (if hasPyCode == true) a late realisation that it
         * can't generate the PyCode
         */
        public TestSelectableObject(boolean hasPyCode) {
            this.pyCode = null;
            this.hasPyCode = hasPyCode;
        }

        public TestSelectableObject(String pyCode) {
            this.pyCode = pyCode;
            this.hasPyCode = pyCode != null && pyCode.length() > 0;
        }

        public TestSelectableObject() {
            this.pyCode = null;
            this.hasPyCode = false;
        }

        @Override
        public String getPyCode() {
            return pyCode;
        }

        @Override
        public boolean hasPyCode() {
            return hasPyCode;
        }

    }

    /**
     * An object that can be put into a Selection within this test that throws
     * an exception on any call to getPyCode/hasPyCode
     */
    private static class TestExceptionObject implements IScriptConsoleCodeGenerator {
        @Override
        public String getPyCode() {
            throw new RuntimeException("getPyCode Forced Failure");
        }

        @Override
        public boolean hasPyCode() {
            throw new RuntimeException("hasPyCode Forced Failure");
        }

    }

    public void testGetAdapterForStructuredSelection() {
        IScriptConsoleCodeGenerator generator = PythonSnippetUtils
                .getScriptConsoleCodeGeneratorAdapter(StructuredSelection.EMPTY);
        assertEquals(StructuredSelectionScriptConsoleCodeGenerator.class, generator.getClass());
    }

    /**
     * Just to make our test cases smaller, extract the long method name as a
     * shorter one. Don't use the {@link SafeScriptConsoleCodeGenerator} wrapper
     * in here, we very much want to see the exceptions in the JUnit output
     */
    public IScriptConsoleCodeGenerator getGen(StructuredSelection selection) {
        return PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(selection);
    }

    public void testEmptySelection() {
        assertEquals(false, getGen(StructuredSelection.EMPTY).hasPyCode());
        assertEquals(null, getGen(StructuredSelection.EMPTY).getPyCode());
    }

    public void testSingleSelection() {
        StructuredSelection selection = new StructuredSelection(new Object());
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new TestSelectableObject());
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new TestSelectableObject("pycode"));
        assertEquals(true, getGen(selection).hasPyCode());
        assertEquals("pycode", getGen(selection).getPyCode());

        selection = new StructuredSelection(new TestSelectableObject(true));
        assertEquals(true, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());
    }

    public void testMultiSelection_NoPyCodeAvailable() {
        StructuredSelection selection = new StructuredSelection(new Object[] { new Object(), new Object() });
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new Object[] { new Object(), new TestSelectableObject("pycode") });
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new Object[] { new TestSelectableObject("pycode"), new Object() });
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new Object[] { new TestSelectableObject(),
                new TestSelectableObject("pycode") });
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());

        selection = new StructuredSelection(new Object[] { new TestSelectableObject(), new TestSelectableObject() });
        assertEquals(false, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());
    }

    public void testMultiSelection_LateNoPyCodeAvailable() {
        StructuredSelection selection = new StructuredSelection(new Object[] { new TestSelectableObject(true),
                new TestSelectableObject("pycode") });
        assertEquals(true, getGen(selection).hasPyCode());
        assertEquals(null, getGen(selection).getPyCode());
    }

    public void testMultiSelection_PyCodeAvailable() {
        StructuredSelection selection = new StructuredSelection(new Object[] { new TestSelectableObject("pycode1"),
                new TestSelectableObject("pycode2") });
        assertEquals(true, getGen(selection).hasPyCode());
        assertEquals("(pycode1, pycode2)", getGen(selection).getPyCode());
    }

    public void testNestedSelection() {
        StructuredSelection selection_inner = new StructuredSelection(new Object[] {
                new TestSelectableObject("pycode_inner1"), new TestSelectableObject("pycode_inner2") });
        StructuredSelection selection = new StructuredSelection(new Object[] { new TestSelectableObject("pycode1"),
                new TestSelectableObject("pycode2"), selection_inner });
        assertEquals(true, getGen(selection).hasPyCode());
        assertEquals("(pycode1, pycode2, (pycode_inner1, pycode_inner2))", getGen(selection).getPyCode());
    }

    public void testSafeRunner() {
        IScriptConsoleCodeGenerator generator = new TestExceptionObject();
        try {
            generator.hasPyCode();
            fail();
        } catch (RuntimeException e) {
            assertEquals("hasPyCode Forced Failure", e.getMessage());
        }
        try {
            generator.getPyCode();
            fail();
        } catch (RuntimeException e) {
            assertEquals("getPyCode Forced Failure", e.getMessage());
        }

        IScriptConsoleCodeGenerator wrapped = new SafeScriptConsoleCodeGenerator(generator);
        assertEquals(false, wrapped.hasPyCode());
        assertEquals(null, wrapped.getPyCode());

        StructuredSelection selection = new StructuredSelection(generator);
        IScriptConsoleCodeGenerator wrappedSelection = new SafeScriptConsoleCodeGenerator(getGen(selection));
        assertEquals(false, wrappedSelection.hasPyCode());
        assertEquals(null, wrappedSelection.getPyCode());
    }

}
