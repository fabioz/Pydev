package org.python.pydev.editor.codecompletion.proposals;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.swt.widgets.Display;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.ICompletionRequest;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IToken;
import org.python.pydev.core.ShellId;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.docutils.PySelection.DocstringInfo;
import org.python.pydev.core.formatter.FormatStd;
import org.python.pydev.core.interactive_console.IScriptConsoleViewer;
import org.python.pydev.core.proposals.ICompletionProposalFactory;
import org.python.pydev.editor.codecompletion.PyTemplateProposal;
import org.python.pydev.editor.codefolding.PyCalltipsContextInformationFromIToken;
import org.python.pydev.editor.correctionassist.FixCompletionProposal;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposal;
import org.python.pydev.editor.correctionassist.IgnoreCompletionProposalInSameLine;
import org.python.pydev.editor.correctionassist.IgnoreFlake8CompletionProposalInSameLine;
import org.python.pydev.editor.correctionassist.IgnorePyLintCompletionProposalInSameLine;
import org.python.pydev.editor.correctionassist.docstrings.AssistDocstringCompletionProposal;
import org.python.pydev.editor.correctionassist.heuristics.AssistAssignCompletionProposal;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.code_completion.IPyCompletionProposal.ICompareContext;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_ui.ImageCache;

public class DefaultCompletionProposalFactory implements ICompletionProposalFactory {

    @Override
    public ICompletionProposalHandle createAssistDocstringCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext, String initial, String delimiter, String docStringMarker,
            String delimiterAndIndent, String preferredDocstringStyle2, boolean inFunctionLine,
            DocstringInfo finalDocstringFromFunction, String indentation, FastStringBuffer buf, List<String> params) {
        return new AssistDocstringCompletionProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority,
                compareContext, initial, delimiter, docStringMarker, delimiterAndIndent, preferredDocstringStyle2,
                inFunctionLine, finalDocstringFromFunction, indentation, buf, params);
    }

    @Override
    public ICompletionProposalHandle createIgnorePyLintCompletionProposalInSameLine(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            IImageHandle image, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, IPyEdit edit, String line, PySelection ps, FormatStd format,
            IMarker marker) {
        return new IgnorePyLintCompletionProposalInSameLine(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority, edit, line,
                ps, format, marker);
    }

    @Override
    public ICompletionProposalHandle createIgnoreFlake8CompletionProposalInSameLine(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            IImageHandle image, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, IPyEdit edit, String line, PySelection ps, FormatStd format,
            IMarker marker) {
        return new IgnoreFlake8CompletionProposalInSameLine(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority, edit, line,
                ps, format, marker);
    }

    @Override
    public ICompletionProposalHandle createPyTemplateProposal(Template template, TemplateContext context,
            IRegion region, IImageHandle image, int relevance) {
        return new PyTemplateProposal(template, context, region, ImageCache.asImage(image), relevance);
    }

    @Override
    public ICompletionProposalHandle createIgnoreCompletionProposalInSameLine(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, IPyEdit edit,
            String line, PySelection ps, FormatStd format) {
        return new IgnoreCompletionProposalInSameLine(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority, edit, line,
                ps, format);
    }

    @Override
    public ICompletionProposalHandle createIgnoreCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, IPyEdit edit) {
        return new IgnoreCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                image, displayString, (IContextInformation) contextInformation, additionalProposalInfo, priority, edit);
    }

    @Override
    public ICompletionProposalHandle createAssistAssignCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            Object sourceViewer, ICompareContext compareContext) {
        return new AssistAssignCompletionProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority,
                (ISourceViewer) sourceViewer, compareContext);
    }

    @Override
    public ICompletionProposalHandle createOverrideMethodCompletionProposal(ICompletionRequest request, PySelection ps,
            int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, /*FunctionDef*/ ISimpleNode functionDef,
            String parentClassName, String currentClassName) {
        return new OverrideMethodCompletionProposal(replacementOffset, replacementLength, cursorPosition, image,
                functionDef, parentClassName, currentClassName);
    }

    @Override
    public ICompletionProposalHandle createCtxInsensitiveImportComplProposalReparseOnApply(
            String replacementString, int replacementOffset, int replacementLength, int cursorPosition,
            int infoTypeForImage, String displayString, Object contextInformation,
            String additionalProposalInfo, int priority, String realImportRep, ICompareContext compareContext,
            boolean forceReparseOnApply) {
        return new CtxInsensitiveImportComplProposalReparseOnApply(replacementString, replacementOffset,
                replacementLength, cursorPosition, infoTypeForImage, displayString,
                (IContextInformation) contextInformation,
                additionalProposalInfo, priority, realImportRep, compareContext, forceReparseOnApply);
    }

    @Override
    public ICompletionProposalHandle createPyConsoleCompletion(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int infoTypeForImage, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, String realImportRep,
            IScriptConsoleViewer viewer, ICompareContext compareContext) {
        return new PyConsoleCompletion(replacementString, replacementOffset, replacementLength, cursorPosition,
                infoTypeForImage, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority, realImportRep,
                viewer, compareContext);
    }

    @Override
    public ICompletionProposalHandle createCtxInsensitiveImportComplProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, int infoTypeForImage,
            String displayString, Object contextInformation, String additionalProposalInfo, int priority,
            String realImportRep, ICompareContext compareContext) {
        return new CtxInsensitiveImportComplProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, infoTypeForImage, displayString, (IContextInformation) contextInformation,
                additionalProposalInfo, priority,
                realImportRep, compareContext);
    }

    @Override
    public ICompletionProposalHandle createPyTemplateProposalForTests(Template template,
            TemplateContext context, IRegion region, IImageHandle image, int relevance) {
        return new PyTemplateProposalForTests(template, context, region, ImageCache.asImage(image), relevance);
    }

    @Override
    public ICompletionProposalHandle createFixCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int lineToRemove) {
        return new FixCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, (IContextInformation) contextInformation, additionalProposalInfo, lineToRemove);
    }

    @Override
    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                priority, compareContext);
    }

    @Override
    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                priority);
    }

    @Override
    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority,
            ICompareContext compareContext) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, (IContextInformation) contextInformation, additionalProposalInfo, priority,
                compareContext);
    }

    @Override
    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, (IContextInformation) contextInformation, additionalProposalInfo, priority);
    }

    @Override
    public ICompletionProposalHandle createPyCompletionProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args) {
        return new PyCompletionProposal(replacementString, replacementOffset, replacementLength, cursorPosition, image,
                displayString, (IContextInformation) contextInformation, additionalProposalInfo, priority,
                onApplyAction, args);
    }

    @Override
    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IToken element, String displayString,
            Object contextInformation, int priority, int onApplyAction, String args,
            ICompareContext compareContext) {
        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, element, displayString, (IContextInformation) contextInformation, priority,
                onApplyAction, args,
                compareContext);
    }

    @Override
    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, ICompareContext compareContext) {
        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority,
                onApplyAction, args, compareContext);
    }

    @Override
    public ICompletionProposalHandle createPyLinkedModeCompletionProposal(String replacementString,
            int replacementOffset, int replacementLength, int cursorPosition, IImageHandle image, String displayString,
            Object contextInformation, String additionalProposalInfo, int priority, int onApplyAction,
            String args, boolean goToLinkedMode, ICompareContext compareContext) {
        return new PyLinkedModeCompletionProposal(replacementString, replacementOffset, replacementLength,
                cursorPosition, image, displayString, (IContextInformation) contextInformation, additionalProposalInfo,
                priority,
                onApplyAction, args, goToLinkedMode, compareContext);
    }

    @Override
    public ICompletionProposalHandle createSimpleAssistProposal(String replacementString, int replacementOffset,
            int replacementLength, int cursorPosition, int priority, ICompareContext compareContext) {
        return new SimpleAssistProposal(replacementString, replacementOffset, replacementLength, cursorPosition,
                priority, compareContext);
    }

    @Override
    public Object createPyCalltipsContextInformationFromIToken(IToken element, String args,
            int contextInformationOffset) {
        return new PyCalltipsContextInformationFromIToken(element, args, contextInformationOffset);
    }

    @Override
    public ShellId getShellId() {
        return Display.getCurrent() != null ? ShellId.MAIN_THREAD_SHELL
                : ShellId.OTHER_THREADS_SHELL;
    }

    @Override
    public ShellId getCythonShellId() {
        return Display.getCurrent() != null ? ShellId.CYTHON_MAIN_THREAD_SHELL
                : ShellId.CYTHON_OTHER_THREADS_SHELL;
    }

    @Override
    public ICompletionProposalHandle createMoveImportsToLocalCompletionProposal(Object refactoringRequest,
            String importedToken, ImportHandleInfo importHandleInfo, IImageHandle iImageHandle, String displayString) {
        return new PyMoveImportsToLocalCompletionProposal((RefactoringRequest) refactoringRequest, importedToken,
                importHandleInfo, iImageHandle, displayString);
    }

}
