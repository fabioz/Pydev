package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.utils.PlatformUtils;

import at.jta.Key;
import at.jta.Regor;

public class PythonInterpreterProviderFactory extends AbstractInterpreterProviderFactory {

    public IInterpreterProvider[] getInterpreterProviders(InterpreterType type) {
        if (type != IInterpreterProviderFactory.InterpreterType.PYTHON) {
            return null;
        }

        List<String> pathsToSearch = new ArrayList<String>();
        if (!PlatformUtils.isWindowsPlatform()) {
            pathsToSearch.add("/usr/bin");
            pathsToSearch.add("/usr/local/bin");
            final String ret = searchPaths(pathsToSearch, "python");
            if (ret != null) {
                return AlreadyInstalledInterpreterProvider.create("python", ret);
            }
        } else {
            // On windows we can try to see the installed versions...
            List<String> foundVersions = new ArrayList<String>();
            try {
                Regor regor = new Regor();

                // The structure for Python is something as
                // Software\\Python\\PythonCore\\2.6\\InstallPath
                for (Key root : new Key[] { Regor.HKEY_LOCAL_MACHINE, Regor.HKEY_CURRENT_USER }) {
                    Key key = regor.openKey(root, "Software\\Python\\PythonCore", Regor.KEY_READ);
                    if (key != null) {
                        try {
                            @SuppressWarnings("rawtypes")
                            List l = regor.listKeys(key);
                            for (Object o : l) {
                                Key openKey = regor.openKey(key, (String) o + "\\InstallPath", Regor.KEY_READ);
                                if (openKey != null) {
                                    try {
                                        byte buf[] = regor.readValue(openKey, "");
                                        if (buf != null) {
                                            String parseValue = Regor.parseValue(buf);
                                            // Ok, this should be the directory
                                            // where it's installed, try to find
                                            // a 'python.exe' there...
                                            File file = new File(parseValue, "python.exe");
                                            if (file.isFile()) {
                                                foundVersions.add(file.toString());
                                            }
                                        }
                                    } finally {
                                        regor.closeKey(openKey);
                                    }
                                }
                            }
                        } finally {
                            regor.closeKey(key);
                        }
                    }
                }

            } catch (Throwable e) {
                Log.log(e);
            }
            if (foundVersions.size() > 0) {
                return AlreadyInstalledInterpreterProvider.create("python",
                        foundVersions.toArray(new String[foundVersions.size()]));
            }
        }

        // This should be enough to find it from the PATH or any other way it's
        // defined.
        return AlreadyInstalledInterpreterProvider.create("python", "python");
    }

}
