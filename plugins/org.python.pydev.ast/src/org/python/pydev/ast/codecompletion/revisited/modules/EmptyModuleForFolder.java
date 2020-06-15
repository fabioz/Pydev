package org.python.pydev.ast.codecompletion.revisited.modules;

import java.io.File;

import org.python.pydev.shared_core.string.StringUtils;

public class EmptyModuleForFolder extends EmptyModule {

    private static final long serialVersionUID = 1L;

    public String path;

    public EmptyModuleForFolder(String name, File f, String path) {
        super(name, f);
        this.path = path;
    }

    @Override
    public String toString() {
        return StringUtils.join(" ", "EmptyModuleForFolder[", name, " file: ", f, " path: ", path, "]");
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EmptyModuleForFolder)) {
            return false;
        }
        EmptyModuleForFolder m = (EmptyModuleForFolder) obj;

        if (name == null || m.name == null) {
            if (name != m.name) {
                return false;
            }
            //both null at this point
        } else if (!name.equals(m.name)) {
            return false;
        }

        if (f == null || m.f == null) {
            if (f != m.f) {
                return false;
            }
            //both null at this point
        } else if (!f.equals(m.f)) {
            return false;
        }
        if (path == null || m.path == null) {
            if (path != m.path) {
                return false;
            }
            //both null at this point
        } else if (!path.equals(m.path)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 123;
        if (f != null) {
            hash += f.hashCode();
        }
        if (name != null) {
            hash += name.hashCode();
        }
        if (path != null) {
            hash += path.hashCode();
        }
        return hash;
    }
}
