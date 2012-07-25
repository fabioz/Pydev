/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 04/08/2005
 * 
 * @author Fabio Zadrozny
 */
package org.python.pydev.ui.pythonpathconf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.IInterpreterManager;
import org.python.pydev.core.Tuple;
import org.python.pydev.core.docutils.StringUtils;
import org.python.pydev.core.log.Log;
import org.python.pydev.runners.SimpleRunner;

public class JythonInterpreterEditor extends AbstractInterpreterEditor {

    public JythonInterpreterEditor(String labelText, Composite parent, IInterpreterManager interpreterManager) {
        super(IInterpreterManager.JYTHON_INTERPRETER_PATH, labelText, parent, interpreterManager);
    }

    @Override
    public String[] getInterpreterFilterExtensions() {
        return new String[] { "*.jar", "*.*" };
    }

    protected Tuple<String, String> getAutoNewInput() {
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

            return super.getAutoNewInputFromPaths(pathsToSearch, "jython.jar", "jython");

        } catch (CoreException e) {
            Log.log(e);
        }

        return null;
    }

    protected void doFillIntoGrid(Composite parent, int numColumns) {
        super.doFillIntoGrid(parent, numColumns);
        this.autoConfigButton.setToolTipText("Will try to find Jython on the PATH (will fail if not available)");
    }

}