package org.python.pydev.dltk.console.ui;

import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.graphics.Color;

public class ScriptStyleRange extends StyleRange{

    public static final int UNKNOWN = -1;
    public static final int STDIN = 0;
    public static final int PROMPT = 1;
    public static final int STDOUT = 2;
    public static final int STDERR = 3;
    
    public int scriptType;

    public ScriptStyleRange(int start, int len, Color fore, Color back, int scriptType) {
        super(start, len, fore, back);
        this.scriptType = scriptType;
    }

}
