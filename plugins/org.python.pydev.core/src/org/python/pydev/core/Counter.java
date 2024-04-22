package org.python.pydev.core;

public class Counter {

    private boolean ascending = true;
    private int currValue = 0;

    public Counter() {
    }

    public Counter(int firstValue) {
        this.currValue = firstValue;
    }

    public Counter(int firstValue, boolean ascending) {
        this.currValue = firstValue;
        this.ascending = ascending;
    }

    public int next() {
        int ret = this.currValue;
        if (ascending) {
            this.currValue += 1;
        } else {
            this.currValue -= 1;
        }
        return ret;
    }

}
