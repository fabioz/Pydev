/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.filters;

import org.eclipse.jface.viewers.Viewer;

public class PyoFilter extends AbstractFilter {

    @Override
    public boolean select(Viewer viewer, Object parentElement, Object element) {
        String name = getName(element);
        if (name != null && name.endsWith(".pyo")) {
            return false;
        }

        return true;
    }

}
