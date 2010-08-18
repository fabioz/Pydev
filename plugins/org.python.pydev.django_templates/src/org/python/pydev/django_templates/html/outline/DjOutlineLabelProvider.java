package org.python.pydev.django_templates.html.outline;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;

import com.aptana.editor.common.outline.CommonOutlineItem;

public class DjOutlineLabelProvider extends LabelProvider {

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
