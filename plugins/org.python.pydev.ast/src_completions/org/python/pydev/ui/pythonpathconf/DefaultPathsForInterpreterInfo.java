/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
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
