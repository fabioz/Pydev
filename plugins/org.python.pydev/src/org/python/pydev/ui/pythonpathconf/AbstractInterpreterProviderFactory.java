package org.python.pydev.ui.pythonpathconf;

import java.io.File;

import org.python.pydev.shared_core.io.FileUtils;

public abstract class AbstractInterpreterProviderFactory implements IInterpreterProviderFactory {

    public AbstractInterpreterProviderFactory() {
        super();
    }

    public String searchPaths(java.util.List<String> pathsToSearch, String expectedFilename) {
        for (String s : pathsToSearch) {
            if (s.trim().length() > 0) {
                File file = new File(s.trim());
                if (file.isDirectory()) {
                    String[] available = file.list();
                    if (available != null) {
                        for (String jar : available) {
                            if (jar.toLowerCase().equals(expectedFilename)) {
                                return FileUtils.getFileAbsolutePath(new File(file, jar));
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

}