/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 19, 2006
 */
package org.python.pydev.editorinput;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.python.pydev.shared_core.string.FastStringBuffer;

public class PyFileLabelProvider implements ILabelProvider {

    private WorkbenchLabelProvider provider;

    public PyFileLabelProvider() {
        provider = new WorkbenchLabelProvider();
    }

    @Override
    public Image getImage(Object element) {
        return provider.getImage(element);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof IFile) {
            IFile f = (IFile) element;
            FastStringBuffer buffer = new FastStringBuffer();
            buffer.append(f.getName());
            buffer.append(" (");
            buffer.append(f.getFullPath().removeFileExtension().removeLastSegments(1).toString());
            buffer.append(")");
            return buffer.toString();
        }
        return provider.getText(element);
    }

    @Override
    public void addListener(ILabelProviderListener listener) {
        provider.addListener(listener);
    }

    @Override
    public void dispose() {
        provider.dispose();
    }

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return provider.isLabelProperty(element, property);
    }

    @Override
    public void removeListener(ILabelProviderListener listener) {
        provider.removeListener(listener);
    }

}
