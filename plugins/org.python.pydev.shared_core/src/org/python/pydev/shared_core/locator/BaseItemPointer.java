package org.python.pydev.shared_core.locator;

import org.python.pydev.shared_core.structure.Location;

public class BaseItemPointer {
    /**
     * IFile or File object (may be null)
     */
    public final Object file;

    /**
     * Position of the 1st character 
     */
    public final Location start;

    /**
     * Position of the last character
     */
    public final Location end;

    public BaseItemPointer(Object file, Location start, Location end) {
        this.file = file;
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer("ItemPointer [");
        buffer.append(file);
        buffer.append(" - ");
        buffer.append(start);
        buffer.append(" - ");
        buffer.append(end);
        buffer.append("]");
        return buffer.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        BaseItemPointer i = (BaseItemPointer) obj;
        if (!i.file.equals(file)) {
            return false;
        }
        if (!i.start.equals(start)) {
            return false;
        }
        if (!i.end.equals(end)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int colLineBasedHash = (this.end.column + this.start.line + 7) * 3;
        if (this.file != null) {
            return this.file.hashCode() + colLineBasedHash;
        } else {
            return colLineBasedHash;
        }
    }
}
