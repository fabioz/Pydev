package com.python.pydev.refactoring.wizards.extract;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyExtractMethodProcessor extends RefactoringProcessor{

    private RefactoringRequest request;

	public PyExtractMethodProcessor (RefactoringRequest request) {
        this.request = request;
    }

	@Override
	public Object[] getElements() {
		return null;
	}

	@Override
	public String getIdentifier() {
		return null;
	}

	@Override
	public String getProcessorName() {
		return null;
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return false;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return null;
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		return null;
	}

}
