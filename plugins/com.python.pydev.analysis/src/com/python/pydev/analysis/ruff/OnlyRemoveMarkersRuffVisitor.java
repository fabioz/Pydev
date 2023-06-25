/**
 * Copyright (c) 2018 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ruff;

import org.eclipse.core.resources.IResource;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.external.AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor;

public class OnlyRemoveMarkersRuffVisitor extends AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor {

    public OnlyRemoveMarkersRuffVisitor(IResource resource) {
        super(resource);
    }

    public static final String RUFF_PROBLEM_MARKER = IMiscConstants.RUFF_PROBLEM_MARKER;
    public static final String RUFF_MESSAGE_ID = IMiscConstants.RUFF_MESSAGE_ID;

    @Override
    public String getProblemMarkerId() {
        return RUFF_PROBLEM_MARKER;
    }

    @Override
    public String getMessageId() {
        return RUFF_MESSAGE_ID;
    }

}
