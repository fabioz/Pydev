package com.python.pydev.analysis.mypy;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

import junit.framework.TestCase;

public class MypyAnalysisTest extends TestCase {

    private File tempDir;
    private FileStub file;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_mypy_analysis");
        tempDir.mkdirs();
        ProjectStub project = new ProjectStub(tempDir, null);
        file = new FileStub(project, new File(tempDir, "snippet.py"));
    }

    public void testMarkersMessage() {

        IDocument document = new Document(
                "" +
                        "from typing import Protocol\n" +
                        "\n" +
                        "\n" +
                        "class IBar(Protocol):\n" +
                        "\n" +
                        "    def bar(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "class NotBar(object):\n" +
                        "\n" +
                        "    def bar(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "def call(a:IBar) -> IBar:\n" +
                        "    pass\n" +
                        "\n" +
                        "\n" +
                        "x:IBar = NotBar()");

        MypyAnalysis mypyAnalysis = new MypyAnalysis(file, document, null, new NullProgressMonitor(),
                null);

        String output = "snippet.py:26:11: error: Incompatible types in assignment (expression has type \"NotBar\", variable has type \"IBar\")\n"
                +
                "snippet.py:26:11: note: Following member(s) of \"NotBar\" have conflicts:\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def bar(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def bar(self) -> int\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def foo(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def foo(self) -> int\n" +
                "Found 1 error in 1 file (checked 1 source file)\n" +
                "";

        mypyAnalysis.afterRunProcess(output, "", null);

        assertEquals(1, mypyAnalysis.getMarkers(file).size());

        String expected = "Mypy: Incompatible types in assignment (expression has type \"NotBar\", variable has type \"IBar\")\n"
                +
                "Following member(s) of \"NotBar\" have conflicts:\n" +
                "Expected:\n" +
                "def bar(self) -> bool\n" +
                "Got:\n" +
                "def bar(self) -> int\n" +
                "Expected:\n" +
                "def foo(self) -> bool\n" +
                "Got:\n" +
                "def foo(self) -> int";

        assertEquals(expected, mypyAnalysis.getMarkers(file).get(0).message);
    }

    public void testMarkersMessageWithoutCol() {

        IDocument document = new Document(
                "" +
                        "from typing import Protocol\n" +
                        "\n" +
                        "\n" +
                        "class IBar(Protocol):\n" +
                        "\n" +
                        "    def bar(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "class NotBar(object):\n" +
                        "\n" +
                        "    def bar(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "def call(a:IBar) -> IBar:\n" +
                        "    pass\n" +
                        "\n" +
                        "\n" +
                        "x:IBar = NotBar()");

        MypyAnalysis mypyAnalysis = new MypyAnalysis(file, document, null, new NullProgressMonitor(),
                null);

        String output = "snippet.py:26: error: Incompatible types in assignment (expression has type \"NotBar\", variable has type \"IBar\")\n"
                +
                "snippet.py:26:11: note: Following member(s) of \"NotBar\" have conflicts:\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def bar(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def bar(self) -> int\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def foo(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def foo(self) -> int\n" +
                "Found 1 error in 1 file (checked 1 source file)\n" +
                "";

        mypyAnalysis.afterRunProcess(output, "", null);

        assertEquals(2, mypyAnalysis.getMarkers(file).size());

        String expected1 = "Mypy: Following member(s) of \"NotBar\" have conflicts:\n" +
                "Expected:\n" +
                "def bar(self) -> bool\n" +
                "Got:\n" +
                "def bar(self) -> int\n" +
                "Expected:\n" +
                "def foo(self) -> bool\n" +
                "Got:\n" +
                "def foo(self) -> int";
        String expected2 = "Mypy: Incompatible types in assignment (expression has type \"NotBar\", variable has type \"IBar\")";
        List<MarkerInfo> markers = mypyAnalysis.getMarkers(file);
        Set<String> s = new HashSet<>();
        s.add(expected1);
        s.add(expected2);

        // Compare using Set as the order is not deterministic.
        assertEquals(s, new HashSet<>(
                Arrays.asList(markers.get(0).message, markers.get(1).message)));
    }

    public void testMarkersMessageWithoutCol2() {

        IDocument document = new Document(
                "" +
                        "def method(a: int):\n" +
                        "    pass\n" +
                        "\n" +
                        "method('')  # type: ignore -- error due to this comment\n" +
                        "method('')  # This is not reported due to the invalid type ignore in the other line");

        MypyAnalysis mypyAnalysis = new MypyAnalysis(file, document, null, new NullProgressMonitor(),
                null);

        String output = "snippet.py:4: error: Invalid \"type: ignore\" comment";

        mypyAnalysis.afterRunProcess(output, "", null);

        assertEquals(1, mypyAnalysis.getMarkers(file).size());

        String expected = "Mypy: Invalid \"type: ignore\" comment";

        assertEquals(expected, mypyAnalysis.getMarkers(file).get(0).message);
    }

    public void testMarkersMessage2() {

        IDocument document = new Document(
                "" +
                        "from typing import Protocol\n" +
                        "\n" +
                        "\n" +
                        "class IBar(Protocol):\n" +
                        "\n" +
                        "    def bar(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> bool:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "class NotBar(object):\n" +
                        "\n" +
                        "    def bar(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "    def foo(self) -> int:\n" +
                        "        pass\n" +
                        "\n" +
                        "\n" +
                        "def call(a:IBar) -> IBar:\n" +
                        "    pass\n" +
                        "\n" +
                        "\n" +
                        "x:IBar = NotBar() # noqa");

        MypyAnalysis mypyAnalysis = new MypyAnalysis(file, document, null, new NullProgressMonitor(),
                null);

        String output = "snippet.py:26:11: error: Incompatible types in assignment (expression has type \"NotBar\", variable has type \"IBar\")\n"
                +
                "snippet.py:26:11: note: Following member(s) of \"NotBar\" have conflicts:\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def bar(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def bar(self) -> int\n" +
                "snippet.py:26:11: note:     Expected:\n" +
                "snippet.py:26:11: note:         def foo(self) -> bool\n" +
                "snippet.py:26:11: note:     Got:\n" +
                "snippet.py:26:11: note:         def foo(self) -> int\n" +
                "Found 1 error in 1 file (checked 1 source file)\n" +
                "";

        mypyAnalysis.afterRunProcess(output, "", null);

        assertEquals(0, mypyAnalysis.getMarkers(file).size()); // No message because of the `noqa`.
    }

    public void testMessagesFromAnotherFile() {
        IDocument document = new Document(
                "" +
                        "some_variable = 10");

        Path docPath = new Path("/sample/src/package1/module1.py");
        MypyAnalysis mypyAnalysis = new MypyAnalysis(file, document, docPath, new NullProgressMonitor(),
                null);

        String output = " src\\package1-stubs\\__init__.pyi:10:1: error: Name 'logger' already defined on line 9\n" +
                "Found 1 error in 1 file (checked 1 source file)\n";

        mypyAnalysis.afterRunProcess(output, "", null);

        assertEquals(0, mypyAnalysis.getMarkers(file).size());
    }
}
