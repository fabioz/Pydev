/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.python.pydev.debug.model.PySetNextTarget;
import org.python.pydev.debug.ui.actions.ISetNextTarget;
import org.python.pydev.editor.PyEdit;

/**
 * @author Hussain Bohra
 */
public class PyEditSetNextAdapterFactory implements IAdapterFactory {

    private static PySetNextTarget pySetNextTarget = new PySetNextTarget();

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
        if (adaptableObject instanceof PyEdit && adapterType == ISetNextTarget.class) {
            return (T) pySetNextTarget;
        }
        return null;
    }

    @Override
    public Class<?>[] getAdapterList() {
        return new Class[] { IRunToLineTarget.class };
    }

}
