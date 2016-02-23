/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com>    - initial API and implementation
*     Roland Grunberg <rgrunber@redhat.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.debug.ui;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.python.pydev.editor.codecompletion.revisited.PythonPathHelper;
import org.python.pydev.navigator.elements.IWrappedResource;

public class PythonTypePropertyTester extends PropertyTester {

    @Override
    public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

        IFile iFile = null;

        if (receiver instanceof IWrappedResource) {
            IWrappedResource wrappedResource = (IWrappedResource) receiver;
            Object actualObject = wrappedResource.getActualObject();
            if (actualObject instanceof IProject) {
                return true;
            } else if (actualObject instanceof IFile) {
                iFile = (IFile) actualObject;
            }
        }
        if (receiver instanceof IAdaptable) {
            IAdaptable iAdaptable = (IAdaptable) receiver;
            iFile = (IFile) iAdaptable.getAdapter(IFile.class);
        }

        if (iFile != null) {
            if (PythonPathHelper.markAsPyDevFileIfDetected(iFile)) {
                return true;
            }
        }
        return false;
    }

}
