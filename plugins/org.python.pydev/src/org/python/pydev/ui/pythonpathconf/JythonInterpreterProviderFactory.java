package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.runners.SimpleRunner;

public class JythonInterpreterProviderFactory extends AbstractInterpreterProviderFactory {

    public IInterpreterProvider[] getInterpreterProviders(InterpreterType type) {
        if (type != IInterpreterProviderFactory.InterpreterType.JYTHON) {
            return null;
        }

        try {
            Map<String, String> env = SimpleRunner.getDefaultSystemEnv(null);
            List<String> pathsToSearch = new ArrayList<String>();
            if (env.containsKey("JYTHON_HOME")) {
                pathsToSearch.add(env.get("JYTHON_HOME"));
            }
            if (env.containsKey("PYTHON_HOME")) {
                pathsToSearch.add(env.get("PYTHON_HOME"));
            }
            if (env.containsKey("JYTHONHOME")) {
                pathsToSearch.add(env.get("JYTHONHOME"));
            }
            if (env.containsKey("PYTHONHOME")) {
                pathsToSearch.add(env.get("PYTHONHOME"));
            }
            if (env.containsKey("PATH")) {
                String path = env.get("PATH");
                String separator = SimpleRunner.getPythonPathSeparator();
                final List<String> split = StringUtils.split(path, separator);
                pathsToSearch.addAll(split);
            }
            pathsToSearch.add("/usr/bin");
            pathsToSearch.add("/usr/local/bin");
            pathsToSearch.add("/usr/share/java");

            String searchResult = searchPaths(pathsToSearch, "jython.jar");
            if (searchResult != null) {
                return AlreadyInstalledInterpreterProvider.create("jython", searchResult);
            }
        } catch (CoreException e) {
            Log.log(e);
        }

        return null;
    }

}
