/**
 * Copyright (c) 2017 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ctrl_1.pylint;

import org.python.pydev.core.IMiscConstants;

import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;

public class PyLintAnalysisMarkersParticipants extends AbstractAnalysisMarkersParticipants {

    @Override
    protected void fillParticipants() {
        participants.add(new PyLintIgnoreErrorParticipant());
    }

    @Override
    protected String getMarkerType() {
        return IMiscConstants.PYLINT_PROBLEM_MARKER;
    }
}
