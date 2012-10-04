/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.outline;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.outline.DjLanguageOutlineLabelProvider;
import org.python.pydev.django_templates.outline.DjOutlineLabelProvider;

import com.aptana.editor.html.outline.HTMLOutlineLabelProvider;

public class DjHTMLOutlineLabelProvider extends HTMLOutlineLabelProvider {

    private DjOutlineLabelProvider labelProvider;

    public DjHTMLOutlineLabelProvider(IDocument document) {
        labelProvider = new DjOutlineLabelProvider(document);
        addSubLanguage(IDjConstants.CONTENT_TYPE_DJANGO_HTML, new DjLanguageOutlineLabelProvider());
    }

    @Override
    public Image getImage(Object element) {
        Image image = labelProvider.getImage(element);
        if (image != null) {
            return image;
        }
        return super.getImage(element);
    }

    @Override
    public String getText(Object element) {
        String text = labelProvider.getText(element);
        if (text != null) {
            return text;
        }
        return super.getText(element);
    }

}
