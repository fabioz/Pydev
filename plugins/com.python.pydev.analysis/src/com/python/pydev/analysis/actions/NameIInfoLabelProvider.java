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
public class NameIInfoLabelProvider extends LabelProvider {
    
    /**
     * Should we should the whole name with the package structure or only the name of the token (if true
     * shows it fully qualified)
     */
    protected final boolean showCompleteName;

    public NameIInfoLabelProvider(boolean showCompleteName){
        this.showCompleteName = showCompleteName;
    }
    
    @Override
    public String getText(Object element) {
        IInfo info = getInfo(element);
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
        IInfo info = getInfo(element);
        if(info == null){
            return null;
        }
        return AnalysisPlugin.getImageForTypeInfo(info);
    }

    
    /**
     * Can return null (i.e. if we receive a string on a multiple selection)
     */
    public static IInfo getInfo(Object element){
        if(element instanceof AdditionalInfoAndIInfo){
            element = ((AdditionalInfoAndIInfo)element).info;
        }
        if(!(element instanceof IInfo)){
            return null;
        }
        IInfo info = (IInfo) element;
        return info;
    }
}