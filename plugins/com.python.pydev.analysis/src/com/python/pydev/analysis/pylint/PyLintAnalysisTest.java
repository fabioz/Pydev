package com.python.pydev.analysis.pylint;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterInfo.UnableToFindExecutableException;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.preferences.PydevPrefs;
import org.python.pydev.shared_core.markers.PyMarkerUtils.MarkerInfo;

import junit.framework.TestCase;

public class PyLintAnalysisTest extends TestCase {

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

        PyLintAnalysis pyLintAnalysis = new PyLintAnalysis(null, document, null, new NullProgressMonitor(),
                null);

        String output = "PyLint: The stdout of the command line is:\n"
                + " ************* Module snippet\n"
                + "C: 23, 0: (line-too-long) Line too long (106/100)\n"
                + "C: 23, 0: (missing-final-newline) Final newline missing\n"
                + "C:  1, 0: (missing-module-docstring) Missing module docstring\n"
                + "C:  3, 0: (invalid-name) Constant name \"shift\" doesn't conform to UPPER_CASE naming style\n"
                + "C:  7, 0: (invalid-name) Constant name \"encoded\" doesn't conform to UPPER_CASE naming style\n"
                + "C: 11,12: (invalid-name) Constant name \"encoded\" doesn't conform to UPPER_CASE naming style\n"
                + "\n"
                + "------------------------------------------------------------------\n"
                + "\n"
                + "Your code has been rated at 7.00/10 (previous run: 5.26/10, +0.00)\n";

        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_WARNINGS, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_CODING_STANDARD, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_REFACTOR, IMarker.SEVERITY_ERROR);

        pyLintAnalysis.afterRunProcess(output, "No config file found, using default configuration\n", null);

        List<MarkerInfo> markers = pyLintAnalysis.markers;
        for (MarkerInfo marker : markers) {
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertMarkerEquals(22, "line-too-long", "Line too long (106/100)", markers.get(0));
        assertMarkerEquals(22, "missing-final-newline", "Final newline missing", markers.get(1));
        assertMarkerEquals(0, "missing-module-docstring", "Missing module docstring", markers.get(2));
        assertMarkerEquals(2, "invalid-name", "Constant name \"shift\" doesn't conform to UPPER_CASE naming style",
                markers.get(3));
        assertMarkerEquals(6, "invalid-name", "Constant name \"encoded\" doesn't conform to UPPER_CASE naming style",
                markers.get(4));
        assertMarkerEquals(10, "invalid-name", "Constant name \"encoded\" doesn't conform to UPPER_CASE naming style",
                markers.get(5));
        assertEquals(6, markers.size());
    }

    private void assertMarkerEquals(int line, String message_id, String message, MarkerInfo actualMarker) {
        assertEquals(line, actualMarker.lineStart);
        assertEquals(message_id, actualMarker.additionalInfo.get(PyLintVisitor.PYLINT_MESSAGE_ID));
        assertEquals("PyLint: " + message, actualMarker.message);
    }
}
