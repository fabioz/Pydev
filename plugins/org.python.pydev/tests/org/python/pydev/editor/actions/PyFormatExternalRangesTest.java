/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.Document;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.FormatStd.FormatterEnum;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;

import junit.framework.TestCase;

/**
 * Formatting only some lines with black / ruff, going through a real process: the tool is a small
 * POSIX shell script which records its arguments, so neither black nor ruff has to be installed.
 */
public class PyFormatExternalRangesTest extends TestCase {

    private static final String ECHO_INPUT = "cat";

    private File workDir;
    private File recordedRuns;

    @Override
    public void runBare() throws Throwable {
        if (!PlatformUtils.isWindowsPlatform()) {
            super.runBare();
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        workDir = Files.createTempDirectory("pydev-fake-formatter").toFile();
        recordedRuns = new File(workDir, "runs.txt");
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtils.deleteDirectoryTree(workDir);
        super.tearDown();
    }

    public void testBlackGetsOneLineRangePerBlockOfLines() throws Exception {
        format(FormatterEnum.BLACK, "--fast", ECHO_INPUT, "a\nb\nc\nd\ne\nf\n", 4, 0, 1);

        assertRuns("- --fast --line-ranges=1-2 --line-ranges=5-5");
    }

    public void testRuffRunsOncePerBlockFromTheBottomUp() throws Exception {
        format(FormatterEnum.RUFF, "--line-length=100", ECHO_INPUT, "a\nb\nc\nd\ne\nf\n", 4, 0, 1);

        assertRuns("format - --line-length=100 --range=5-6", "format - --line-length=100 --range=1-3");
    }

    public void testRuffRangeReachingTheLastLineHasNoEnd() throws Exception {
        format(FormatterEnum.RUFF, "", ECHO_INPUT, "a=1\nb=2", 1);

        assertRuns("format - --range=2");
    }

    public void testEachRuffRunContinuesFromThePreviousResult() throws Exception {
        Document doc = format(FormatterEnum.RUFF, "", "cat; echo '#formatted'", "a=1\nb=2\nc=3\n", 0, 2);

        assertEquals("a=1\nb=2\nc=3\n#formatted\n#formatted\n", doc.get());
    }

    public void testNewLineIsNotAddedAtTheEndOfTheFile() throws Exception {
        Document doc = format(FormatterEnum.RUFF, "", ECHO_INPUT, "a=1", 0);

        assertEquals("a=1", doc.get());
    }

    private void assertRuns(String... expected) throws Exception {
        assertEquals(Arrays.asList(expected), Files.readAllLines(recordedRuns.toPath()));
    }

    /**
     * @param toolBody shell commands of the fake tool (stdin: the document, stdout: the formatted document).
     */
    private Document format(FormatterEnum style, String parameters, String toolBody, String contents, int... lines)
            throws Exception {
        File tool = new File(workDir, "fake-formatter.sh");
        FileUtils.writeStrToFile("#!/bin/sh\necho \"$@\" >> \"" + recordedRuns + "\"\n" + toolBody + "\n", tool);
        assertTrue(tool.setExecutable(true));

        FormatStd std = new FormatStd();
        std.formatterStyle = style;
        std.updateFormatterStyle(); // Like in the preferences: e.g. asks for a new line at the end of the file.
        std.searchBlackInInterpreter = false;
        std.blackExecutableLocation = tool.getPath();
        std.blackParameters = parameters;
        std.searchRuffInInterpreter = false;
        std.ruffExecutableLocation = tool.getPath();
        std.ruffParameters = parameters;

        Document doc = new Document(contents);
        PyFormatter.formatSelection(doc, lines, new FakeEditor(std), new PySelection(doc), std);
        return doc;
    }

    /**
     * As in PyEdit, each call to getFormatStd() gives a new instance.
     */
    private static final class FakeEditor implements IPyFormatStdProvider, IAdaptable {

        private final FormatStd std;

        FakeEditor(FormatStd std) {
            this.std = std;
        }

        @Override
        public Object getFormatStd() {
            return std.clone();
        }

        @Override
        public IPythonNature getPythonNature() {
            return null;
        }

        @Override
        public IGrammarVersionProvider getGrammarVersionProvider() {
            return null;
        }

        @Override
        public IIndentPrefs getIndentPrefs() {
            return null;
        }

        @Override
        public File getEditorFile() {
            return null;
        }

        @Override
        public <T> T getAdapter(Class<T> adapter) {
            return null;
        }
    }
}
