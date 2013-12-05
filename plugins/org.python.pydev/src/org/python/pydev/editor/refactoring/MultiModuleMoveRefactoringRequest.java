package org.python.pydev.editor.refactoring;

import java.io.File;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.plugin.nature.PythonNature;

public class MultiModuleMoveRefactoringRequest extends PyRefactoringRequest {

    private final String initialName;
    private IContainer target;

    public MultiModuleMoveRefactoringRequest(List<ModuleRenameRefactoringRequest> requests, IContainer target)
            throws MisconfigurationException {
        super(requests.toArray(new RefactoringRequest[requests.size()]));
        PythonNature nature = PythonNature.getPythonNature(target);
        File file = target.getLocation().toFile();
        this.target = target;
        this.initialName = nature.resolveModule(file);
        Assert.isNotNull(this.initialName, "Unable to resolve file as a python module: " + target.getFullPath());
    }

    @Override
    public void setInputName(String text) {
        this.inputName = text;
        for (RefactoringRequest r : requests) {
            String lastPart = FullRepIterable.getLastPart(r.initialName);
            r.inputName = text + "." + lastPart;
        }
    }

    @Override
    public String getInitialName() {
        return this.initialName;
    }

    public IContainer getTarget() {
        return this.target;
    }

}
