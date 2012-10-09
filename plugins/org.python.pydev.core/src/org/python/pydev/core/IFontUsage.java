/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

/**
 * <tt>IFontUsage</tt> is an enum-like interface describing usage cases 
 * for fonts used throughout Pydev.
 * <p>
 * It is used primarily by {@link FontUtils} to have all font usages 
 * in a central place to ease future edits.
 * </p>
 * <p>
 * <table width="100%" border="0">
 * <tr><th>value</th><th>used for</th></tr>
 * <tr><td>STYLED</td><td>styled text widgets (ex. Editor prefs)</td></tr>
 * <tr><td>DIALOG</td><td>modal dialogs (ex. Py2To3)</td></tr>
 * <tr><td>WIDGET</td><td>other widgets (ex. Code Coverage view or Comment Blocks prefs)</td></tr>
 * <tr><td>IMAGECACHE</td><td>overlaying monospaced text onto images</td></tr>
 * </table>
 * </p>
 * 
 * @author André Berg
 * @version 0.1
 */
public interface IFontUsage {
    /**
     * used for styled text widgets (ex. Editor prefs)
     */
    public static final int STYLED = 0;
    /**
     * used for modal dialogs (ex. Py2To3 dialog)
     */
    public static final int DIALOG = 1;
    /**
     * used for other widgets (ex. Code Coverage view or Comment Blocks prefs)
     */
    public static final int WIDGET = 2;
    /**
     * used in {@link org.python.pydev.core.bundle.ImageCache ImageCache} for overlaying code text onto images
     */
    public static final int IMAGECACHE = 3;
}
