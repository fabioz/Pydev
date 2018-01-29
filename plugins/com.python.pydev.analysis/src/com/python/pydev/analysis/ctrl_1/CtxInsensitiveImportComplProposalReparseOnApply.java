package com.python.pydev.analysis.ctrl_1;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.SWT;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.structure.Tuple;

import com.python.pydev.analysis.CtxInsensitiveImportComplProposal;

class CtxInsensitiveImportComplProposalReparseOnApply extends CtxInsensitiveImportComplProposal {

    private boolean forceReparseOnApply;

    public CtxInsensitiveImportComplProposalReparseOnApply(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int infoTypeForImage, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority,
            String realImportRep, ICompareContext compareContext, boolean forceReparseOnApply) {
        super(replacementString, replacementOffset, replacementLength, cursorPosition, infoTypeForImage,
                displayString, contextInformation, additionalProposalInfo, priority, realImportRep, compareContext);
        this.forceReparseOnApply = forceReparseOnApply;
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
        //Overridden to do nothing (i.e.: don't leave yellow when ctrl is pressed).
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        if ((stateMask & SWT.SHIFT) != 0) {
            this.setAddLocalImport(true);
        }
        super.apply(viewer, trigger, stateMask, offset);
        if (forceReparseOnApply) {
            //and after applying it, let's request a reanalysis
            if (viewer instanceof PySourceViewer) {
                PySourceViewer sourceViewer = (PySourceViewer) viewer;
                PyEdit edit = sourceViewer.getEdit();
                if (edit != null) {
                    edit.getParser().forceReparse(
                            new Tuple<String, Boolean>(IMiscConstants.ANALYSIS_PARSER_OBSERVER_FORCE,
                                    true));
                }
            }
        }
    }
}