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
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

public class PyLintAnalysisTest extends TestCase {

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

        List<Tuple<Integer, String>> markersMessageInfo = new ArrayList<Tuple<Integer, String>>();

        for (MarkerInfo marker : pyLintAnalysis.markers) {
            markersMessageInfo.add(new Tuple<Integer, String>(marker.lineStart,
                    (String) marker.additionalInfo.get(PyLintVisitor.PYLINT_MESSAGE_ID)));

            // checks if it is getting marker region right (single line matches)
            assertEquals(marker.lineStart, marker.lineEnd);
        }

        assertEquals(new Tuple<Integer, String>(13, "bad-whitespace"), markersMessageInfo.get(0));
        assertEquals(new Tuple<Integer, String>(21, "missing-final-newline"), markersMessageInfo.get(1));
        assertEquals(new Tuple<Integer, String>(21, "superfluous-parens"), markersMessageInfo.get(2));
        assertEquals(new Tuple<Integer, String>(0, "missing-docstring"), markersMessageInfo.get(3));
        assertEquals(new Tuple<Integer, String>(2, "invalid-name"), markersMessageInfo.get(4));
        assertEquals(new Tuple<Integer, String>(3, "invalid-name"), markersMessageInfo.get(5));
        assertEquals(new Tuple<Integer, String>(4, "invalid-name"), markersMessageInfo.get(6));
        assertEquals(new Tuple<Integer, String>(5, "invalid-name"), markersMessageInfo.get(7));
        assertEquals(new Tuple<Integer, String>(6, "invalid-name"), markersMessageInfo.get(8));
    }

}
