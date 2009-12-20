package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;

import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;

public class AssignCompletionInfo{

    public final ArrayList<IToken> completions;
    public final Definition[] defs;

    public AssignCompletionInfo(Definition[] defs, ArrayList<IToken> ret) {
        this.defs = defs;
        this.completions = ret;
    }

}
