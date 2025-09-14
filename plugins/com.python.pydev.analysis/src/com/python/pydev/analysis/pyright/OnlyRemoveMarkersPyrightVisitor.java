/**
 * Copyright (c) 2025 Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.pyright;

import org.eclipse.core.resources.IResource;
import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.external.AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor;

public class OnlyRemoveMarkersPyrightVisitor extends AbstractExternalCodeAnalysisOnlyRemoveMarkersVisitor {

    public OnlyRemoveMarkersPyrightVisitor(IResource resource) {
        super(resource);
    }

    public static final String PYRIGHT_PROBLEM_MARKER = IMiscConstants.PYRIGHT_PROBLEM_MARKER;
    public static final String PYRIGHT_MESSAGE_ID = IMiscConstants.PYRIGHT_MESSAGE_ID;

    @Override
    public String getProblemMarkerId() {
        return PYRIGHT_PROBLEM_MARKER;
    }

    @Override
    public String getMessageId() {
        return PYRIGHT_MESSAGE_ID;
    }

}
