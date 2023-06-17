/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Feb 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.actions;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.python.pydev.ast.formatter.PyFormatter;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.formatter.PyFormatterPreferences;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.PySelectionFromEditor;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
import org.python.pydev.shared_core.io.FileUtils;

/**
 * @author Fabio Zadrozny
 */
public class PyFormatAction extends PyAction implements IFormatter {

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

            try {
                int[] regionsToFormat = null;
                if (ps.getSelLength() > 0) {
                    int startLineIndex = ps.getStartLineIndex();
                    int endLineIndex = ps.getEndLineIndex();
                    regionsToFormat = new int[endLineIndex - startLineIndex + 1];
                    for (int i = startLineIndex, j = 0; i <= endLineIndex; i++, j++) {
                        regionsToFormat[j] = i;
                    }
                } else {
                    //For full-formatting, we cannot have a syntax error.
                    if (pyEdit.hasSyntaxError(ps.getDoc())) {
                        return;
                    }
                }

                applyFormatAction(pyEdit, ps, regionsToFormat, true, pyEdit.getSelectionProvider());
            } catch (SyntaxErrorException e) {
                pyEdit.getStatusLineManager().setErrorMessage(e.getMessage());
            }

        } catch (Exception e) {
            beep(e);
        }
    }

    /**
     * This method applies the code-formatting to the document in the PySelection
     *
     * @param pyEdit used to restore the selection
     * @param ps the selection used (contains the document that'll be changed)
     * @param regionsToFormat if null or empty, the whole document will be formatted, otherwise, only the passed ranges will
     * be formatted.
     * @throws SyntaxErrorException
     */
    public void applyFormatAction(IPyFormatStdProvider pyEdit, PySelection ps, int[] regionsToFormat,
            boolean throwSyntaxError,
            ISelectionProvider selectionProvider)
            throws BadLocationException, SyntaxErrorException {
        final IFormatter participant = getFormatter();
        final IDocument doc = ps.getDoc();
        final SelectionKeeper selectionKeeper = new SelectionKeeper(ps);

        DocumentRewriteSession session = null;
        try {

            if (regionsToFormat == null || regionsToFormat.length == 0) {
                if (doc instanceof IDocumentExtension4) {
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.STRICTLY_SEQUENTIAL);
                }
                participant.formatAll(doc, pyEdit, null, true, throwSyntaxError);

            } else {
                if (doc instanceof IDocumentExtension4) {
                    IDocumentExtension4 ext = (IDocumentExtension4) doc;
                    session = ext.startRewriteSession(DocumentRewriteSessionType.SEQUENTIAL);
                }
                participant.formatSelection(doc, regionsToFormat, pyEdit, ps);
            }

        } finally {
            if (session != null) {
                ((IDocumentExtension4) doc).stopRewriteSession(session);
            }
        }
        if (selectionProvider != null) {
            selectionKeeper.restoreSelection(selectionProvider, doc);
        }

    }

    /**
     * @return the source code formatter to be used.
     */
    public IFormatter getFormatter() {
        IFormatter participant = (IFormatter) ExtensionHelper.getParticipant(ExtensionHelper.PYDEV_FORMATTER, false);
        if (participant == null) {
            participant = this;
        }
        return participant;
    }

    @Override
    public void formatSelection(IDocument doc, int[] regionsForSave, IPyFormatStdProvider edit, PySelection ps) {
        FormatStd formatStd = PyFormatterPreferences.getFormatStd(edit);
        PyFormatter.formatSelection(doc, regionsForSave, edit, ps, formatStd);
    }

    /**
     * Formats the whole document
     * @throws SyntaxErrorException
     * @see IFormatter
     */
    @Override
    public void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile,
            boolean throwSyntaxError)
            throws SyntaxErrorException {
        //        Formatter formatter = new Formatter();
        //        formatter.formatAll(doc, edit);

        FormatStd formatStd = (FormatStd) (edit != null ? edit.getFormatStd() : PyFormatterPreferences.getFormatStd(f));
        String filepath = null;
        if (edit != null) {
            File editorFile = edit.getEditorFile();
            if (editorFile != null) {
                filepath = FileUtils.getFileAbsolutePath(editorFile);
            }
        }
        if (filepath == null && f != null) {
            IPath path = f.getLocation().makeAbsolute();
            filepath = path.toOSString();
        }
        PyFormatter.formatAll(filepath, doc, edit, isOpenedFile, formatStd, throwSyntaxError, true);

    }

}
