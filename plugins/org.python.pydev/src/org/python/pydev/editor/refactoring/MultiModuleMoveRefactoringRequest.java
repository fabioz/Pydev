package org.python.pydev.editor.refactoring;

import java.io.File;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.core.log.Log;
import org.python.pydev.plugin.nature.PythonNature;
import org.python.pydev.shared_core.string.StringUtils;

public class MultiModuleMoveRefactoringRequest extends PyRefactoringRequest {

    private String initialName;
    private IContainer target;

    public MultiModuleMoveRefactoringRequest(List<ModuleRenameRefactoringRequest> requests, IContainer target)
            throws MisconfigurationException, TargetNotInPythonpathException {
        super(requests.toArray(new RefactoringRequest[requests.size()]));
        PythonNature nature = PythonNature.getPythonNature(target);
        File file = target.getLocation().toFile();
        this.target = target;
        this.initialName = nature.resolveModule(file);
        IPath fullPath = target.getFullPath();
        if (this.initialName == null) {
            //Check if it's a source folder...
            try {
                Set<String> projectSourcePathSet = nature.getPythonPathNature().getProjectSourcePathSet(true);
                for (String string : projectSourcePathSet) {
                    if (new Path(string).equals(fullPath)) {
                        this.initialName = "";
                        break;
                    }
                }
            } catch (CoreException e) {
                Log.log(e);
            }
        }
        if (this.initialName == null) {
            throw new TargetNotInPythonpathException("Unable to resolve file as a python module: " + fullPath);
        }
    }

    @Override
    public void setInputName(String text) {
        Assert.isNotNull(text, "Not expecting input name to be null.");
        this.inputName = text;

        Path initialPath = new Path(StringUtils.replaceAll(this.initialName, ".", "/"));
        Path finalPath = new Path(StringUtils.replaceAll(text, ".", "/"));

        IContainer initialContainer = target;
        IContainer finalContainer;
        if (initialPath.equals(finalPath)) {
            finalContainer = initialContainer;

        } else if (initialPath.isPrefixOf(finalPath)) {
            IPath walk = finalPath.removeFirstSegments(initialPath.segmentCount());
            finalContainer = initialContainer.getFolder(walk);

        } else {
            IPath walk = finalPath.removeFirstSegments(initialPath.segmentCount());
            finalContainer = initialContainer;
            for (int i = 0; i < walk.segmentCount(); i--) {
                finalContainer = finalContainer.getParent();
            }

        }

        for (RefactoringRequest r : requests) {
            ModuleRenameRefactoringRequest modReq = (ModuleRenameRefactoringRequest) r;
            modReq.setTarget(finalContainer);

            String lastPart = FullRepIterable.getLastPart(r.initialName);
            if (text.length() > 0) {
                r.inputName = text + "." + lastPart;
            } else {
                r.inputName = lastPart;
            }
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
