/******************************************************************************
* Copyright (C) 2013  Jonah Graham
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.ui.pythonpathconf;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.python.pydev.core.log.Log;
import org.python.pydev.runners.SimpleRunner;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.utils.PlatformUtils;

import at.jta.Key;
import at.jta.Regor;

public class PythonInterpreterProviderFactory extends AbstractInterpreterProviderFactory {

    @Override
    public IInterpreterProvider[] getInterpreterProviders(InterpreterType type) {
        if (type != IInterpreterProviderFactory.InterpreterType.PYTHON) {
            return null;
        }
        List<String> foundVersions = new ArrayList<String>();

        Set<String> pathsToSearch = new LinkedHashSet<String>();
        try {
            Map<String, String> env = SimpleRunner.getDefaultSystemEnv(null);
            if (env.containsKey("PYTHON_HOME")) {
                pathsToSearch.add(env.get("PYTHON_HOME"));
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
        } catch (CoreException e) {
            Log.log(e);
        }
        if (!PlatformUtils.isWindowsPlatform()) {
            // Paths to search on linux/mac
            pathsToSearch.add("/usr/bin");
            pathsToSearch.add("/usr/local/bin");
        }
        if (PlatformUtils.isMacOsPlatform()) {
            // Path to search on mac
            pathsToSearch.add("/Library/Frameworks/Python.framework/Versions/Current/bin");
        }
        // Do this first (i.e.: give priority to the one found first in the path).
        List<String> searchPatterns;
        if (PlatformUtils.isWindowsPlatform()) {
            searchPatterns = Arrays.asList("python.exe", "pypy.exe");

        } else {
            searchPatterns = Arrays.asList("python", "python\\d(\\.\\d)*|pypy");
        }
        final String[] ret = searchPaths(pathsToSearch, searchPatterns, false);
        foundVersions.addAll(Arrays.asList(ret));

        if (PlatformUtils.isWindowsPlatform()) {
            // On windows we can also try to see the installed versions...
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
        }

        if (foundVersions.size() > 0) {
            // Remove duplicates
            foundVersions = new ArrayList<String>(new LinkedHashSet<String>(foundVersions));

            return AlreadyInstalledInterpreterProvider.create("python",
                    foundVersions.toArray(new String[foundVersions.size()]));
        }

        // This should be enough to find it from the PATH or any other way it's
        // defined.
        return AlreadyInstalledInterpreterProvider.create("python", "python");
    }

}
