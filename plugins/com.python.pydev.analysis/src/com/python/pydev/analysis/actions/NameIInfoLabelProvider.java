package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.python.pydev.analysis.AnalysisPlugin;
import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Provides the labels and images for the top panel
 *
 * @author Fabio
 */
public final class NameIInfoLabelProvider extends LabelProvider {
    
    private final boolean showCompleteName;

    public NameIInfoLabelProvider(boolean showCompleteName){
        this.showCompleteName = showCompleteName;
    }
    
    @Override
    public String getText(Object element) {
        if(element instanceof AdditionalInfoAndIInfo){
            element = ((AdditionalInfoAndIInfo)element).info;
        }
        IInfo info = (IInfo) element;
        if(info == null){
            return "";
        }
        if(showCompleteName){
            return info.getName()+ " - " + info.getDeclaringModuleName();
        }
        return info.getName();
    }

    @Override
    public Image getImage(Object element) {
        if(element instanceof AdditionalInfoAndIInfo){
            element = ((AdditionalInfoAndIInfo)element).info;
        }
        IInfo info = (IInfo) element;
        if(info == null){
            return null;
        }
        return AnalysisPlugin.getImageForTypeInfo(info);
    }
}