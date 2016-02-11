/**
 * Copyright (c) 2016 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 * 
 * Copied from the JDT implementation of
 * <code>org.eclipse.jdt.internal.corext.util.Messages</code>.
 */
package org.python.pydev.utils;

import com.ibm.icu.text.MessageFormat;

/**
 * Helper class to format message strings.
 *
 * @since 3.1
 */
public class Messages {

    public static String format(String message, Object object) {
        return MessageFormat.format(message, new Object[] { object });
    }

    public static String format(String message, Object[] objects) {
        return MessageFormat.format(message, objects);
    }

    private Messages() {
        // Not for instantiation
    }
}
