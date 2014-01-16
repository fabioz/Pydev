/******************************************************************************
* Copyright (C) 2011-2013  Jin Gong
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jin Gong <xyyhun@gmail.com>    - initial API and implementation
******************************************************************************/

package org.python.pydev.debug.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.IAdaptable;

public class ExecutableDirectoryPropertyTester extends PropertyTester {

    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        IFolder iFolder = null;

        if (receiver instanceof IAdaptable) { //Also handles IWrappedResource
            IAdaptable iAdaptable = (IAdaptable) receiver;
            iFolder = (IFolder) iAdaptable.getAdapter(IFolder.class);

        }

        if (iFolder != null) {
            return (iFolder.getFile("__main__.py").exists());
        }

        return false;
    }
}
