/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 12, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.actions.PyAction;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.CompletionProposalFactory;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;
import org.python.pydev.ui.importsconf.ImportsPreferencesPage;

/**
 * @author Fabio Zadrozny
 */
public class AssistImport implements IAssistProps {

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#getProps(org.python.pydev.core.docutils.PySelection, org.python.pydev.shared_ui.ImageCache)
     */
    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException {
        ArrayList<ICompletionProposalHandle> l = new ArrayList<>();
        String sel = PyAction.getLineWithoutComments(ps).trim();

        int i = sel.indexOf("import");
        if (ps.getStartLineIndex() != ps.getEndLineIndex()) {
            return l;
        }

        String delimiter = PyAction.getDelimiter(ps.getDoc());
        boolean isFuture = PySelection.isFutureImportLine(sel);

        int lineToMoveImport = ps.getLineAvailableForImport(isFuture);

        try {
            int lineToMoveOffset = ps.getDoc().getLineOffset(lineToMoveImport);

            if (i >= 0) {
                l.add(CompletionProposalFactory.get().createFixCompletionProposal(sel + delimiter, lineToMoveOffset, 0,
                        ps.getStartLine().getOffset(),
                        imageCache
                                .get(UIConstants.ASSIST_MOVE_IMPORT),
                        "Move import to global scope", null, null, ps
                                .getStartLineIndex() + 1));
            }
        } catch (BadLocationException e) {
            //Ignore
        }

        if (i >= 0) {
            String cursorLineContents = ps.getCursorLineContents();
            String importEngine = ImportsPreferencesPage.getImportEngine(edit);
            String messageToIgnore = "@NoMove";
            String caption = messageToIgnore.substring(1);
            if (ImportsPreferencesPage.IMPORT_ENGINE_ISORT.equals(importEngine)) {
                caption = messageToIgnore = "isort:skip";
            }
            if (!cursorLineContents.contains(messageToIgnore)) {
                ICompletionProposalHandle proposal = CompletionProposalFactory.get()
                        .createIgnoreCompletionProposalInSameLine(messageToIgnore, ps.getEndLineOffset(), 0, offset,
                                imageCache.get(UIConstants.ASSIST_ANNOTATION), caption, null, null,
                                IPyCompletionProposal.PRIORITY_DEFAULT, edit, cursorLineContents, ps, null);

                l.add(proposal);
            }
        }
        return l;
    }

    /**
     * @see org.python.pydev.editor.correctionassist.heuristics.IAssistProps#isValid(org.python.pydev.core.docutils.PySelection)
     */
    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return sel.indexOf("import ") != -1;
    }

}
