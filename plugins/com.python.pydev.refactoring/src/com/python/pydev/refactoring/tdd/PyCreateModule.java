package com.python.pydev.refactoring.tdd;

import java.util.List;

import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.python.pydev.refactoring.core.base.RefactoringInfo;

public class PyCreateModule extends PyCreateAction{

    @Override
    public void execute(RefactoringInfo refactoringInfo, int locationStrategyBeforeCurrent) {
        System.out.println("Execute");
    }

    private void apply() {
        // TODO Auto-generated method stub

    }
    @Override
    public ICompletionProposal createProposal(RefactoringInfo refactoringInfo, String actTok, int locationStrategy,
            List<String> parametersAfterCall) {
        System.out.println("Create proposal.");
        return new CompletionProposal("prop", 0, 0, 0);
    }

}
