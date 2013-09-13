/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_interactive_console.console.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

/**
 * Defines a style range that identifies a given type of content in the console (as defined in the constants)
 *
 * @author Fabio
 */
public class ScriptStyleRange extends StyleRange {

    public static final int UNKNOWN = -1;
    public static final int STDIN = 0;
    public static final int PROMPT = 1;
    public static final int STDOUT = 2;
    public static final int STDERR = 3;

    public int scriptType;

    public ScriptStyleRange(int start, int len, Color fore, Color back, int scriptType) {
        super(start, len, fore, back);
        Assert.isTrue(len >= 0);
        this.scriptType = scriptType;
    }

    public ScriptStyleRange(int start, int len, Color fore, Color back, int scriptType, int fontStyle) {
        this(start, len, fore, back, scriptType);
        this.fontStyle = fontStyle;
    }

}
