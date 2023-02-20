package org.python.pydev.shared_core.string;

public class LineCol {

    private final int line;
    private final int col;

    public LineCol(int line, int col) {
        this.line = line;
        this.col = col;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + col;
        result = prime * result + line;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LineCol)) {
            return false;
        }
        LineCol other = (LineCol) obj;
        if (col != other.col) {
            return false;
        }
        if (line != other.line) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "LineCol [line=" + line + ", col=" + col + "]";
    }
}
