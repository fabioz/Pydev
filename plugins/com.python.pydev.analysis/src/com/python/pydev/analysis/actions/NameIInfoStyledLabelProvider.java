package com.python.pydev.analysis.actions;

import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider.IStyledLabelProvider;

import com.python.pydev.analysis.additionalinfo.IInfo;

/**
 * Just used to declare the interface (as it's not available in eclipse 3.2)
 */
public class NameIInfoStyledLabelProvider extends NameIInfoLabelProvider implements IStyledLabelProvider{

    public NameIInfoStyledLabelProvider(boolean showCompleteName) {
        super(showCompleteName);
    }


    /**
     * @return the text with a style for the module part
     */
    public StyledString getStyledText(Object element){
        IInfo info = getInfo(element);
        if(info == null){
            return new StyledString();
        }
        if(showCompleteName){
            return new StyledString(info.getName()).append(" - " + info.getDeclaringModuleName(), StyledString.QUALIFIER_STYLER);
        }
        return new StyledString(info.getName());
    }
}
