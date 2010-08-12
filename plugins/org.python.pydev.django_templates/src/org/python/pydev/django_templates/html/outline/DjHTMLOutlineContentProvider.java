package org.python.pydev.django_templates.html.outline;

import org.python.pydev.django_templates.IDjConstants;

import com.aptana.editor.common.outline.CommonOutlineContentProvider;
import com.aptana.editor.html.outline.HTMLOutlineContentProvider;

public class DjHTMLOutlineContentProvider extends HTMLOutlineContentProvider {
    public DjHTMLOutlineContentProvider() {
        addSubLanguage(IDjConstants.LANGUAGE_DJANGO_TEMPLATES, new CommonOutlineContentProvider());
    }
}
