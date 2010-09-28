/**
 * Copyright (c) 2010 Aptana, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    protected DjHTMLDocumentProvider() {
        super(IDjConstants.CONTENT_TYPE_DJANGO_HTML, HTMLSourceConfiguration.getDefault(), DjHtmlSourceConfiguration.getDefault(),
                DjPartitionerSwitchStrategy.getDefault());
    }

}
