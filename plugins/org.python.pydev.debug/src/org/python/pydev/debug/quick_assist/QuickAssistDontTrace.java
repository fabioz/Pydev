package org.python.pydev.debug.quick_assist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposal;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposalInSameLine;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.proposals.PyCompletionProposal;

public class QuickAssistDontTrace implements IAssistProps {

    @Override
    public List<ICompletionProposal> getProps(PySelection ps, ImageCache imageCache, File f, IPythonNature nature,
            PyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        List<ICompletionProposal> l = new ArrayList<>();
        String cursorLineContents = ps.getCursorLineContents();
        String messageToIgnore = "@DontTrace";
        if (!cursorLineContents.contains(messageToIgnore)) {
            IgnoreCompletionProposal proposal = new IgnoreCompletionProposalInSameLine(messageToIgnore,
                    ps.getEndLineOffset(), 0,
                    offset, //note: the cursor position is unchanged!
                    imageCache.get(UIConstants.ASSIST_ANNOTATION), messageToIgnore.substring(1), null, null,
                    PyCompletionProposal.PRIORITY_DEFAULT, edit, cursorLineContents, ps, null);

            l.add(proposal);
        }
        return l;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, PyEdit edit, int offset) {
        return ps.isInFunctionLine(false) && DebugPrefsPage.getDontTraceEnabled();
    }

}
