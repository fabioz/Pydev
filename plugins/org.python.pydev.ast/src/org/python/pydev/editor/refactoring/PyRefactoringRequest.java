package org.python.pydev.editor.refactoring;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.python.pydev.core.log.Log;

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

    @Override
    public IFile getIFileResource() {
        IFile f = null;
        for (RefactoringRequest r : requests) {
            if (f == null) {
                f = r.getIFileResource();
            } else {
                IFile f2 = r.getIFileResource();
                if (!f.equals(f2)) {
                    // This is inconsistent
                    return null;
                }
            }
        }
        return f;
    }

    @Override
    public void setSimpleResourceRename(boolean simpleResourceRename) {
        for (RefactoringRequest r : requests) {
            r.setSimpleResourceRename(simpleResourceRename);
        }
    }

    @Override
    public boolean getSimpleResourceRename() {
        try {
            Boolean b = null;
            for (RefactoringRequest r : requests) {
                if (b == null) {
                    b = r.getSimpleResourceRename();
                } else {
                    if (b != r.getSimpleResourceRename()) {
                        // If there's a conflict, we can't do a resource rename
                        // (this makes no sense).
                        return false;
                    }
                }
            }
            if (b == null) {
                return false;
            }
            return b;
        } catch (Exception e) {
            Log.log(e);
            return false;
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
