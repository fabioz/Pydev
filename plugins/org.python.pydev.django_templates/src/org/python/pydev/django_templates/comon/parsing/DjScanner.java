/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.django_templates.comon.parsing;

import org.python.pydev.django_templates.common.parsing.lexer.DjangoTemplatesTokens;

import com.aptana.editor.common.parsing.CompositeTokenScanner;
import com.aptana.editor.common.parsing.IScannerSwitchStrategy;
import com.aptana.editor.common.parsing.ScannerSwitchStrategy;

public class DjScanner {

    private static final String[] DJ_ENTER_TOKENS = new String[] { DjangoTemplatesTokens
            .getTokenName(DjangoTemplatesTokens.DJ_START) };
    private static final String[] DJ_EXIT_TOKENS = new String[] { DjangoTemplatesTokens
            .getTokenName(DjangoTemplatesTokens.DJ_END) };
    private static final IScannerSwitchStrategy DJ_STRATEGY = new ScannerSwitchStrategy(DJ_ENTER_TOKENS, DJ_EXIT_TOKENS);

    private boolean isInDj;

    public static final IScannerSwitchStrategy[] SWITCH_STRATEGY = new IScannerSwitchStrategy[] { DJ_STRATEGY };

    public Short getTokenType(CompositeTokenScanner scanner, Object data) {
        IScannerSwitchStrategy strategy = scanner.getCurrentSwitchStrategy();
        if (strategy == DJ_STRATEGY) {
            if (!isInDj) {
                isInDj = true;
            }
            return DjangoTemplatesTokens.DJ_START;
        }
        if (strategy == null && isInDj) {
            isInDj = false;
            return DjangoTemplatesTokens.DJ_END;
        }
        return null;
    }

}
