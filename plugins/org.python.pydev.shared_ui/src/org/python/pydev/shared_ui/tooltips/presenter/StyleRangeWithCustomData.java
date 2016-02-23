/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.tooltips.presenter;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

/**
 * @author fabioz
 *
 */
public class StyleRangeWithCustomData extends StyleRange {

    /** 
     * Create a new style range.
     *
     * @param start start offset of the style
     * @param length length of the style 
     * @param foreground foreground color of the style, null if none 
     * @param background background color of the style, null if none
     * @param fontStyle font style of the style, may be SWT.NORMAL, SWT.ITALIC or SWT.BOLD
     */
    public StyleRangeWithCustomData(int start, int length, Color foreground, Color background, int fontStyle) {
        super(start, length, foreground, background, fontStyle);
    }

    public StyleRangeWithCustomData(int start, int length, Color foreground, Color background) {
        super(start, length, foreground, background);
    }

    public StyleRangeWithCustomData() {
        super();
    }

    public Object customData;

    @Override
    public boolean similarTo(StyleRange style) {
        if (!(style instanceof StyleRangeWithCustomData)) {
            return false;
        }
        StyleRangeWithCustomData other = (StyleRangeWithCustomData) style;
        if (customData == null) {
            if (other.customData != null)
                return false;
        } else if (!customData.equals(other.customData))
            return false;

        if (super.similarTo(style)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((customData == null) ? 0 : customData.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        StyleRangeWithCustomData other = (StyleRangeWithCustomData) obj;
        if (customData == null) {
            if (other.customData != null)
                return false;
        } else if (!customData.equals(other.customData))
            return false;
        return true;
    }
}
