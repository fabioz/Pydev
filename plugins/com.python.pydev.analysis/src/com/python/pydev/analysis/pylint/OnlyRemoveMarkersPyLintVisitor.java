/**
 * Copyright (c) 2017 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.pylint;

import org.eclipse.core.resources.IResource;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.external.AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor;

public class OnlyRemoveMarkersPyLintVisitor extends AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor {

    public OnlyRemoveMarkersPyLintVisitor(IResource resource) {
        super(resource);
    }

    public static final String PYLINT_PROBLEM_MARKER = IMiscConstants.PYLINT_PROBLEM_MARKER;

    public static final String PYLINT_MESSAGE_ID = IMiscConstants.PYLINT_MESSAGE_ID;

    @Override
    protected String getProblemMarkerId() {
        return PYLINT_PROBLEM_MARKER;
    }

}
