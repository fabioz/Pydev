/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.outline.CommonOutlineItem;

public class DjLanguageOutlineLabelProvider extends LabelProvider {

    @Override
    public Image getImage(Object element) {
        if (element instanceof CommonOutlineItem) {
            return getImage(((CommonOutlineItem) element).getReferenceNode());
        }

        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        if (element instanceof CommonOutlineItem) {
            return getText(((CommonOutlineItem) element).getReferenceNode());
        }
        return super.getText(element);
    }
}
