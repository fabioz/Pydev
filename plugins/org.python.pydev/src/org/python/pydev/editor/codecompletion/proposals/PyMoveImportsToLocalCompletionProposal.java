package org.python.pydev.editor.codecompletion.proposals;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.ReplaceEdit;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.ast.refactoring.IPyRefactoring2;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.codefolding.PySourceViewer;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.IMiscConstants;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageHandle;
import org.python.pydev.shared_core.string.TextSelectionUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_ui.ImageCache;

import com.python.pydev.analysis.refactoring.quick_fixes.AddTokenAndImportStatement;

public class PyMoveImportsToLocalCompletionProposal
        implements ICompletionProposal, ICompletionProposalExtension2, ICompletionProposalHandle {

    private String displayString;
    private IImageHandle iImageHandle;
    private RefactoringRequest refactoringRequest;
    private String fReplacementString;
    private boolean appliedWithTrigger;
    private int importLen;
    private ImportHandleInfo importHandleInfo;
    private String importedToken;
    private boolean forceReparseOnApply = true;

    public PyMoveImportsToLocalCompletionProposal(RefactoringRequest refactoringRequest,
            String importedToken, ImportHandleInfo importHandleInfo, IImageHandle iImageHandle,
            String displayString) {
        this.importedToken = importedToken;
        this.displayString = displayString;
        this.importHandleInfo = importHandleInfo;
        this.iImageHandle = iImageHandle;
        this.refactoringRequest = refactoringRequest;
    }

    @Override
    public String getAdditionalProposalInfo() {
        return null;
    }

    @Override
    public String getDisplayString() {
        return displayString;
    }

    @Override
    public void apply(IDocument doc) {
        RefactoringRequest req = refactoringRequest;
        final IPyRefactoring2 r = (IPyRefactoring2) AbstractPyRefactoring.getPyRefactoring();
        if (req.initialName != null && req.initialName.trim().length() > 0) {
            Map<Tuple<String, File>, HashSet<ASTEntry>> occurrences;
            try {
                occurrences = r.findAllOccurrences(req);
                Set<Entry<Tuple<String, File>, HashSet<ASTEntry>>> entrySet = occurrences
                        .entrySet();

                Set<Integer> appliedOffsets = new HashSet<Integer>();
                for (Map.Entry<Tuple<String, File>, HashSet<ASTEntry>> o : entrySet) {
                    HashSet<ASTEntry> entries = o.getValue();
                    ASTEntry[] ordered = entries.toArray(new ASTEntry[0]);
                    Arrays.sort(ordered, (entry0, entry1) -> {
                        // Note: order is reversed.
                        return Integer.compare(entry1.node.beginLine, entry0.node.beginLine);
                    });
                    for (ASTEntry entry : entries) {
                        if (entry.node != null) {
                            int beginLine = entry.node.beginLine;
                            IDocument document = req.getDoc();

                            int useLine = beginLine - 1;

                            if (useLine >= importHandleInfo.getStartLine()
                                    && useLine <= importHandleInfo.getEndLine()) {
                                // Skip the import itself.
                                continue;
                            }

                            String currLine = TextSelectionUtils.getLine(document, useLine);
                            if (!currLine.isEmpty() && !Character.isWhitespace(currLine.charAt(0))) {
                                continue; // Skip global occurrences of the token
                            }

                            for (int i = useLine; i < document.getNumberOfLines(); i++) {
                                String line = TextSelectionUtils.getLine(document, i);
                                if (!line.trim().isEmpty()) {
                                    if (Character.isWhitespace(line.charAt(0))) {
                                        useLine = i;
                                        break;
                                    }
                                }
                            }

                            boolean addLocalImport = true;
                            boolean addLocalImportsOnTopOfMethod = true;
                            boolean groupImports = false;
                            int offset = new PySelection(req.ps.getDoc(), useLine, 0)
                                    .getAbsoluteCursorOffset();
                            int maxCols = 200;
                            char trigger = ' ';

                            String fromImportStr = importHandleInfo.getFromImportStr();
                            String realImportRep;
                            if (fromImportStr == null || fromImportStr.isEmpty()) {
                                realImportRep = "import " + this.importedToken;
                            } else {
                                realImportRep = "from " + fromImportStr + " import " + this.importedToken;
                            }
                            int fReplacementOffset = offset;
                            int fLen = 0;
                            String indentString = "               ";

                            this.fReplacementString = "";
                            AddTokenAndImportStatement.ComputedInfo computedInfo = new AddTokenAndImportStatement.ComputedInfo(
                                    realImportRep, fReplacementOffset, fLen, indentString,
                                    fReplacementString, appliedWithTrigger, importLen, document);
                            this.appliedWithTrigger = computedInfo.appliedWithTrigger;
                            this.importLen = computedInfo.importLen;

                            new AddTokenAndImportStatement(document, trigger, offset, addLocalImport,
                                    addLocalImportsOnTopOfMethod, groupImports, maxCols)
                                            .createTextEdit(computedInfo);
                            for (ReplaceEdit edit : computedInfo.replaceEdit) {
                                if (!appliedOffsets.contains(edit.getOffset())) {
                                    appliedOffsets.add(edit.getOffset());
                                    try {
                                        edit.apply(document);
                                    } catch (Exception e) {
                                        Log.log(e);
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (OperationCanceledException | CoreException e) {
                Log.log(e);
            }
        }
    }

    @Override
    public Point getSelection(IDocument document) {
        return new Point(this.refactoringRequest.ps.getAbsoluteCursorOffset(), 0);
    }

    @Override
    public Image getImage() {
        return ImageCache.asImage(iImageHandle);
    }

    @Override
    public IContextInformation getContextInformation() {
        return null;
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
        IDocument doc = viewer.getDocument();
        apply(doc);
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

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {

    }

    @Override
    public void unselected(ITextViewer viewer) {

    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
        return false;
    }
}
