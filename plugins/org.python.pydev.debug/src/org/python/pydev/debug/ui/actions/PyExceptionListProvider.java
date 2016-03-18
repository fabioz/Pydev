/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui.actions;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.debug.model.PyExceptionBreakPointManager;

public class PyExceptionListProvider implements IStructuredContentProvider {

    private Object newInput;
    private Object[] elementsForCurrentInput;
    private static final String[] EMPTY = new String[0];

    public PyExceptionListProvider() {

    }

    @Override
    public Object[] getElements(Object inputElement) {

        if (this.newInput == null) {
            this.inputChanged(null, null, inputElement);
        }
        return elementsForCurrentInput == null ? EMPTY : elementsForCurrentInput;
    }

    @Override
    public void dispose() {
        elementsForCurrentInput = null;
        newInput = null;
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
        if (newInput == this.newInput) {
            return;
        }

        // let go of the old info before getting the new info
        dispose();
        this.newInput = newInput;

        if (newInput == null) {
            return;
        }

        PyExceptionBreakPointManager instance = PyExceptionBreakPointManager.getInstance();
        List<String> list = instance.getBuiltinExceptions();
        list.addAll(instance.getUserConfiguredExceptions());

        elementsForCurrentInput = list.toArray(new String[0]);
    }

    public void addUserConfiguredException(String userConfiguredException) {
        PyExceptionBreakPointManager.getInstance().addUserConfiguredException(userConfiguredException);
        this.newInput = null;
    }
}
