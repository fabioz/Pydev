/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.editor;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.editor.DjSourceConfiguration;

import com.aptana.editor.common.CommonEditorPlugin;
import com.aptana.editor.common.scripting.IContentTypeTranslator;
import com.aptana.editor.common.scripting.QualifiedContentType;

public class DjHtmlSourceConfiguration extends DjSourceConfiguration {

    public DjHtmlSourceConfiguration() {
        super(IDjConstants.CONTENT_TYPE_DJANGO_HTML);
    }

    private static DjSourceConfiguration instance;

    static {
        IContentTypeTranslator c = CommonEditorPlugin.getDefault().getContentTypeTranslator();
        c.addTranslation(new QualifiedContentType(IDjConstants.CONTENT_TYPE_DJANGO_HTML), new QualifiedContentType(
                SOURCE_DJ));
        c.addTranslation(new QualifiedContentType(STRING_SINGLE), new QualifiedContentType(STRING_QUOTED_SINGLE_DJ));
        c.addTranslation(new QualifiedContentType(STRING_DOUBLE), new QualifiedContentType(STRING_QUOTED_DOUBLE_DJ));
        c.addTranslation(new QualifiedContentType(COMMENT), new QualifiedContentType(COMMENT_DJ));
    }

    public static DjSourceConfiguration getDefault() {
        if (instance == null) {
            instance = new DjHtmlSourceConfiguration();
        }
        return instance;
    }

}
