/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import com.python.pydev.analysis.additionalinfo.AbstractAdditionalTokensInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Contains information about some IInfo and its related additional info.
 * 
 * @author Fabio
 */
public class AdditionalInfoAndIInfo {

    public final AbstractAdditionalTokensInfo additionalInfo;
    public final IInfo info;

    public AdditionalInfoAndIInfo(AbstractAdditionalTokensInfo additionalInfo, IInfo info) {
        this.additionalInfo = additionalInfo;
        this.info = info;
    }

    @Override
    public int hashCode() {
        return this.info.hashCode() + this.additionalInfo.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AdditionalInfoAndIInfo)) {
            return false;
        }
        AdditionalInfoAndIInfo other = (AdditionalInfoAndIInfo) obj;
        return this.info.equals(other.info) && this.additionalInfo.equals(other.additionalInfo);
    }

}
