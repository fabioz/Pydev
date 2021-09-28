/**
 * Copyright (c) 2021 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.flake8;

import org.eclipse.core.resources.IResource;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.external.AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor;

public class OnlyRemoveMarkersFlake8Visitor extends AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor {

    public OnlyRemoveMarkersFlake8Visitor(IResource resource) {
        super(resource);
    }

    public static final String FLAKE8_PROBLEM_MARKER = IMiscConstants.FLAKE8_PROBLEM_MARKER;
    public static final String FLAKE8_MESSAGE_ID = IMiscConstants.FLAKE8_MESSAGE_ID;

    @Override
    public String getProblemMarkerId() {
        return FLAKE8_PROBLEM_MARKER;
    }

    @Override
    public String getMessageId() {
        return FLAKE8_MESSAGE_ID;
    }

}
