package com.python.pydev.refactoring.wizards.extract;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.CompositeChange;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RefactoringParticipant;
import org.eclipse.ltk.core.refactoring.participants.RefactoringProcessor;
import org.eclipse.ltk.core.refactoring.participants.SharableParticipants;
import org.python.pydev.editor.refactoring.RefactoringRequest;

public class PyExtractMethodProcessor extends RefactoringProcessor{

    private RefactoringRequest request;
	private CompositeChange fChange;

	public PyExtractMethodProcessor (RefactoringRequest request) {
        this.request = request;
    }

	@Override
	public Object[] getElements() {
        return new Object[] { this.request };
	}

    public static final String IDENTIFIER = "org.python.pydev.pyExtractMethod";
    
	@Override
	public String getIdentifier() {
		return IDENTIFIER;
	}

	@Override
	public String getProcessorName() {
		return "Pydev PyExtractMethodProcessor";
	}

	@Override
	public boolean isApplicable() throws CoreException {
		return true;
	}

	@Override
	public RefactoringStatus checkInitialConditions(IProgressMonitor pm) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
		return status;
	}

	@Override
	public RefactoringStatus checkFinalConditions(IProgressMonitor pm, CheckConditionsContext context) throws CoreException, OperationCanceledException {
        RefactoringStatus status = new RefactoringStatus();
        fChange = new CompositeChange("RenameChange: "+request.duringProcessInfo.name);
		return status;
	}

	@Override
	public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
		return fChange; 
	}

	@Override
	public RefactoringParticipant[] loadParticipants(RefactoringStatus status, SharableParticipants sharedParticipants) throws CoreException {
		return null; // no participants are loaded
	}

}
