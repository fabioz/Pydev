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