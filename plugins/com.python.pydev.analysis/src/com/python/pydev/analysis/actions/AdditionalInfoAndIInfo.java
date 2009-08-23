package com.python.pydev.analysis.actions;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Contains information about some IInfo and its related additional info.
 * 
 * @author Fabio
 */
public class AdditionalInfoAndIInfo{
    
    public final AbstractAdditionalInterpreterInfo additionalInfo;
    public final IInfo info;
    
    public AdditionalInfoAndIInfo(AbstractAdditionalInterpreterInfo additionalInfo, IInfo info) {
        this.additionalInfo = additionalInfo;
        this.info = info;
    }

}
