/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.outline;

import java.util.StringTokenizer;

import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.graphics.Image;
import org.python.pydev.django_templates.DjPlugin;
import org.python.pydev.django_templates.comon.parsing.DjangoTemplatesNode;

import com.aptana.editor.common.outline.CommonOutlineItem;
import com.aptana.parsing.ast.IParseNode;
import com.aptana.parsing.ast.ParseNode;

public class DjOutlineLabelProvider {

    private IDocument fDocument;

    private static final int TRIM_TO_LENGTH = 20;

    public DjOutlineLabelProvider(IDocument document) {
        this.fDocument = document;
    }

    public Image getImage(Object element) {
        if (element instanceof CommonOutlineItem) {
            IParseNode node = ((CommonOutlineItem) element).getReferenceNode();

            if (node instanceof DjangoTemplatesNode) {
                return DjPlugin.getImageCache().get("icons/element.gif");
            }
        }
        return null;
    }

    private String getDisplayText(DjangoTemplatesNode script) {
        StringBuilder text = new StringBuilder();
        text.append(script.getStartTag()).append(" "); //$NON-NLS-1$
        String source = new String(fDocument.get());
        // locates the source
        ParseNode node = script.getNode();
        source = source.substring(node.getStartingOffset(), node.getEndingOffset() + 1);
        // gets the first line of the source
        StringTokenizer st = new StringTokenizer(source, "\n\r\f"); //$NON-NLS-1$
        source = st.nextToken();
        if (source.length() <= TRIM_TO_LENGTH) {
            text.append(source);
        } else {
            text.append(source.substring(0, TRIM_TO_LENGTH - 1)).append("..."); //$NON-NLS-1$
        }
        text.append(" ").append(script.getEndTag()); //$NON-NLS-1$
        return text.toString();
    }

    public String getText(Object element) {
        if (element instanceof CommonOutlineItem) {
            IParseNode node = ((CommonOutlineItem) element).getReferenceNode();
            if (node instanceof DjangoTemplatesNode) {
                return getDisplayText((DjangoTemplatesNode) node);
            }
        }
        return null;
    }

}
