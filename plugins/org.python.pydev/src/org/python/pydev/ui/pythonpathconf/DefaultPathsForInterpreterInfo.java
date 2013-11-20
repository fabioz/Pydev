package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.Set;

import org.eclipse.core.runtime.IPath;

public class DefaultPathsForInterpreterInfo {

    private final Set<IPath> rootPaths;

    public DefaultPathsForInterpreterInfo() {
        rootPaths = InterpreterConfigHelpers.getRootPaths();

    }

    public boolean selectByDefault(String data) {
        return !InterpreterConfigHelpers.isChildOfRootPath(data, rootPaths);
    }

    public boolean exists(String data) {
        return new File(data).exists();
    }
}
