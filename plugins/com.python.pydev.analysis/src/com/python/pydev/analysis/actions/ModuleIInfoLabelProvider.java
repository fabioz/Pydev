/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.core.structure.FastStringBuffer;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Provides the labels and images for the bottom panel
 *
 * @author Fabio
 */
public final class ModuleIInfoLabelProvider extends LabelProvider {
    
    @Override
    public String getText(Object element) {
        if(element instanceof AdditionalInfoAndIInfo){
            element = ((AdditionalInfoAndIInfo)element).info;
        }
        if(element instanceof String){
            return (String) element;
        }
        IInfo info = (IInfo) element;
        String path = info.getPath();
        int pathLen;
        if(path != null && (pathLen = path.length()) > 0){
            FastStringBuffer buf = new FastStringBuffer(info.getDeclaringModuleName(), pathLen+5);
            buf.append("/");
            buf.append(path);
            return buf.toString();
        }
        return info.getDeclaringModuleName();
    }

    @Override
    public Image getImage(Object element) {
        IInfo info = NameIInfoLabelProvider.getInfo(element);
        if(info == null){
            return null;
        }
        return AnalysisPlugin.getImageForTypeInfo(info);
    }
}