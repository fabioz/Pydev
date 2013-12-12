package org.python.pydev.editor.refactoring;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IPyRefactoringRequest {

    List<RefactoringRequest> getRequests();

    void fillInitialNameAndOffset();

    void setInputName(String text);

    void pushMonitor(IProgressMonitor pm);

    IProgressMonitor popMonitor();

    IProgressMonitor getMonitor();

    String getInputName();

    boolean isModuleRenameRefactoringRequest();

    String getInitialName();

}
