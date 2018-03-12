package org.python.pydev.core;

import org.python.pydev.core.ICompletionState.LookingFor;

public class IterTokenEntry extends IterEntry {

    public IterTokenEntry() {
        super();
    }

    public IterTokenEntry(Object o) {
        super(o);
    }

    public IterTokenEntry(IToken initialToken, LookingFor lookingFor) {
        super(initialToken);
        this.lookingFor = lookingFor;
    }

    public IToken getToken() {
        return (IToken) object;
    }

}
