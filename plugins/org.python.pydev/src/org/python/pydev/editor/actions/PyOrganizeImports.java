/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Copyright (c) 2013 by Syapse, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 18, 2005
 *
 * @author Fabio Zadrozny, Jeremy J. Carroll
 */
package org.python.pydev.editor.actions;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IInterpreterInfo;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.ISystemModulesManager;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.ModulesKey;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.core.pep8.ISortRunner;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PySelectionFromEditor;
import org.python.pydev.editor.actions.organize_imports.ImportArranger;
import org.python.pydev.editor.actions.organize_imports.Pep8ImportArranger;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.shared_core.io.FileUtils;
import org.python.pydev.shared_core.io.PyUnsupportedEncodingException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.utils.DocUtils;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * @author Fabio Zadrozny, Jeremy J. Carroll
 */
public class PyOrganizeImports extends PyAction implements IFormatter {

    private final boolean automatic;

    public PyOrganizeImports() {
        automatic = false;
    }

    public PyOrganizeImports(boolean automatic) {
        this.automatic = automatic;
    }

    /**
     * @see org.eclipse.ui.IActionDelegate#run(org.eclipse.jface.action.IAction)
     */
    @Override
    public void run(IAction action) {
        try {
            if (!canModifyEditor()) {
                return;
            }

            PyEdit pyEdit = getPyEdit();

            PySelection ps = PySelectionFromEditor.createPySelectionFromEditor(pyEdit);
            final IDocument doc = ps.getDoc();
            if (ps.getStartLineIndex() == ps.getEndLineIndex()) {
                organizeImports(pyEdit, doc, null, ps);
            } else {
                DocumentRewriteSession session = TextSelectionUtils.startWrite(doc);
                try {
                    ps.performSimpleSort(doc, ps.getStartLineIndex(), ps.getEndLineIndex());
                } finally {
                    TextSelectionUtils.endWrite(doc, session);
                }
            }
        } catch (Exception e) {
            Log.log(e);
            beep(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void organizeImports(PyEdit edit, final IDocument doc, IFile f, PySelection ps)
            throws MisconfigurationException, PythonNatureWithoutProjectException {
        DocumentRewriteSession session = null;
        String endLineDelim = ps.getEndLineDelim();
        List<IOrganizeImports> participants = null;
        if (f == null && !automatic) {
            // organizing single file ...
            //let's see if someone wants to make a better implementation in another plugin...
            participants = ExtensionHelper.getParticipants(ExtensionHelper.PYDEV_ORGANIZE_IMPORTS);

            for (IOrganizeImports organizeImports : participants) {
                if (!organizeImports.beforePerformArrangeImports(ps, edit, f)) {
                    return;
                }
            }
        }
        String fileContents = doc.get();
        if (fileContents.contains("isort:skip_file") || fileContents.length() == 0) {
            return;
        }
        IAdaptable projectAdaptable = edit != null ? edit : f;
        String indentStr = edit != null ? edit.getIndentPrefs().getIndentationString()
                : DefaultIndentPrefs.get(f).getIndentationString();
        session = TextSelectionUtils.startWrite(doc);
        try {
            //Important: the remove and later update have to be done in the same session (since the remove
            //will just remove some names and he actual perform will remove the remaining if needed).
            //i.e.: from a import b <-- b will be removed by the OrganizeImportsFixesUnused and the
            //from a will be removed in the performArrangeImports later on.
            boolean removeUnusedImports = false;
            if (!automatic) {
                //Only go through the removal of unused imports if it's manually activated (not on automatic mode).
                removeUnusedImports = ImportsPreferencesPage.getDeleteUnusedImports(projectAdaptable);
                if (removeUnusedImports) {
                    new OrganizeImportsFixesUnused().beforePerformArrangeImports(ps, edit, f);
                }
            }

            Set<String> knownThirdParty = new HashSet<String>();
            // isort itself already has a reasonable stdLib, so, don't do our own.

            String importEngine = ImportsPreferencesPage.getImportEngine(projectAdaptable);
            if (edit != null) {
                IPythonNature pythonNature = edit.getPythonNature();
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
                case ImportsPreferencesPage.IMPORT_ENGINE_ISORT:
                    if (fileContents.length() > 0) {
                        File targetFile = edit != null ? edit.getEditorFile() : null;
                        if (targetFile == null) {
                            if (f != null) {
                                IPath location = f.getLocation();
                                if (location != null) {
                                    targetFile = location.toFile();
                                }
                            }
                        }
                        String encoding = null;
                        try {
                            encoding = FileUtils.getPythonFileEncoding(doc, null);
                        } catch (PyUnsupportedEncodingException e) {
                            Log.log(e);
                        }
                        if (encoding == null) {
                            encoding = "utf-8";
                        }

                        Optional<String> executableLocation = ImportsPreferencesPage
                                .getISortExecutable(projectAdaptable);
                        String[] args = ImportsPreferencesPage.getISortArguments(projectAdaptable);

                        String isortResult = ISortRunner.formatWithISort(targetFile, edit.getPythonNature(),
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

                case ImportsPreferencesPage.IMPORT_ENGINE_REGULAR_SORT:
                    performArrangeImports(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit);
                    break;

                default: //case ImportsPreferencesPage.IMPORT_ENGINE_PEP_8:
                    if (f == null) {
                        f = edit.getIFile();
                    }
                    IProject p = f != null ? f.getProject() : null;
                    pep8PerformArrangeImports(doc, removeUnusedImports, endLineDelim, p, indentStr, automatic, edit);
                    break;
            }

            if (participants != null) {
                for (IOrganizeImports organizeImports : participants) {
                    organizeImports.afterPerformArrangeImports(ps, edit);
                }
            }
        } finally {
            TextSelectionUtils.endWrite(doc, session);
        }
    }

    /**
     * Actually does the action in the document. Public for testing.
     *
     * @param doc
     * @param removeUnusedImports
     * @param endLineDelim
     */
    public static void performArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            String indentStr, boolean automatic, IPyFormatStdProvider edit) {
        new ImportArranger(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit).perform();
    }

    /**
     * Pep8 compliant version. Actually does the action in the document.
     *
     * @param doc
     * @param removeUnusedImports
     * @param endLineDelim
     */
    public static void pep8PerformArrangeImports(IDocument doc, boolean removeUnusedImports, String endLineDelim,
            IProject prj, String indentStr, boolean automatic, IPyFormatStdProvider edit) {
        new Pep8ImportArranger(doc, removeUnusedImports, endLineDelim, prj, indentStr, automatic, edit).perform();
    }

    /**
     * Used by legacy tests.
     * @param doc
     * @param endLineDelim
     * @param indentStr
     */
    public static void performArrangeImports(Document doc, String endLineDelim, String indentStr,
            IPyFormatStdProvider edit) {
        performArrangeImports(doc, false, endLineDelim, indentStr, false, edit);
    }

    public static void performPep8ArrangeImports(Document doc, String endLineDelim, String indentStr,
            boolean automatic, IPyFormatStdProvider edit) {
        IProject project = null;
        pep8PerformArrangeImports(doc, false, endLineDelim, project, indentStr, automatic, edit);
    }

    @Override
    public void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile,
            boolean throwSyntaxError)
            throws SyntaxErrorException {
        try {
            organizeImports((PyEdit) edit, doc, f, new PySelection(doc));
        } catch (MisconfigurationException | PythonNatureWithoutProjectException e) {
            Log.log(e); // Can't do it without a configured nature/interpreter.
        }
    }

    @Override
    public void formatSelection(IDocument doc, int[] regionsToFormat, IPyFormatStdProvider edit, PySelection ps) {
        throw new UnsupportedOperationException();
    }
}
