package org.python.pydev.core.proposals;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ShellId;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocstringInfo;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal.ICompareContext;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.FastStringBuffer;

public interface ICompletionProposalFactory {

    ICompletionProposalHandle createAssistDocstringCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext, String initial, String delimiter, String docStringMarker,
            String delimiterAndIndent, String preferredDocstringStyle2, boolean inFunctionLine,
            DocstringInfo finalDocstringFromFunction, String indentation, FastStringBuffer buf, List<String> params);

    ICompletionProposalHandle createIgnorePyLintCompletionProposalInSameLine(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            IImageHandle image, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, IPyEdit edit, String line, PySelection ps, FormatStd format,
            IMarker marker);

    ICompletionProposalHandle createIgnoreFlake8CompletionProposalInSameLine(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            IImageHandle image, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, IPyEdit edit, String line, PySelection ps, FormatStd format,
            IMarker marker);

    ICompletionProposalHandle createPyTemplateProposal(Template template, TemplateContext context,
            IRegion region, IImageHandle image, int relevance);

    ICompletionProposalHandle createIgnoreCompletionProposalInSameLine(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, IPyEdit edit,
            String line, PySelection ps, FormatStd format);

    ICompletionProposalHandle createIgnoreCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, IPyEdit edit);

    ICompletionProposalHandle createAssistAssignCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            /*ISourceViewer*/ Object sourceViewer, ICompareContext compareContext);

    ICompletionProposalHandle createOverrideMethodCompletionProposal(ICompletionRequest request,
            PySelection ps, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, /*FunctionDef*/ ISimpleNode functionDef,
            String parentClassName, String currentClassName);

    ICompletionProposalHandle createCtxInsensitiveImportComplProposalReparseOnApply(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            int infoTypeForImage, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, String realImportRep, ICompareContext compareContext,
            boolean forceReparseOnApply);

    ICompletionProposalHandle createPyConsoleCompletion(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int infoTypeForImage, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, String realImportRep,
            IScriptConsoleViewer viewer, ICompareContext compareContext);

    ICompletionProposalHandle createCtxInsensitiveImportComplProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, int infoTypeForImage,
            String displayString, Object contextInformation, String additionalProposalInfo, int priority,
            String realImportRep, ICompareContext compareContext);

    ICompletionProposalHandle createPyTemplateProposalForTests(Template template,
            TemplateContext context, IRegion region, IImageHandle image, int relevance);

    ICompletionProposalHandle createFixCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int lineToRemove);

    ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext);

    ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority);

    ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext);

    ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority);

    ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args);

    ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IToken element, String displayString,
            Object contextInformation, int priority, int onApplyAction, String args,
            ICompareContext compareContext);

    ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, ICompareContext compareContext);

    ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, boolean goToLinkedMode, ICompareContext compareContext);

    ICompletionProposalHandle createSimpleAssistProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext);

    /* IContextInformation */ Object createPyCalltipsContextInformationFromIToken(IToken element, String args,
            int contextInformationOffset);

    ShellId getShellId();

    ICompletionProposalHandle createMoveImportsToLocalCompletionProposal(
            /*RefactoringRequest*/ Object refactoringRequest, String importedToken, ImportHandleInfo importHandleInfo,
            IImageHandle iImageHandle, String displayString);

    ShellId getCythonShellId();

}