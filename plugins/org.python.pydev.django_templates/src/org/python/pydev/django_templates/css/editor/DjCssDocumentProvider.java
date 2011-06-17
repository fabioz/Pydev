/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.css.editor;

import org.python.pydev.django_templates.IDjConstants;
import org.python.pydev.django_templates.editor.DjPartitionerSwitchStrategy;

import com.aptana.editor.common.CompositeDocumentProvider;
import com.aptana.editor.css.CSSSourceConfiguration;

/**
 * @author Fabio Zadrozny
 */
public class DjCssDocumentProvider extends CompositeDocumentProvider {

    public DjCssDocumentProvider() {
        super(IDjConstants.CONTENT_TYPE_DJANGO_CSS, CSSSourceConfiguration.getDefault(), DjCssSourceConfiguration.getDefault(),
                DjPartitionerSwitchStrategy.getDefault());
    }

}
