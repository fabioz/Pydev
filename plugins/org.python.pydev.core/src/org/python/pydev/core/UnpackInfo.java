/**
 * Copyright (c) 2015 by Brainwy Software Ltda. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

public class UnpackInfo {

    private int unpackTuple = -1;
    private boolean unpackFor = false;
    private boolean unpackBackwards;
    private boolean unpackWith;

    public UnpackInfo() {

    }

    public UnpackInfo(boolean unpackFor, boolean unpackWith, int unpackTuple) {
        this(unpackFor, unpackWith, unpackTuple, false);
    }

    public UnpackInfo(boolean unpackFor, int unpackTuple) {
        this(unpackFor, false, unpackTuple, false);
    }

    /**
     * @param unpackBackwards means something as a[-1], a[-2] (but the unpackTuple is still positive)
     */
    public UnpackInfo(boolean unpackFor, boolean unpackWith, int unpackTuple, boolean unpackBackwards) {
        this.unpackFor = unpackFor;
        this.unpackWith = unpackWith;
        this.unpackTuple = unpackTuple;
        this.unpackBackwards = unpackBackwards;
    }

    public void addUnpackFor() {
        unpackFor = true;
    }

    public void addUnpackWith() {
        unpackWith = true;
    }

    public void addUnpackTuple(int i) {
        unpackTuple = i;
    }

    /**
     * @param length is the size of the element to be unpacked (we need the size if
     * the user specified something as a[-1], so, it has to be calculated).
     * @return the index to be used to unpack or -1 if it should not be unpacked.
     */
    public int getUnpackTuple(int length) {
        if (unpackTuple >= length) {
            return -1;
        }
        if (unpackBackwards) {
            return length - unpackTuple;
        }
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

    public boolean getUnpackWith() {
        return this.unpackWith;
    }

    public boolean hasUnpackInfo() {
        return this.unpackTuple >= 0;
    }

}
