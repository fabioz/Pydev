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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentRewriteSession;
import org.eclipse.jface.text.DocumentRewriteSessionType;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.IPyFormatStdProvider;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.SyntaxErrorException;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.organize_imports.ImportArranger;
import org.python.pydev.editor.actions.organize_imports.Pep8ImportArranger;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.prettyprinterv2.IFormatter;
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

            PySelection ps = new PySelection(pyEdit);
            final IDocument doc = ps.getDoc();
            if (ps.getStartLineIndex() == ps.getEndLineIndex()) {
                organizeImports(pyEdit, doc, null, ps);
            } else {
                String endLineDelim = ps.getEndLineDelim();
                DocumentRewriteSession session = startWrite(doc);
                performSimpleSort(doc, endLineDelim, ps.getStartLineIndex(), ps.getEndLineIndex());
                endWrite(doc, session);
            }
        } catch (Exception e) {
            Log.log(e);
            beep(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void organizeImports(PyEdit edit, final IDocument doc, IFile f, PySelection ps) {
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

        IAdaptable projectAdaptable = edit != null ? edit : f;
        String indentStr = edit != null ?
                edit.getIndentPrefs().getIndentationString() :
                DefaultIndentPrefs.get(f).getIndentationString();
        session = startWrite(doc);
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

            boolean pep8 = ImportsPreferencesPage.getPep8Imports(projectAdaptable);

            if (pep8) {
                if (f == null) {
                    f = edit.getIFile();
                }
                IProject p = f != null ? f.getProject() : null;
                pep8PerformArrangeImports(doc, removeUnusedImports, endLineDelim, p, indentStr, automatic, edit);

            } else {
                performArrangeImports(doc, removeUnusedImports, endLineDelim, indentStr, automatic, edit);

            }

            if (participants != null) {
                for (IOrganizeImports organizeImports : participants) {
                    organizeImports.afterPerformArrangeImports(ps, edit);
                }
            }
        } finally {
            if (session != null) {
                endWrite(doc, session);
            }
        }
    }

    /**
     * Stop a rewrite session
     */
    private void endWrite(IDocument doc, DocumentRewriteSession session) {
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            d.stopRewriteSession(session);
        }
    }

    /**
     * Starts a rewrite session (keep things in a single undo/redo)
     */
    private DocumentRewriteSession startWrite(IDocument doc) {
        if (doc instanceof IDocumentExtension4) {
            IDocumentExtension4 d = (IDocumentExtension4) doc;
            return d.startRewriteSession(DocumentRewriteSessionType.UNRESTRICTED);
        }
        return null;
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
     * Performs a simple sort without taking into account the actual contents of the selection (aside from lines
     * ending with '\' which are considered as a single line).
     * 
     * @param doc the document to be sorted
     * @param endLineDelim the delimiter to be used
     * @param startLine the first line where the sort should happen
     * @param endLine the last line where the sort should happen
     */
    public static void performSimpleSort(IDocument doc, String endLineDelim, int startLine, int endLine) {
        try {
            ArrayList<String> list = new ArrayList<String>();

            StringBuffer lastLine = null;
            for (int i = startLine; i <= endLine; i++) {

                String line = PySelection.getLine(doc, i);

                if (lastLine != null) {
                    int len = lastLine.length();
                    if (len > 0 && lastLine.charAt(len - 1) == '\\') {
                        lastLine.append(endLineDelim);
                        lastLine.append(line);
                    } else {
                        list.add(lastLine.toString());
                        lastLine = new StringBuffer(line);
                    }
                } else {
                    lastLine = new StringBuffer(line);
                }
            }

            if (lastLine != null) {
                list.add(lastLine.toString());
            }

            Collections.sort(list);
            StringBuffer all = new StringBuffer();
            for (Iterator<String> iter = list.iterator(); iter.hasNext();) {
                String element = iter.next();
                all.append(element);
                if (iter.hasNext()) {
                    all.append(endLineDelim);
                }
            }

            int length = doc.getLineInformation(endLine).getLength();
            int endOffset = doc.getLineInformation(endLine).getOffset() + length;
            int startOffset = doc.getLineInformation(startLine).getOffset();

            doc.replace(startOffset, endOffset - startOffset, all.toString());

        } catch (BadLocationException e) {
            Log.log(e);
        }

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

    public void formatAll(IDocument doc, IPyFormatStdProvider edit, IFile f, boolean isOpenedFile,
            boolean throwSyntaxError)
            throws SyntaxErrorException {
        organizeImports((PyEdit) edit, doc, f, new PySelection(doc));
    }

    public void formatSelection(IDocument doc, int[] regionsToFormat, IPyFormatStdProvider edit, PySelection ps) {
        throw new UnsupportedOperationException();
    }
}
