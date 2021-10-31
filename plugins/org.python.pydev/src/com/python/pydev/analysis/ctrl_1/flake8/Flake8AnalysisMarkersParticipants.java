/**
 * Copyright (c) 2021 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.ctrl_1.flake8;

import org.python.pydev.shared_core.IMiscConstants;

import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;

public class Flake8AnalysisMarkersParticipants extends AbstractAnalysisMarkersParticipants {

    @Override
    protected void fillParticipants() {
        participants.add(new Flake8IgnoreErrorParticipant());
    }

    @Override
    protected String getMarkerType() {
        return IMiscConstants.FLAKE8_PROBLEM_MARKER;
    }
}
