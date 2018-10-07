/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.mypy;

import org.eclipse.core.resources.IResource;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.external.AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor;

public class OnlyRemoveMarkersMypyVisitor extends AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor {

    public OnlyRemoveMarkersMypyVisitor(IResource resource) {
        super(resource);
    }

    public static final String MYPY_PROBLEM_MARKER = IMiscConstants.MYPY_PROBLEM_MARKER;
    public static final String MYPY_MESSAGE_ID = IMiscConstants.MYPY_MESSAGE_ID;

    @Override
    public String getProblemMarkerId() {
        return MYPY_PROBLEM_MARKER;
    }

    @Override
    public String getMessageId() {
        return MYPY_MESSAGE_ID;
    }

}
