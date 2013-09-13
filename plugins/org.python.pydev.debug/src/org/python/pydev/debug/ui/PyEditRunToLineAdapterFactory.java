/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.ui;

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.python.pydev.debug.model.PyRunToLineTarget;
import org.python.pydev.editor.PyEdit;

public class PyEditRunToLineAdapterFactory implements IAdapterFactory {

    private static PyRunToLineTarget pyRunToLineTarget = new PyRunToLineTarget();

    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adaptableObject instanceof PyEdit && adapterType == IRunToLineTarget.class) {
            return pyRunToLineTarget;

        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { IRunToLineTarget.class };
    }
}