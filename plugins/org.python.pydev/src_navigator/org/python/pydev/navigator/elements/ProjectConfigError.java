/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.navigator.elements;

import org.eclipse.core.resources.IProject;

public class ProjectConfigError implements ISortedElement {

    private final IProject project;
    private final String label;

    public ProjectConfigError(IProject project, String label) {
        this.project = project;
        this.label = label;
    }

    public IProject getParent() {
        return this.project;
    }

    @Override
    public int getRank() {
        return ISortedElement.RANK_ERROR;
    }

    public String getLabel() {
        return label;
    }

}
