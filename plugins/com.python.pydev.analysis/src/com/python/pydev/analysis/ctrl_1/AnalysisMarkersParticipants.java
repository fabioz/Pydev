/*
 * Created on 24/09/2005
 */
package com.python.pydev.analysis.ctrl_1;


public class AnalysisMarkersParticipants extends AbstractAnalysisMarkersParticipants{


    protected void fillParticipants() {
        participants.add(new IgnoreErrorParticipant());
        participants.add(new UndefinedVariableFixParticipant());
    }


}
