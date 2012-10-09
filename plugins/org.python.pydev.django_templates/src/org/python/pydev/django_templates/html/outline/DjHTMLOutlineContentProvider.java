/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.outline;

import org.python.pydev.django_templates.IDjConstants;

import com.aptana.editor.common.AbstractThemeableEditor;
import com.aptana.editor.common.outline.CommonOutlineContentProvider;
import com.aptana.editor.html.outline.HTMLOutlineContentProvider;

public class DjHTMLOutlineContentProvider extends HTMLOutlineContentProvider {

    public DjHTMLOutlineContentProvider(AbstractThemeableEditor editor) {
        super(editor);
        addSubLanguage(IDjConstants.CONTENT_TYPE_DJANGO_HTML, new CommonOutlineContentProvider());
    }
}
