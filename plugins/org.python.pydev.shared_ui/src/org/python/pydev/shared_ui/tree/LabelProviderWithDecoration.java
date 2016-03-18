/******************************************************************************
* Copyright (C) 2011-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_ui.tree;

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

    @Override
    public String getText(Object element) {
        return labelProvider.getText(element);
    }

    @Override
    public Image getImage(Object element) {
        return labelProvider.getImage(element);
    }
}