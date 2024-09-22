package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.python.pydev.ast.refactoring.RefactoringInfo;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.code_completion.ICompletionProposalHandle;
import org.python.pydev.shared_core.structure.Tuple;

public class ExecutePyCreate {

    public static void execute(AbstractPyCreateClassOrMethodOrField action, RefactoringInfo refactoringInfo,
            int locationStrategy) {
        try {
            PySelection pySelection = refactoringInfo.getPySelection();
            Tuple<String, Integer> currToken = pySelection.getCurrToken();
            String actTok = currToken.o1;
            List<String> parametersAfterCall = null;
            if (actTok.length() == 0) {
                actTok = action.getDefaultActTok();
                // String creationStr = this.getCreationStr();
                // final String asTitle = StringUtils.getWithFirstUpper(creationStr);
                // InputDialog dialog = new InputDialog(EditorUtils.getShell(), asTitle + " name",
                //         "Please enter the name of the " + asTitle + " to be created.", "", new IInputValidator() {
                //
                //             @Override
                //             public String isValid(String newText) {
                //                 if (newText.length() == 0) {
                //                     return "The " + asTitle + " name may not be empty";
                //                 }
                //                 if (StringUtils.containsWhitespace(newText)) {
                //                     return "The " + asTitle + " name may not contain whitespaces.";
                //                 }
                //                 return null;
                //             }
                //         });
                // if (dialog.open() != InputDialog.OK) {
                //     return;
                // }
                // actTok = dialog.getValue();
            } else {
                parametersAfterCall = pySelection.getParametersAfterCall(currToken.o2 + actTok.length());

            }

            execute(action, refactoringInfo, actTok, parametersAfterCall, locationStrategy);
        } catch (BadLocationException e) {
            Log.log(e);
        }
    }

    /**
     * When executed it'll create a proposal and apply it.
     */
    public static void execute(AbstractPyCreateClassOrMethodOrField action, RefactoringInfo refactoringInfo,
            String actTok, List<String> parametersAfterCall,
            int locationStrategy) {
        try {
            ICompletionProposalHandle proposal = action.createProposal(refactoringInfo, actTok, locationStrategy,
                    parametersAfterCall);
            if (proposal != null) {
                if (proposal instanceof ICompletionProposalExtension2) {
                    ICompletionProposalExtension2 extension2 = (ICompletionProposalExtension2) proposal;
                    extension2.apply(null, '\n', 0, 0);
                } else {
                    proposal.apply(refactoringInfo.getDocument());
                }
            }

        } catch (Exception e) {
            Log.log(e);
        }
    }

}
