/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */

package org.python.pydev.django_templates.html.editor;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.editor.DjPartitionerSwitchStrategy;

import com.aptana.editor.common.CompositeDocumentProvider;
import com.aptana.editor.html.HTMLSourceConfiguration;

/**
 * @author Fabio Zadrozny
 */
public class DjHTMLDocumentProvider extends CompositeDocumentProvider {

    public DjHTMLDocumentProvider() {
        super(IDjConstants.CONTENT_TYPE_DJANGO_HTML, HTMLSourceConfiguration.getDefault(), DjHtmlSourceConfiguration
                .getDefault(), DjPartitionerSwitchStrategy.getDefault());
    }

}
