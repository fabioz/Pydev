package org.python.pydev.core;

import org.python.pydev.core.ICompletionState.LookingFor;

public class IterEntry {

    public IterEntry() {

    }

    public IterEntry(Object o) {
        this.object = o;
    }

    public Object object;
    public LookingFor lookingFor;

}