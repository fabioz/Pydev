package org.python.pydev.ui.refactoring;

import java.io.IOException;
import java.util.List;

import org.eclipse.ltk.core.refactoring.participants.RenameRefactoring;
import org.eclipse.ltk.ui.refactoring.RefactoringWizardOpenOperation;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.item_pointer.ItemPointer;
import org.python.pydev.ast.refactoring.AbstractPyRefactoring;
import org.python.pydev.ast.refactoring.IPyRefactoring;
import org.python.pydev.ast.refactoring.IPyRefactoringRequest;
import org.python.pydev.ast.refactoring.ModuleRenameRefactoringRequest;
import org.python.pydev.ast.refactoring.MultiModuleMoveRefactoringRequest;
import org.python.pydev.ast.refactoring.PyRefactoringRequest;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_ui.EditorUtils;

import com.python.pydev.refactoring.wizards.RefactorProcessFactory;
import com.python.pydev.refactoring.wizards.rename.PyRenameEntryPoint;

public class PyRenameRefactoring {

    public static String rename(IPyRefactoringRequest request) {
        try {
            List<RefactoringRequest> actualRequests = request.getRequests();
            if (actualRequests.size() == 1) {
                RefactoringRequest req = actualRequests.get(0);

                //Note: if it's already a ModuleRenameRefactoringRequest, no need to change anything.
                if (!(req.isModuleRenameRefactoringRequest())) {

                    //Note: if we're renaming an import, we must change to the appropriate req
                    IPyRefactoring pyRefactoring = AbstractPyRefactoring.getPyRefactoring();
                    ItemPointer[] pointers = pyRefactoring.findDefinition(req);
                    for (ItemPointer pointer : pointers) {
                        Definition definition = pointer.definition;
                        if (RefactorProcessFactory.isModuleRename(definition)) {
                            try {
                                request = new PyRefactoringRequest(new ModuleRenameRefactoringRequest(
                                        definition.module.getFile(), req.nature, null));
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                }
            }

            PyRenameEntryPoint entryPoint = new PyRenameEntryPoint(request);
            RenameRefactoring renameRefactoring = new RenameRefactoring(entryPoint);
            request.fillInitialNameAndOffset();

            String title = "Rename";
            if (request instanceof MultiModuleMoveRefactoringRequest) {
                MultiModuleMoveRefactoringRequest multiModuleMoveRefactoringRequest = (MultiModuleMoveRefactoringRequest) request;
                title = "Move To package (project: "
                        + multiModuleMoveRefactoringRequest.getTarget().getProject().getName()
                        + ")";
            }
            final PyRenameRefactoringWizard wizard = new PyRenameRefactoringWizard(renameRefactoring, title,
                    "inputPageDescription", request);
            try {
                RefactoringWizardOpenOperation op = new RefactoringWizardOpenOperation(wizard);
                op.run(EditorUtils.getShell(), "Rename Refactor Action");
            } catch (InterruptedException e) {
                // do nothing. User action got cancelled
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return null;
    }

}
