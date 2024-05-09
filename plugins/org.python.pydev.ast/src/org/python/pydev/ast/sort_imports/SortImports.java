package org.python.pydev.ast.sort_imports;

import java.io.File;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.imports.ImportPreferences;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.pep8.ISortRunner;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.PyUnsupportedEncodingException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.DocUtils;

public class SortImports {

    public void sortImports(IPythonNature pythonNature, IAdaptable projectAdaptable, File targetFile,
            String fileContents, final IDocument doc, boolean removeUnusedImports, String endLineDelim,
            String indentStr, boolean automatic, IPyFormatStdProvider edit, int maxCols)
            throws MisconfigurationException, PythonNatureWithoutProjectException {

        Set<String> knownThirdParty = new HashSet<String>();
        // isort itself already has a reasonable stdLib, so, don't do our own.

        String importEngine = ImportPreferences.getImportEngine(projectAdaptable);
        if (!ImportPreferences.IMPORT_ENGINE_ISORT.equals(importEngine)) {
            if (pythonNature != null) {
                IInterpreterInfo projectInterpreter = pythonNature.getProjectInterpreter();
                ISystemModulesManager modulesManager = projectInterpreter.getModulesManager();
                ModulesKey[] onlyDirectModules = modulesManager.getOnlyDirectModules();

                Set<String> stdLib = new HashSet<>();

                for (ModulesKey modulesKey : onlyDirectModules) {
                    if (modulesKey.file == null) {
                        int i = modulesKey.name.indexOf('.');
                        String name;
                        if (i < 0) {
                            name = modulesKey.name;
                        } else {
                            name = modulesKey.name.substring(0, i);
                        }
                        // Add all names to std lib
                        stdLib.add(name);
                    }
                }
                for (ModulesKey modulesKey : onlyDirectModules) {
                    int i = modulesKey.name.indexOf('.');
                    String name;
                    if (i < 0) {
                        name = modulesKey.name;
                    } else {
                        name = modulesKey.name.substring(0, i);
                    }

                    // Consider all in site-packages to be third party.
                    if (modulesKey.file != null && modulesKey.file.toString().contains("site-packages")) {
                        stdLib.remove(name);
                        knownThirdParty.add(name);
                    }
                }
            }
        }

        switch (importEngine) {
            case ImportPreferences.IMPORT_ENGINE_ISORT:
                if (fileContents.length() > 0) {
                    String encoding = null;
                    try {
                        encoding = FileUtils.getPythonFileEncoding(doc, null);
                    } catch (PyUnsupportedEncodingException e) {
                        Log.log(e);
                    }
                    if (encoding == null) {
                        encoding = "utf-8";
                    }

                    Optional<String> executableLocation = ImportPreferences
                            .getISortExecutable(projectAdaptable);
                    String[] args = ImportPreferences.getISortArguments(projectAdaptable);

                    String isortResult = ISortRunner.formatWithISort(targetFile, pythonNature,
                            fileContents, encoding, targetFile.getParentFile(), args, knownThirdParty,
                            executableLocation);

                    if (isortResult != null) {
                        String delimiter = PySelection.getDelimiter(doc);
                        if (!delimiter.equals(System.lineSeparator())) {
                            // Argh, isort seems to not keep line delimiters, so, hack around it.
                            isortResult = StringUtils.replaceAll(isortResult, System.lineSeparator(), delimiter);
                        }
                        try {
                            DocUtils.updateDocRangeWithContents(doc, fileContents, isortResult.toString());
                        } catch (Exception e) {
                            Log.log(
                                    StringUtils.format(
                                            "Error trying to apply isort result. Curr doc:\n>>>%s\n<<<.\nNew doc:\\n>>>%s\\n<<<.",
                                            fileContents, isortResult.toString()),
                                    e);
                        }
                    }
                }
                break;

            case ImportPreferences.IMPORT_ENGINE_REGULAR_SORT:
                performArrangeImports(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit, maxCols);
                break;

            default: //case ImportsPreferencesPage.IMPORT_ENGINE_PEP_8:
                pep8PerformArrangeImports(doc, removeUnusedImports, endLineDelim, pythonNature, indentStr, automatic,
                        edit, maxCols);
                break;
        }
    }

    public static void performPep8ArrangeImports(Document doc, String endLineDelim, String indentStr,
            boolean automatic, IPyFormatStdProvider edit, IPythonNature nature, int maxCols) {
        pep8PerformArrangeImports(doc, false, endLineDelim, nature, indentStr, automatic, edit, maxCols);
    }

    /**
     * Used by legacy tests.
     * @param doc
     * @param endLineDelim
     * @param indentStr
     */
    public static void performArrangeImports(Document doc, String endLineDelim, String indentStr,
            IPyFormatStdProvider edit, int maxCols) {
        performArrangeImports(doc, false, endLineDelim, indentStr, false, edit, maxCols);
    }

    /**
     * Pep8 compliant version. Actually does the action in the document.
     *
     * @param doc
     * @param removeUnusedImports
     * @param endLineDelim
     */
    public static void pep8PerformArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            IPythonNature nature, String indentStr, boolean automatic, IPyFormatStdProvider edit, int maxCols) {
        new Pep8ImportArranger(doc, removeUnusedImports, endLineDelim, nature, indentStr, automatic, edit, maxCols)
                .perform();
    }

    /**
     * Actually does the action in the document. Public for testing.
     *
     * @param doc
     * @param removeUnusedImports
     * @param endLineDelim
     */
    public static void performArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            String indentStr, boolean automatic, IPyFormatStdProvider edit, int maxCols) {
        new ImportArranger(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit, maxCols).perform();
    }
}
