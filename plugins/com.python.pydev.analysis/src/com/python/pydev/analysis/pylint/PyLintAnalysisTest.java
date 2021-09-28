package com.python.pydev.analysis.pylint;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;
import org.python.pydev.shared_core.resource_stubs.FileStub;
import org.python.pydev.shared_core.resource_stubs.ProjectStub;

import junit.framework.TestCase;

public class PyLintAnalysisTest extends TestCase {

    private File tempDir;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        tempDir = FileUtils.getTempFileAt(new File("."), "data_pylint_analysis");
        tempDir.mkdirs();
    }

    /**
     * Verify markers are created with correct line number and text
     *  when supplied simulated PyLint input and output.
     *
     * @throws UnableToFindExecutableException
     * @throws MisconfigurationException
     * @throws PythonNatureWithoutProjectException
     */
    public void testMarkersInfoOutput()
            throws UnableToFindExecutableException, MisconfigurationException, PythonNatureWithoutProjectException {

        ProjectStub project = new ProjectStub(tempDir, null);
        FileStub file = new FileStub(project, new File(tempDir, "file.py"));

        IDocument document = new Document(
                "" +
                        "import string\n" +
                        "\n" +
                        "shift = 3\n" +
                        "choice = input(\"would you like to encode or decode?\")\n" +
                        "word = input(\"Please enter text\")\n" +
                        "letters = string.ascii_letters + string.punctuation + string.digits\n" +
                        "encoded = ''\n" +
                        "if choice == \"encode\":\n" +
                        "    for letter in word:\n" +
                        "        if letter == ' ':\n" +
                        "            encoded = encoded + ' '\n" +
                        "        else:\n" +
                        "            x = letters.index(letter) + shift\n" +
                        "            encoded=encoded + letters[x]\n" +
                        "if choice == \"decode\":\n" +
                        "    for letter in word:\n" +
                        "        if letter == ' ':\n" +
                        "            encoded = encoded + ' '\n" +
                        "        else:\n" +
                        "            x = letters.index(letter) - shift\n" +
                        "            encoded = encoded + letters[x]\n" +
                        "print(encoded)\n" +
                        "print(\"This line is really long\")" +
                        " # This comment is part of the long line, going over the 100 char limit");

        PyLintAnalysis pyLintAnalysis = new PyLintAnalysis(file, document, null, new NullProgressMonitor(),
                null);

        String output = "PyLint: The stdout of the command line is:\n"
                + " ************* Module snippet\n"
                + "C: 14,19: (bad-whitespace) Exactly one space required around assignment\n"
                + "            encoded=encoded + letters[x]\n"
                + "\n"
                + "                   ^\n"
                + "C: 17,25: (trailing-whitespace) Trailing whitespace\n"
                + "W: 22, 0: (unnecessary-semicolon) Unnecessary semicolon\n"
                + "C: 23, 0: (line-too-long) Line too long (106/100)\n"
                + "C: 23, 0: (missing-final-newline) Final newline missing\n"
                + "C:  1, 0: (missing-module-docstring) Missing module docstring\n"
                + "C:  3, 0: (invalid-name) Constant name \"shift\" doesn't conform to UPPER_CASE naming style\n"
                + "C:  7, 0: (invalid-name) Constant name \"encoded\" doesn't conform to UPPER_CASE naming style\n"
                + "C: 11,12: (invalid-name) Constant name \"encoded\" doesn't conform to UPPER_CASE naming style\n"
                + "\n"
                + "------------------------------------------------------------------\n"
                + "\n"
                + "Your code has been rated at 5.50/10 (previous run: 5.26/10, +0.00)\n";

        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_WARNINGS, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_CODING_STANDARD, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_REFACTOR, IMarker.SEVERITY_ERROR);

        pyLintAnalysis.afterRunProcess(output, "No config file found, using default configuration\n", null);

        List<MarkerInfo> markers = pyLintAnalysis.getMarkers(file);
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertEquals(9, markers.size());
        assertMarkerEquals(0, 13, "bad-whitespace", "Exactly one space required around assignment", markers);
        assertMarkerEquals(1, 16, "trailing-whitespace", "Trailing whitespace", markers);
        assertMarkerEquals(2, 21, "unnecessary-semicolon", "Unnecessary semicolon", markers);
        assertMarkerEquals(3, 22, "line-too-long", "Line too long (106/100)", markers);
        assertMarkerEquals(4, 22, "missing-final-newline", "Final newline missing", markers);
        assertMarkerEquals(5, 0, "missing-module-docstring", "Missing module docstring", markers);
        assertMarkerEquals(6, 2, "invalid-name", "Constant name \"shift\" doesn't conform to UPPER_CASE naming style",
                markers);
        assertMarkerEquals(7, 6, "invalid-name", "Constant name \"encoded\" doesn't conform to UPPER_CASE naming style",
                markers);
        assertMarkerEquals(8, 10, "invalid-name",
                "Constant name \"encoded\" doesn't conform to UPPER_CASE naming style",
                markers);
    }

    /** Check the specified marker has been processed correctly
     *
     * @param index index within markers list
     * @param line zero based index of the line in the document
     * @param message_id pylint short code for message
     * @param message descriptive message
     * @param actualMarkers markers
     */
    private void assertMarkerEquals(int index, int line, String message_id, String message,
            List<MarkerInfo> actualMarkers) {

        MarkerInfo actualMarker = actualMarkers.get(index);

        assertEquals(index + ": MarkerInfo lineStart", line, actualMarker.lineStart);
        assertEquals(index + ": MarkerInfo message_id", message_id,
                actualMarker.additionalInfo.get(PyLintVisitor.PYLINT_MESSAGE_ID));
        assertEquals(index + ": MarkerInfo message", "PyLint: " + message, actualMarker.message);

        Map<String, Object> attribMap = actualMarker.getAsMap();
        //User readable line number (1 based)
        assertEquals(index + ": IMarker.LINE_NUMBER", line + 1, attribMap.get(IMarker.LINE_NUMBER));

    }

}
