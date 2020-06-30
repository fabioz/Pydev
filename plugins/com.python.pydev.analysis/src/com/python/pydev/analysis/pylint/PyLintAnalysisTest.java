package com.python.pydev.analysis.pylint;

import java.util.ArrayList;
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

    public void testMessageOutput()
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
                        "print(encoded)");

        PyLintAnalysis pyLintAnalysis = new PyLintAnalysis(null, document, null, new NullProgressMonitor(),
                null);

        String output = "************* Module snippet\n" +
                "C: 14, 0: (bad-whitespace) Exactly one space required around assignment\n" +
                "            encoded=encoded + letters[x]\n" +
                "\n" +
                "                   ^\n" +
                "C: 22, 0: (missing-final-newline) Final newline missing\n" +
                "C: 22, 0: (superfluous-parens) Unnecessary parens after 'print' keyword\n" +
                "C:  1, 0: (missing-docstring) Missing module docstring\n" +
                "C:  3, 0: (invalid-name) Constant name \"shift\" doesn't conform to UPPER_CASE naming style\n" +
                "C:  4, 0: (invalid-name) Constant name \"choice\" doesn't conform to UPPER_CASE naming style\n" +
                "C:  5, 0: (invalid-name) Constant name \"word\" doesn't conform to UPPER_CASE naming style\n" +
                "C:  6, 0: (invalid-name) Constant name \"letters\" doesn't conform to UPPER_CASE naming style\n" +
                "C:  7, 0: (invalid-name) Constant name \"encoded\" doesn't conform to UPPER_CASE naming style\n" +
                "\n" +
                "------------------------------------------------------------------\n" +
                "\n" +
                "Your code has been rated at 5.26/10 (previous run: 5.26/10, +0.00)\n";

        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_WARNINGS, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_CODING_STANDARD, IMarker.SEVERITY_ERROR);
        PydevPrefs.getEclipsePreferences().putInt(PyLintPreferences.SEVERITY_REFACTOR, IMarker.SEVERITY_ERROR);

        pyLintAnalysis.afterRunProcess(output, "No config file found, using default configuration\n", null);

        List<String> markersMessage = new ArrayList<String>();

        for (MarkerInfo marker : pyLintAnalysis.markers) {
            markersMessage.add(marker.message);
        }

        assertEquals("PyLint: bad-whitespace", markersMessage.get(0));
        assertEquals("PyLint: missing-final-newline", markersMessage.get(1));
        assertEquals("PyLint: superfluous-parens", markersMessage.get(2));
        assertEquals("PyLint: missing-docstring", markersMessage.get(3));
        assertEquals("PyLint: invalid-name", markersMessage.get(4));
        assertEquals("PyLint: invalid-name", markersMessage.get(5));
        assertEquals("PyLint: invalid-name", markersMessage.get(6));
        assertEquals("PyLint: invalid-name", markersMessage.get(7));
        assertEquals("PyLint: invalid-name", markersMessage.get(8));
    }

}
