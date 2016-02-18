/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *        Andy Clement (GoPivotal, Inc) aclement@gopivotal.com - Contributions for
 *                          Bug 383624 - [1.8][compiler] Revive code generation support for type annotations (from Olivier's work)
 *     Jesper Steen Moeller - Contribution for
 *                          Bug 406973 - [compiler] Parse MethodParameters attribute
 *     Mark Leone - Modifications for PyDev
 *******************************************************************************/
package org.python.pydev.utils;

/**
 * Helper class to format message strings.
 *
 * @since 3.1
 */
public class Messages {

    public static String format(String message, Object object) {
        return String.format(message, new Object[] { object });
    }

    public static String format(String message, Object[] objects) {
        return String.format(message, objects);
    }

    private Messages() {
        // Not for instantiation
    }
}
