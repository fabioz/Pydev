package com.python.pydev.analysis.flake8;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

import junit.framework.TestCase;

public class Flake8Test extends TestCase {

    private File tempDir;
    private FileStub file;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_flake8_analysis");
        tempDir.mkdirs();
        ProjectStub project = new ProjectStub(tempDir, null);
        file = new FileStub(project, new File(tempDir, "snippet.py"));
    }

    public void testMarkersMessage1() {
        IDocument document = new Document("x = 10");
        Flake8Analysis flake8Analysis = new Flake8Analysis(file, document, null, new NullProgressMonitor(),
                null);
        String output = ".\\snippet.py:1:7: W292 no newline at end of file";
        flake8Analysis.afterRunProcess(output, "", null);

        List<MarkerInfo> markers = flake8Analysis.getMarkers(file);
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertEquals(1, markers.size());
        assertMarkerEquals(0, 0, 6, "W292", "no newline at end of file", markers);
    }

    public void testMarkersMessage2() {
        IDocument document = new Document("x = 10");
        Flake8Analysis flake8Analysis = new Flake8Analysis(file, document, null, new NullProgressMonitor(),
                null);
        String output = "./snippet.py:1:7: W292 no newline at end of file";
        flake8Analysis.afterRunProcess(output, "", null);

        List<MarkerInfo> markers = flake8Analysis.getMarkers(file);
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertEquals(1, markers.size());
        assertMarkerEquals(0, 0, 6, "W292", "no newline at end of file", markers);
    }

    public void testMarkersMessage3() {
        IDocument document = new Document("import math\n" +
                "x = \"blablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablablabla\"\n"
                +
                "y = 20\n" +
                "    f = 10\n" +
                "def foo():\n" +
                "        w = 10\n" +
                "    pass");
        Flake8Analysis flake8Analysis = new Flake8Analysis(file, document, null, new NullProgressMonitor(),
                null);
        String output = ".\\snippet.py:1:1: E902 IndentationError: unindent does not match any outer indentation level\n"
                +
                ".\\snippet.py:2:80: E501 line too long (129 > 79 characters)\n" +
                ".\\snippet.py:4:5: E113 unexpected indentation\n" +
                ".\\snippet.py:5:1: E302 expected 2 blank lines, found 0\n" +
                ".\\snippet.py:6:9: E117 over-indented";
        flake8Analysis.afterRunProcess(output, "", null);

        List<MarkerInfo> markers = flake8Analysis.getMarkers(file);
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertEquals(5, markers.size());
        assertMarkerEquals(0, 0, 0, "E902", "IndentationError: unindent does not match any outer indentation level",
                markers);
        assertMarkerEquals(1, 1, 79, "E501", "line too long (129 > 79 characters)", markers);
        assertMarkerEquals(2, 3, 4, "E113", "unexpected indentation", markers);
        assertMarkerEquals(3, 4, 0, "E302", "expected 2 blank lines, found 0", markers);
        assertMarkerEquals(4, 5, 8, "E117", "over-indented", markers);
    }

    /** Check the specified marker has been processed correctly
    *
    * @param index index within markers list
    * @param line zero based index of the line in the document
    * @param column zero based index of the column in the document
    * @param message_id pylint short code for message
    * @param message descriptive message
    * @param actualMarkers markers
    */
    private void assertMarkerEquals(int index, int line, int column, String message_id, String message,
            List<MarkerInfo> actualMarkers) {
        MarkerInfo actualMarker = actualMarkers.get(index);
        assertEquals(index + ": MarkerInfo lineStart", line, actualMarker.lineStart);
        assertEquals(index + ": MarkerInfo colStart", column, actualMarker.colStart);
        assertEquals(index + ": MarkerInfo message_id", message_id,
                actualMarker.additionalInfo.get(Flake8Visitor.FLAKE8_MESSAGE_ID));
        assertEquals(index + ": MarkerInfo message", "Flake8: " + message, actualMarker.message);
        Map<String, Object> attribMap = actualMarker.getAsMap();
        //User readable line number (1 based)
        assertEquals(index + ": IMarker.LINE_NUMBER", line + 1, attribMap.get(IMarker.LINE_NUMBER));

    }
}
