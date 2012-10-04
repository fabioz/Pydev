package com.python.pydev.actions;

import org.eclipse.jface.viewers.DecoratingStyledCellLabelProvider;
import org.eclipse.jface.viewers.IDecorationContext;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.graphics.Image;

/**
 * Needed so that the decoration happens properly for the module names.
 * 
 * @author fabioz
 */
public final class LabelProviderWithDecoration extends DecoratingStyledCellLabelProvider implements ILabelProvider {
    private ILabelProvider labelProvider;

    public LabelProviderWithDecoration(IStyledLabelProvider labelProvider, ILabelDecorator decorator,
            IDecorationContext decorationContext) {
        super(labelProvider, decorator, decorationContext);
        this.labelProvider = (ILabelProvider) labelProvider;
    }

    public String getText(Object element) {
        return labelProvider.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        return labelProvider.getImage(element);
    }
}