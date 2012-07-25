/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.common;

import org.python.pydev.core.docutils.PythonPairMatcher;
import org.python.pydev.editor.PyDoubleClickStrategy;

public class DjDoubleClickStrategy extends PyDoubleClickStrategy {

    public static final char[] BRACKETS = { '{', '}', '(', ')', '[', ']', '<', '>' };

    public DjDoubleClickStrategy(String contentType) {
        super(contentType);
        fPairMatcher = new PythonPairMatcher(BRACKETS);
    }

}
