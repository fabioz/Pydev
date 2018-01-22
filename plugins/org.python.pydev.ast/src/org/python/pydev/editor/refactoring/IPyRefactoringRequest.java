package org.python.pydev.editor.refactoring;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;

public interface IPyRefactoringRequest {

    List<RefactoringRequest> getRequests();

    void fillInitialNameAndOffset();

    void setInputName(String text);

    void pushMonitor(IProgressMonitor pm);

    IProgressMonitor popMonitor();

    IProgressMonitor getMonitor();

    String getInputName();

    String getInitialName();

    boolean isModuleRenameRefactoringRequest();

    // The ones below are available when isModuleRenameRefactoringRequest() is true.

    IFile getIFileResource();

    void setUpdateReferences(boolean selection);

    void setSimpleResourceRename(boolean resourceRenameBool);

    boolean getSimpleResourceRename();
}
