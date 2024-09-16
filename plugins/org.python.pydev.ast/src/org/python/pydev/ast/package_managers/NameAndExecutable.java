package org.python.pydev.ast.package_managers;

import org.python.pydev.shared_core.string.FastStringBuffer;

public final class NameAndExecutable {

    public String name;
    public final String executable;

    public NameAndExecutable(String name, String executableOrJar) {
        this.name = name;
        this.executable = executableOrJar;
    }

    public String getName() {
        return this.name;
    }

    public String getExecutableOrJar() {
        return this.executable;
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
        buffer.appendObject(name);
        buffer.append(" -- ");
        buffer.appendObject(executable);
        buffer.append("]");
        return buffer.toString();
    }

}
