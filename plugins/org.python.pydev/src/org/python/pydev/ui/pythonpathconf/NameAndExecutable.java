package org.python.pydev.ui.pythonpathconf;

import org.python.pydev.shared_core.string.FastStringBuffer;

public class NameAndExecutable {

    public String o1;
    public final String o2;

    public NameAndExecutable(String name, String executableOrJar) {
        this.o1 = name;
        this.o2 = executableOrJar;
    }

    @Override
    public int hashCode() {
        throw new RuntimeException("not hashable.");
    }

    @Override
    public boolean equals(Object obj) {
        throw new RuntimeException("equals not implemented.");
    };

    @Override
    public String toString() {
        FastStringBuffer buffer = new FastStringBuffer();
        buffer.append("NameAndExecutable [");
        buffer.appendObject(o1);
        buffer.append(" -- ");
        buffer.appendObject(o2);
        buffer.append("]");
        return buffer.toString();
    }

}
