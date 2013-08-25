package org.python.pydev.shared_core.partitioner;

public interface IMarkScanner {

    /**
     * @return a mark identifying the current offset.
     */
    public int getMark();

    /**
     * Resets the scanner to the current place.
     */
    public void setMark(int offset);

}
