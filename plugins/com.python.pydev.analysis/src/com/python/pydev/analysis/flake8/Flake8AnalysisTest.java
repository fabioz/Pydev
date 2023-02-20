package com.python.pydev.analysis.flake8;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

import junit.framework.TestCase;

public class Flake8AnalysisTest extends TestCase {

    private File tempDir;
    private FileStub file;
    private int index = 0;
    private List<MarkerInfo> markers;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_flake8_analysis");
        tempDir.mkdirs();
        ProjectStub project = new ProjectStub(tempDir, new PythonNature());
        file = new FileStub(project, new File("snippet.py"));
    }

    private void setUpTestForAssertions(IDocument document, String output) {
        Flake8Analysis flake8Analysis = new Flake8Analysis(file, document, null, new NullProgressMonitor(), null);
        flake8Analysis.afterRunProcess(output, "", null);
        setMarkersAndResetIndex(flake8Analysis);
    }

    private void setMarkersAndResetIndex(Flake8Analysis flake8Analysis) {
        markers = flake8Analysis.getMarkers(file);
        index = 0;
    }

    private void assertMarkersValidity(int expectedAmountOfMarkers) {
        assertEquals(expectedAmountOfMarkers, markers.size());
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }
    }

    /** Check the specified marker has been processed correctly
    *
    * @param line (zero based)
    * @param column (zero based)
    * @param violationCode flake8 violation code
    * @param message flake8 descriptive message
    * @param actualMarkers markers
    */
    private void assertMarkerEquals(int line, int column, String violationCode, String message) {
        MarkerInfo marker = markers.get(index);
        assertEquals(index + ": MarkerInfo lineStart", line, marker.lineStart);
        assertEquals(index + ": MarkerInfo colStart", column, marker.colStart);
        assertEquals(index + ": MarkerInfo message_id", violationCode,
                marker.additionalInfo.get(Flake8Visitor.FLAKE8_MESSAGE_ID));
        assertEquals(index + ": MarkerInfo message", "Flake8: " + message, marker.message);
        Map<String, Object> attribMap = marker.getAsMap();
        assertEquals(index + ": IMarker.LINE_NUMBER", line + 1, attribMap.get(IMarker.LINE_NUMBER));
        index++;
    }

    public void testMarkersMessage()
            throws CoreException, MisconfigurationException, PythonNatureWithoutProjectException {
        IDocument document = new Document("x = 10");
        String output = ".\\snippet.py:1:7: W292 no newline at end of file";
        setUpTestForAssertions(document, output);
        assertMarkersValidity(1);
        assertMarkerEquals(0, 0, "W292", "no newline at end of file (W292)");
    }

    public void testMarkersMessage1() {
        IDocument document = new Document("import math\n" +
                "x = \"longlinelonglinelonglinelonglinelonglinelonglinelonglinelonglinelonglinelongline\"\n" +
                "y = 20\n" +
                "    f = 10\n" +
                "def foo():\n" +
                "        w = 10\n" +
                "    pass");

        String output = ".\\snippet.py:1:1: E902 IndentationError: unindent does not match any outer indentation level\n"
                +
                ".\\snippet.py:2:80: E501 line too long (80 > 79 characters)\n" +
                ".\\snippet.py:4:5: E113 unexpected indentation\n" +
                ".\\snippet.py:5:1: E302 expected 2 blank lines, found 0\n" +
                ".\\snippet.py:6:9: E117 over-indented";

        setUpTestForAssertions(document, output);
        assertMarkersValidity(5);
        assertMarkerEquals(0, 0, "E902",
                "IndentationError: unindent does not match any outer indentation level (E902)");
        assertMarkerEquals(1, 79, "E501", "line too long (80 > 79 characters) (E501)");
        assertMarkerEquals(3, 4, "E113", "unexpected indentation (E113)");
        assertMarkerEquals(4, 0, "E302", "expected 2 blank lines, found 0 (E302)");
        assertMarkerEquals(5, 8, "E117", "over-indented (E117)");
    }

    public void testMarkerSpecificNoqa() {
        IDocument document = new Document("" +
                "y = 20\n" +
                "    f = 10 #noqa: E302\n" + // this line will not return a flake8 E302 violation code, so the actual return will not be silenced.
                "def foo():\n" +
                "        w = 10\n" +
                "    pass");

        String output = "" +
                ".\\snippet.py:1:1: E902 IndentationError: unindent does not match any outer level\n" +
                ".\\snippet.py:2:5: E113 unexpected indentation\n" +
                ".\\snippet.py:3:1: E302 expected 2 blank lines, found 0\n" +
                ".\\snippet.py:4:9: E117 over-indented";

        setUpTestForAssertions(document, output);
        assertMarkersValidity(4);
        assertMarkerEquals(0, 0, "E902", "IndentationError: unindent does not match any outer level (E902)");
        assertMarkerEquals(1, 4, "E113", "unexpected indentation (E113)");
        assertMarkerEquals(2, 0, "E302", "expected 2 blank lines, found 0 (E302)");
        assertMarkerEquals(3, 8, "E117", "over-indented (E117)");
    }

    public void testMarkerSpecificNoqa1() {
        IDocument document = new Document("" +
                "y = 20\n" +
                "    f = 10 #noqa: E113\n" + // this should silence the specific flake8 E113 violation output at line.
                "def foo():\n" +
                "        w = 10\n" +
                "    pass");

        String output = "" +
                ".\\snippet.py:1:1: E902 IndentationError: unindent does not match any outer level\n" +
                ".\\snippet.py:2:5: E113 unexpected indentation\n" +
                ".\\snippet.py:3:1: E302 expected 2 blank lines, found 0\n" +
                ".\\snippet.py:4:9: E117 over-indented";

        setUpTestForAssertions(document, output);
        assertMarkersValidity(3);
        assertMarkerEquals(0, 0, "E902", "IndentationError: unindent does not match any outer level (E902)");
        assertMarkerEquals(2, 0, "E302", "expected 2 blank lines, found 0 (E302)");
        assertMarkerEquals(3, 8, "E117", "over-indented (E117)");
    }
}
