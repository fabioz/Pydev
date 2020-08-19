package com.python.pydev.analysis.mypy;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

import junit.framework.TestCase;

public class MypyAnalysisTest extends TestCase {

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

        MypyAnalysis mypyAnalysis = new MypyAnalysis(null, document, null, new NullProgressMonitor(),
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

        assertEquals(1, mypyAnalysis.markers.size());

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

        assertEquals(expected, mypyAnalysis.markers.get(0).message);
    }
}
