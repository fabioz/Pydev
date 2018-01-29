package org.python.pydev.shared_ui.proposals;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.FormatStd;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_interactive_console.console.ui.IScriptConsoleViewer;
import org.python.pydev.shared_ui.proposals.IPyCompletionProposal.ICompareContext;

public class CompletionProposalFactory {

    public static CompletionProposalFactory get() {
        return new CompletionProposalFactory();
    }

    public ICompletionProposalHandle createIgnoreCompletionProposalInSameLine(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, IPyEdit edit,
            String line, PySelection ps, FormatStd format) {
        return null;
        //        return new IgnoreCompletionProposalInSameLine(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority, edit, line,
        //                ps, format);
    }

    public ICompletionProposalHandle createIgnoreCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, IPyEdit edit) {
        return null;
        //        return new IgnoreCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
        //                image, displayString, contextInformation, additionalProposalInfo, priority, edit);
    }

    public ICompletionProposalHandle createAssistAssignCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority,
            ISourceViewer sourceViewer, ICompareContext compareContext) {
        return null;
        //        return new AssistAssignCompletionProposal(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority,
        //                sourceViewer, compareContext);
    }

    public ICompletionProposalHandle createOverrideMethodCompletionProposal(int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, /*FunctionDef*/ ISimpleNode functionDef,
            String parentClassName, String currentClassName) {
        return null;
        //        return new OverrideMethodCompletionProposal(replacementOffset, replacementLength, cursorPosition, image,
        //                functionDef, parentClassName, currentClassName);
    }

    public ICompletionProposalHandle createCtxInsensitiveImportComplProposalReparseOnApply(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            int infoTypeForImage, String displayString, IContextInformation contextInformation,
            String additionalProposalInfo, int priority, String realImportRep, ICompareContext compareContext,
            boolean forceReparseOnApply) {
        return null;
        //        return new CtxInsensitiveImportComplProposalReparseOnApply(replacementString, replacementOffset,
        //                replacementLength, cursorPosition, infoTypeForImage, displayString, contextInformation,
        //                additionalProposalInfo, priority, realImportRep, compareContext, forceReparseOnApply);
    }

    public ICompletionProposalHandle createPyConsoleCompletion(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int infoTypeForImage, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, String realImportRep,
            IScriptConsoleViewer viewer, ICompareContext compareContext) {
        return null;
        //        return new PyConsoleCompletion(replacementString, replacementOffset, replacementLength, cursorPosition,
        //                infoTypeForImage, displayString, contextInformation, additionalProposalInfo, priority, realImportRep,
        //                viewer, compareContext);
    }

    public ICompletionProposalHandle createCtxInsensitiveImportComplProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, int infoTypeForImage,
            String displayString, IContextInformation contextInformation, String additionalProposalInfo, int priority,
            String realImportRep, ICompareContext compareContext) {
        return null;
        //        return new CtxInsensitiveImportComplProposal(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, infoTypeForImage, displayString, contextInformation, additionalProposalInfo, priority,
        //                realImportRep, compareContext);
    }

    public ICompletionProposalHandle createPyTemplateProposalForTests(Template template,
            TemplateContext context, IRegion region, Image image, int relevance) {
        return null;
        //        return new PyTemplateProposalForTests(template, context, region, image, relevance);
    }

    public ICompletionProposalHandle createFixCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int lineToRemove) {
        return null;
        //        return new FixCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        //                displayString, contextInformation, additionalProposalInfo, lineToRemove);
    }

    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                priority, compareContext);
    }

    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                priority);
    }

    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, contextInformation, additionalProposalInfo, priority, compareContext);
    }

    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, contextInformation, additionalProposalInfo, priority);
    }

    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, contextInformation, additionalProposalInfo, priority, onApplyAction, args);
    }

    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IToken element, String displayString,
            IContextInformation contextInformation, int priority, int onApplyAction, String args,
            ICompareContext compareContext) {
        return null;
        //        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, element, displayString, contextInformation, priority, onApplyAction, args,
        //                compareContext);
    }

    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, ICompareContext compareContext) {
        return null;
        //        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority,
        //                onApplyAction, args, compareContext);
    }

    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            IContextInformation contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, boolean goToLinkedMode, ICompareContext compareContext) {
        return null;
        //        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
        //                cursorPosition, image, displayString, contextInformation, additionalProposalInfo, priority,
        //                onApplyAction, args, goToLinkedMode, compareContext);
    }

}
