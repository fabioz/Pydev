package org.python.pydev.debug.model;

public class SmartStepIntoVariant {

    public final AbstractDebugTarget target;
    public final String name;
    public final boolean isVisited;
    public final int line;
    public final int offset;
    public final int callOrder;
    public final int childOffset;

    public SmartStepIntoVariant(AbstractDebugTarget target, String name, boolean isVisited, int line, int offset,
            int childOffset, int callOrder) {
        this.target = target;
        this.name = name;
        this.isVisited = isVisited;
        this.line = line;
        this.offset = offset;
        this.childOffset = childOffset;
        this.callOrder = callOrder;
    }

    @Override
    public String toString() {
        return "Step In: " + this.name + " line: " + this.line;
    }
}
