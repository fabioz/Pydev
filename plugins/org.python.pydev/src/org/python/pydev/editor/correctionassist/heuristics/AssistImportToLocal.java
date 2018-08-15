package org.python.pydev.editor.correctionassist.heuristics;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.core.IPyEdit;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.docutils.ImportHandle;
import org.python.pydev.core.docutils.ImportHandle.ImportHandleInfo;
import org.python.pydev.core.docutils.PyImportsIterator;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.proposals.CompletionProposalFactory;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.editor.actions.refactoring.PyRefactorAction;
import org.python.pydev.editor.correctionassist.IAssistProps;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.image.IImageCache;
import org.python.pydev.shared_core.image.UIConstants;
import org.python.pydev.shared_core.structure.Tuple;

public class AssistImportToLocal implements IAssistProps {

    @Override
    public List<ICompletionProposalHandle> getProps(PySelection ps, IImageCache imageCache, File f,
            IPythonNature nature, IPyEdit edit, int offset) throws BadLocationException, MisconfigurationException {
        boolean addOnlyGlobalImports = true;
        Tuple<String, Integer> currToken = ps.getCurrToken();
        List<ICompletionProposalHandle> ret = new ArrayList<ICompletionProposalHandle>();

        if (currToken.o1 != null && currToken.o1.length() > 0) {
            PyImportsIterator pyImportsIterator = new PyImportsIterator(ps.getDoc(), addOnlyGlobalImports);
            OUT: while (pyImportsIterator.hasNext()) {
                ImportHandle handle = pyImportsIterator.next();
                List<ImportHandleInfo> importInfo = handle.getImportInfo();
                for (ImportHandleInfo importHandleInfo : importInfo) {
                    List<String> importedStr = importHandleInfo.getImportedStr();
                    int startLine = importHandleInfo.getStartLine();
                    int endLine = importHandleInfo.getEndLine();
                    if (ps.getLineOfOffset() > startLine) {
                        continue;
                    }
                    if (ps.getLineOfOffset() < endLine) {
                        break OUT; // Stop iterating.
                    }
                    for (String s : importedStr) {
                        if (s.equals(currToken.o1)) {
                            // Found!
                            IProgressMonitor monitor = new NullProgressMonitor();
                            final RefactoringRequest req = PyRefactorAction.createRefactoringRequest(monitor,
                                    (PyEdit) edit, ps);
                            req.setAdditionalInfo(RefactoringRequest.FIND_DEFINITION_IN_ADDITIONAL_INFO, false);
                            req.setAdditionalInfo(RefactoringRequest.FIND_REFERENCES_ONLY_IN_LOCAL_SCOPE, true);
                            req.fillInitialNameAndOffset();

                            ret.add(CompletionProposalFactory.get().createMoveImportsToLocalCompletionProposal(
                                    req,
                                    s,
                                    importHandleInfo,
                                    imageCache.get(UIConstants.ASSIST_MOVE_IMPORT),
                                    "Move import to local scope(s)"));

                        }
                    }
                }
            }
        }

        return ret;
    }

    @Override
    public boolean isValid(PySelection ps, String sel, IPyEdit edit, int offset) {
        return true;
    }

}
