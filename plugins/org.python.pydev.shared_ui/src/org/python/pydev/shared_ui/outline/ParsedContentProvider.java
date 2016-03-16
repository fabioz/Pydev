/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created: Jul 25, 2003
 */
package org.python.pydev.shared_ui.outline;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

/**
 * Trivial: A ContentProvider interface ParsedItem tree items
 * 
 */
public class ParsedContentProvider implements ITreeContentProvider {

    @Override
    public Object[] getElements(Object inputElement) {
        return ((IParsedItem) inputElement).getChildren();
    }

    @Override
    public void dispose() {
    }

    @Override
    public Object[] getChildren(Object parentElement) {
        return ((IParsedItem) parentElement).getChildren();
    }

    @Override
    public Object getParent(Object element) {
        return ((IParsedItem) element).getParent();
    }

    @Override
    public boolean hasChildren(Object element) {
        return (((IParsedItem) element).getChildren().length > 0);
    }

    @Override
    public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
    }
}
