package com.python.pydev.refactoring.tdd;

import com.python.pydev.analysis.additionalinfo.builders.AnalysisRunner;
import com.python.pydev.analysis.ctrl_1.AbstractAnalysisMarkersParticipants;
import com.python.pydev.analysis.marker_quick_fixes.TddQuickFixFromMarkersParticipant;

public class TddCodeGenerationQuickFixFromMarkersParticipant extends AbstractAnalysisMarkersParticipants {

    private TddQuickFixFromMarkersParticipant tddQuickFixParticipant;

    @Override
    protected void fillParticipants() {
        tddQuickFixParticipant = new TddQuickFixFromMarkersParticipant();
        participants.add(tddQuickFixParticipant);
    }

    @Override
    protected String getMarkerType() {
        return AnalysisRunner.PYDEV_ANALYSIS_PROBLEM_MARKER;
    }

}
