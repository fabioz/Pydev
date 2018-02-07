package org.python.pydev.debug.quick_assist;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.debug.ui.DebugPrefsPage;
import org.python.pydev.editor.correctionassist.heuristics.IAssistProps;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_ui.proposals.CompletionProposalFactory;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal;

public class QuickAssistDontTrace implements IAssistProps {

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature,
            IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        List<ICompletionProposalHandle> l = new ArrayList<>();
        String cursorLineContents = ps.getCursorLineContents();
        String messageToIgnore = "@DontTrace";
        if (!cursorLineContents.contains(messageToIgnore)) {
            ICompletionProposalHandle proposal = CompletionProposalFactory.get()
                    .createIgnoreCompletionProposalInSameLine(messageToIgnore, ps.getEndLineOffset(), 0, offset,
                            imageCache.get(UIConstants.ASSIST_ANNOTATION), messageToIgnore.substring(1), null, null,
                            IPyCompletionProposal.PRIORITY_DEFAULT, edit, cursorLineContents, ps, null);

            l.add(proposal);
        }
        return l;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return ps.isInFunctionLine(false) && DebugPrefsPage.getDontTraceEnabled();
    }

}
