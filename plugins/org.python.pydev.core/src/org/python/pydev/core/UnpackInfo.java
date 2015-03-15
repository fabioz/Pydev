package org.python.pydev.core;

public class UnpackInfo {

    private int unpackTuple = -1;
    private boolean unpackFor = false;

    public UnpackInfo() {

    }

    public UnpackInfo(boolean unpackFor, int unpackTuple) {
        this.unpackFor = unpackFor;
        this.unpackTuple = unpackTuple;
    }

    public void addUnpackFor() {
        unpackFor = true;
    }

    public void addUnpackTuple(int i) {
        unpackTuple = i;
    }

    public int getUnpackTuple() {
        return unpackTuple;
    }

    public UnpackInfo cloneWithUnpackTuple(int newUnpackTuple) {
        UnpackInfo ret = new UnpackInfo();
        ret.unpackFor = this.unpackFor;
        ret.unpackTuple = newUnpackTuple;
        return ret;
    }

    public boolean getUnpackFor() {
        return this.unpackFor;
    }

}
