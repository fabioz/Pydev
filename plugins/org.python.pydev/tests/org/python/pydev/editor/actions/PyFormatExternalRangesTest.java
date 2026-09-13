/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.ExternalFormatterException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.FormatStd.FormatterEnum;
import org.python.pydev.core.pep8.IExternalFormatterRunner;

import junit.framework.TestCase;

/**
 * Range formatting ("only changed lines") with the external formatters (black / ruff).
 *
 * The external tool is replaced by a fake: what's verified is what PyDev hands to the tool
 * and what PyDev does with the tool's answer.
 */
public class PyFormatExternalRangesTest extends TestCase {

    private static final class FakeExternalFormatter implements IExternalFormatterRunner {

        final List<int[]> receivedLines = new ArrayList<>();
        String output;
        ExternalFormatterException failure;

        @Override
        public String format(String filepath, IPythonNature nature, IDocument doc, FormatStd std, File workingDir,
                int[] zeroBasedLines) throws ExternalFormatterException {
            receivedLines.add(zeroBasedLines);
            if (failure != null) {
                throw failure;
            }
            return output;
        }
    }

    private FakeExternalFormatter fake;
    private Function<FormatterEnum, IExternalFormatterRunner> previousProvider;
    private FormatStd std;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        fake = new FakeExternalFormatter();
        previousProvider = PyFormatter.externalFormatterRunnerProvider;
        PyFormatter.externalFormatterRunnerProvider = style -> fake;
        std = new FormatStd();
        std.formatterStyle = FormatterEnum.BLACK;
        std.updateFormatterStyle();
    }

    @Override
    protected void tearDown() throws Exception {
        PyFormatter.externalFormatterRunnerProvider = previousProvider;
        super.tearDown();
    }

    public void testFormatSelectionWithBlackSendsOnlyTheChangedLinesOnce() throws Exception {
        Document doc = new Document("a=1\nb  =  2\nc   = 3\nd=4\n");
        fake.output = "a=1\nb = 2\nc = 3\nd=4\n";

        PyFormatter.formatSelection(doc, new int[] { 1, 2 }, null, new PySelection(doc), std);

        assertEquals(1, fake.receivedLines.size());
        assertEquals("[1, 2]", Arrays.toString(fake.receivedLines.get(0)));
        assertEquals(fake.output, doc.get());
    }

    public void testFormatSelectionWithBlackKeepsTheToolOutputBlankLines() throws Exception {
        Document doc = new Document("def a():\n    pass\ndef b():\n    pass\n");
        // 4 blank lines: PyDev's own blank-line rules (forced on for external formatters) would cut these to 2.
        fake.output = "def a():\n    pass\n\n\n\n\ndef b():\n    pass\n";

        PyFormatter.formatSelection(doc, new int[] { 2, 3 }, null, new PySelection(doc), std);

        assertEquals(fake.output, doc.get());
    }

    public void testFormatSelectionWithNoLinesDoesNotRunTheTool() throws Exception {
        String original = "a=1\n";
        Document doc = new Document(original);
        fake.output = "a = 1\n";

        PyFormatter.formatSelection(doc, new int[0], null, new PySelection(doc), std);

        assertEquals(0, fake.receivedLines.size());
        assertEquals(original, doc.get());
    }

    public void testFormatSelectionLeavesTheDocumentUntouchedWhenTheToolFails() {
        String original = "a=1\nb  =  2\n";
        Document doc = new Document(original);
        fake.failure = new ExternalFormatterException("black", "black exited with 2");

        try {
            PyFormatter.formatSelection(doc, new int[] { 1 }, null, new PySelection(doc), std);
            fail("Expected ExternalFormatterException");
        } catch (ExternalFormatterException e) {
            assertEquals("black", e.getToolName());
            assertEquals(original, doc.get());
        }
    }

    public void testFormatAllWithRuffPropagatesTheToolFailure() throws Exception {
        std.formatterStyle = FormatterEnum.RUFF;
        String original = "a=1\n";
        Document doc = new Document(original);
        fake.failure = new ExternalFormatterException("ruff", "ruff exited with 2");

        try {
            PyFormatter.formatAll(null, doc, null, true, std, true, true);
            fail("Expected ExternalFormatterException");
        } catch (ExternalFormatterException e) {
            assertEquals("ruff", e.getToolName());
            assertEquals(original, doc.get());
        }
    }
}
