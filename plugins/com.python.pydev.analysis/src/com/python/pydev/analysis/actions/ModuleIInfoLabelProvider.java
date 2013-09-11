/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.log.Log;
import org.python.pydev.shared_core.string.FastStringBuffer;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.AdditionalProjectInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.AdditionalSystemInterpreterInfo;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Provides the labels and images for the bottom panel
 *
 * @author Fabio
 */
public final class ModuleIInfoLabelProvider extends LabelProvider {

    @Override
    public String getText(Object element) {
        if (element instanceof AdditionalInfoAndIInfo) {
            AdditionalInfoAndIInfo additional = (AdditionalInfoAndIInfo) element;
            element = additional.info;
            String suffix = null;
            try {
                if (additional.additionalInfo instanceof AdditionalProjectInterpreterInfo) {
                    AdditionalProjectInterpreterInfo projectInterpreterInfo = (AdditionalProjectInterpreterInfo) additional.additionalInfo;
                    suffix = projectInterpreterInfo.getProject().getName();

                } else if (additional.additionalInfo instanceof AdditionalSystemInterpreterInfo) {
                    AdditionalSystemInterpreterInfo systemInterpreterInfo = (AdditionalSystemInterpreterInfo) additional.additionalInfo;
                    suffix = systemInterpreterInfo.getManager().getDefaultInterpreterInfo(false).getName();

                }
            } catch (Throwable e) {
                Log.log(e);
            }

            String iInfoText = getIInfoText((IInfo) element, suffix);
            return iInfoText;
        }
        if (element instanceof String) {
            return (String) element;
        }
        return getIInfoText((IInfo) element, null);
    }

    private String getIInfoText(IInfo info, String suffix) {
        String path = info.getPath();
        int pathLen;
        if (path != null && (pathLen = path.length()) > 0) {
            int suffixLen = suffix != null ? suffix.length() + 5 : 0;
            FastStringBuffer buf = new FastStringBuffer(info.getDeclaringModuleName(), pathLen + 5 + suffixLen).append(
                    "/").append(path);
            if (suffix != null) {
                return buf.append("   (").append(suffix).append(")").toString();
            }
            return buf.toString();
        }

        String declaringModuleName = info.getDeclaringModuleName();
        if (suffix != null) {
            return new FastStringBuffer(declaringModuleName, suffix.length() + 6).append("   (").append(suffix)
                    .append(")").toString();
        }
        return declaringModuleName;
    }

    @Override
    public Image getImage(Object element) {
        IInfo info = NameIInfoLabelProvider.getInfo(element);
        if (info == null) {
            return null;
        }
        return AnalysisPlugin.getImageForTypeInfo(info);
    }
}