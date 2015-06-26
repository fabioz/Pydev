/**
 * Copyright (c) 20015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.utils;

import org.eclipse.swt.graphics.RGB;
import org.python.pydev.shared_core.log.Log;

public final class ColorParse {

    public static RGB parseRGB(String value, RGB defaultColor) {
        int r;
        int g;
        int b;

        try {
            if (value != null) {
                if (value.startsWith("#") && value.length() >= 7) {
                    r = Integer.parseInt(value.substring(1, 3), 16);
                    g = Integer.parseInt(value.substring(3, 5), 16);
                    b = Integer.parseInt(value.substring(5, 7), 16);

                    if (r < 0) {
                        r = 0;
                    }
                    if (g < 0) {
                        g = 0;
                    }
                    if (b < 0) {
                        b = 0;
                    }
                    if (r > 255) {
                        r = 255;
                    }
                    if (g > 255) {
                        g = 255;
                    }
                    if (b > 255) {
                        b = 255;
                    }
                    return new RGB(r, g, b);
                } else {
                    // Not in hexa: i.e.: r,g,b comma-separated.
                    String[] s = value.split("\\,");
                    if (s.length >= 3) {
                        r = Integer.parseInt(s[0]);
                        g = Integer.parseInt(s[1]);
                        b = Integer.parseInt(s[2]);
                        return new RGB(r, g, b);

                    }
                }
            }
        } catch (Exception e) {
            Log.log(e);
        }
        return defaultColor;
    }

}
