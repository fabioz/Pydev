/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.osgi.service.environment.Constants;
import org.python.pydev.core.Tuple;

/**
 * <p><tt>FontUtils</tt> provides helper methods dealing with
 * <tt>Font</tt>s.</p>
 * 
 * @author André Berg
 * @version 0.1
 */
public class FontUtils {
    
    /**
     * Selects a proper font name and height for various usage cases of monospaced fonts throughout Pydev.
     * 
     * @param usage intended usage. See {@link IFontUsage} for valid values.
     * @return a {@link Tuple} containing the font name as {@link String} and the base height as {@link Integer}.
     * @throws IllegalArgumentException if <tt>usage</tt> is not found in {@link IFontUsage}.
     */
    public static Tuple<String, Integer> getCodeFontNameAndHeight(String usage) throws IllegalArgumentException {
        String fontName = "Courier New";
        int fontHeight = 10;
        if (Platform.getOS().equals(Constants.OS_MACOSX)) {
            if (usage.equalsIgnoreCase(IFontUsage.STYLED)) {
                fontName = "Monaco";
                fontHeight = 11;
            } else if (usage.equalsIgnoreCase(IFontUsage.DIALOG)) {
                // on OS X we need a different font because 
                // under Mac SWT the bitmap font rasterizer 
                // doesn't take hinting into account and thus 
                // makes small fonts rendered as bitmaps unreadable
                // see http://aptanastudio.tenderapp.com/discussions/problems/2052-some-dialogs-have-unreadable-small-font-size
                fontName = "Courier";
                fontHeight = 11;
            } else if (usage.equalsIgnoreCase(IFontUsage.WIDGET)) {
                fontName = "Monaco";
                fontHeight = 9;
            } else if (usage.equalsIgnoreCase(IFontUsage.IMAGECACHE)) {
                fontName = "Monaco";
                fontHeight = 11;
            } else {
                throw new IllegalArgumentException("Invalid usage. See org.python.pydev.core.IFontUsage for valid values.");
            }
        } else {
            if (usage.equalsIgnoreCase(IFontUsage.STYLED)) {
                fontName = "Courier New";
                fontHeight = 10;
            } else if (usage.equalsIgnoreCase(IFontUsage.DIALOG)) {
                fontName = "Courier New";
                fontHeight = 8;
            } else if (usage.equalsIgnoreCase(IFontUsage.WIDGET)) {
                fontName = "Courier New";
                fontHeight = 10;
            } else if (usage.equalsIgnoreCase(IFontUsage.IMAGECACHE)) {
                fontName = "Courier New";
                fontHeight = 9;
            } else {
                throw new IllegalArgumentException("Invalid usage. See org.python.pydev.core.IFontUsage for valid values.");
            }
        }
        return new Tuple<String, Integer>(fontName, fontHeight);
    }
}
