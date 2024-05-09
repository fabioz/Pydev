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
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.python.pydev.ast.sort_imports.SortImports;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.PythonNatureWithoutProjectException;
import org.python.pydev.core.autoedit.DefaultIndentPrefs;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.imports.ImportPreferences;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PySelectionFromEditor;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.plugin.PyDevUiPrefs;
import org.python.pydev.shared_core.SharedCorePlugin;
import org.python.pydev.shared_core.string.TextSelectionUtils;

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

        DocumentRewriteSession session = null;
        session = TextSelectionUtils.startWrite(doc);
        try {
            //Important: the remove and later update have to be done in the same session (since the remove
            //will just remove some names and he actual perform will remove the remaining if needed).
            //i.e.: from a import b <-- b will be removed by the OrganizeImportsFixesUnused and the
            //from a will be removed in the performArrangeImports later on.
            boolean removeUnusedImports = false;
            if (!automatic) {
                //Only go through the removal of unused imports if it's manually activated (not on automatic mode).
                removeUnusedImports = ImportPreferences.getDeleteUnusedImports(projectAdaptable);
                if (removeUnusedImports) {
                    new OrganizeImportsFixesUnused().beforePerformArrangeImports(ps, edit, f);
                }
            }

            IPythonNature pythonNature = null;
            if (edit != null) {
                pythonNature = edit.getPythonNature();
            }

            File targetFile = edit != null ? edit.getEditorFile() : null;
            if (targetFile == null) {
                if (f != null) {
                    IPath location = f.getLocation();
                    if (location != null) {
                        targetFile = location.toFile();
                    }
                }
            }

            SortImports sortImports = new SortImports();
            int maxCols = getMaxCols(ImportPreferences.getMultilineImports(edit));

            sortImports.sortImports(pythonNature, projectAdaptable, targetFile,
                    fileContents, doc, removeUnusedImports, endLineDelim,
                    indentStr, automatic, edit, maxCols);

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
     * @return the maximum number of columns that may be available in a line.
     */
    private static int getMaxCols(boolean multilineImports) {
        final int maxCols;
        if (multilineImports) {
            if (SharedCorePlugin.inTestMode()) {
                maxCols = 80;
            } else {
                IPreferenceStore chainedPrefStore = PyDevUiPrefs.getChainedPrefStore();
                maxCols = chainedPrefStore
                        .getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_PRINT_MARGIN_COLUMN);
            }
        } else {
            maxCols = Integer.MAX_VALUE;
        }
        return maxCols;
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
