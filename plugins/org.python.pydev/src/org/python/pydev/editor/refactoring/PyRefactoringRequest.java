package org.python.pydev.editor.refactoring;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

/**
 * This is a refactoring request which may actually be composed of multiple requests (i.e.: for when we want to 
 * move several resources at once).
 */
public class PyRefactoringRequest implements IPyRefactoringRequest {

    protected final List<RefactoringRequest> requests;
    protected String inputName;

    public PyRefactoringRequest(RefactoringRequest... request) {
        this.requests = Arrays.asList(request);
    }

    @Override
    public List<RefactoringRequest> getRequests() {
        return this.requests;
    }

    @Override
    public void fillInitialNameAndOffset() {
        for (RefactoringRequest r : requests) {
            r.fillInitialNameAndOffset();
        }
    }

    @Override
    public void setInputName(String text) {
        this.inputName = text;
        for (RefactoringRequest r : requests) {
            r.inputName = text;
        }
    }

    @Override
    public void setUpdateReferences(boolean selection) {
        for (RefactoringRequest r : requests) {
            r.setUpdateReferences(selection);
        }
    }

    private final Stack<IProgressMonitor> monitors = new Stack<IProgressMonitor>();

    @Override
    public void pushMonitor(IProgressMonitor pm) {
        if (pm == null) {
            pm = new NullProgressMonitor();
        }
        this.monitors.push(pm);

        for (RefactoringRequest r : requests) {
            r.pushMonitor(pm);
        }
    }

    @Override
    public IProgressMonitor popMonitor() {
        for (RefactoringRequest r : requests) {
            r.popMonitor();
        }
        return this.monitors.pop();
    }

    @Override
    public IProgressMonitor getMonitor() {
        return this.monitors.peek();
    }

    @Override
    public String getInputName() {
        return this.inputName;
    }

    @Override
    public boolean isModuleRenameRefactoringRequest() {
        Boolean ret = null;
        for (RefactoringRequest r : requests) {
            boolean moduleRenameRefactoringRequest = r.isModuleRenameRefactoringRequest();
            if (ret == null) {
                ret = moduleRenameRefactoringRequest;
            } else {
                if (ret != moduleRenameRefactoringRequest) {
                    throw new AssertionError("Error: multiple request types (unexpected condition).");
                }
            }
        }
        if (ret == null) {
            throw new AssertionError("Unable to determine (no requests available)");
        }
        return ret;
    }

    @Override
    public String getInitialName() {
        for (RefactoringRequest r : requests) {
            return r.initialName;
        }
        throw new AssertionError("Unable to get initial name (no requests available)");
    }

}
