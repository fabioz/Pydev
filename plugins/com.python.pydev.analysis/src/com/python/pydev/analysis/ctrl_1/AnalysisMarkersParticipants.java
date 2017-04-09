package com.python.pydev.analysis.ctrl_1;

import com.python.pydev.analysis.builder.AnalysisRunner;

/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 24/09/2005
 */

public class AnalysisMarkersParticipants extends AbstractAnalysisMarkersParticipants {

    @Override
    protected void fillParticipants() {
        participants.add(new IgnoreErrorParticipant());
        participants.add(new UndefinedVariableFixParticipant());
    }

    @Override
    protected String getMarkerType() {
        return AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER;
    }
}
