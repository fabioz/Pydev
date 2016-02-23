/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.tree;

import java.io.File;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.python.pydev.core.log.Log;


public class FileTreeContentProvider implements ITreeContentProvider {
    @Override
    public Object[] getChildren(Object element) {
        Object[] kids = ((File) element).listFiles();
        return kids == null ? new Object[0] : kids;
    }

    @Override
    public Object[] getElements(Object element) {
        return getChildren(element);
    }

    @Override
    public boolean hasChildren(Object element) {
        return getChildren(element).length > 0;
    }

    @Override
    public Object getParent(Object element) {
        if (element == null) {
            return null;
        }

        if (element instanceof File) {
            return ((File) element).getParent();
        } else if (element instanceof String) {
            return new File((String) element).getParent();
        }
        Log.log(("element not instance of File of String: " + element.getClass().getName() + " " + element.toString()));
        return null;
    }

    @Override
    public void dispose() {
    }

    @Override
    public void inputChanged(Viewer viewer, Object old_input, Object new_input) {
    }
}