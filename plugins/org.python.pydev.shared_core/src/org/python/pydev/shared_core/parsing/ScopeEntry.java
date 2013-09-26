package org.python.pydev.shared_core.parsing;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class ScopeEntry {

    public final int type;
    public final boolean open;
    public final int id;
    public final int offset;

    public ScopeEntry(int id, int type, boolean open, int offset) {
        this.type = type;
        this.open = open;
        this.id = id;
        this.offset = offset;
    }

    public void toString(FastStringBuffer temp) {
        if (open) {
            temp.append('[');
            temp.append(id);
            temp.append(' ');
        } else {
            temp.append(' ');
            temp.append(id);
            temp.append(']');
        }
    }

}
