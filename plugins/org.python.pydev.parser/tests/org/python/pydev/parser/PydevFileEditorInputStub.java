/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class PydevFileEditorInputStub implements IEditorInput {

    @Override
    public boolean exists() {
        return true;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getName() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public IPersistableElement getPersistable() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String getToolTipText() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        throw new RuntimeException("Not implemented");
    }

}
