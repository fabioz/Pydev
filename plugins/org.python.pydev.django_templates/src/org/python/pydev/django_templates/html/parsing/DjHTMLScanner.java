/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.html.parsing;

import org.python.pydev.django_templates.comon.parsing.DjScanner;

import com.aptana.editor.html.parsing.HTMLScanner;

public class DjHTMLScanner extends HTMLScanner {

    private DjScanner djScanner = new DjScanner();

    public DjHTMLScanner() {
        super(new DjHTMLTokenScanner(), DjScanner.SWITCH_STRATEGY);
    }

    public short getTokenType(Object data) {
        Short tokenType = djScanner.getTokenType(this, data);
        if (tokenType != null) {
            return tokenType;
        }
        return super.getTokenType(data);
    }
}
