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
