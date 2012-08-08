/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.osgi.service.environment.Constants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;

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
    private static Tuple<String, Integer> getCodeFontNameAndHeight(int usage) throws IllegalArgumentException {
        String fontName = "Courier New";
        int fontHeight = 10;
        if (Platform.getOS().equals(Constants.OS_MACOSX)) {
            switch (usage) {
                case IFontUsage.STYLED:
                    fontName = "Monaco";
                    fontHeight = 11;
                    break;
                case IFontUsage.DIALOG:
                    // on OS X we need a different font because 
                    // under Mac SWT the bitmap font rasterizer 
                    // doesn't take hinting into account and thus 
                    // makes small fonts rendered as bitmaps unreadable
                    // see http://aptanastudio.tenderapp.com/discussions/problems/2052-some-dialogs-have-unreadable-small-font-size
                    fontName = "Courier";
                    fontHeight = 11;
                    break;
                case IFontUsage.WIDGET:
                    fontName = "Monaco";
                    fontHeight = 9;
                    break;
                case IFontUsage.IMAGECACHE:
                    fontName = "Monaco";
                    fontHeight = 11;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Invalid usage. See org.python.pydev.core.IFontUsage for valid values.");
            }
        } else {
            switch (usage) {
                case IFontUsage.STYLED:
                    fontName = "Courier New";
                    fontHeight = 10;
                    break;
                case IFontUsage.DIALOG:
                    fontName = "Courier New";
                    fontHeight = 8;
                    break;
                case IFontUsage.WIDGET:
                    fontName = "Courier New";
                    fontHeight = 10;
                    break;
                case IFontUsage.IMAGECACHE:
                    fontName = "Courier New";
                    fontHeight = 9;
                    break;

                default:
                    throw new IllegalArgumentException(
                            "Invalid usage. See org.python.pydev.core.IFontUsage for valid values.");
            }
        }
        return new Tuple<String, Integer>(fontName, fontHeight);
    }

    public static FontData getFontData(int usage, boolean useDefaultJFaceFontIfPossible) {
        if (useDefaultJFaceFontIfPossible) {
            FontData[] textFontData = JFaceResources.getTextFont().getFontData();
            if (textFontData.length == 1) {
                return textFontData[0];
            }
        }
        Tuple<String, Integer> codeFontDetails = FontUtils.getCodeFontNameAndHeight(IFontUsage.IMAGECACHE);
        String fontName = codeFontDetails.o1;
        int base = codeFontDetails.o2.intValue();
        return new FontData(fontName, base, SWT.BOLD);
    }
}
