/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package org.python.pydev.refactoring.tests.visitors;

public class MockupSelectionConfig {

    private int offset;

    private int selectionLength;

    private int offsetStrategy;

    public MockupSelectionConfig(int offset, int selectionLength, int offsetStrategy) {
        super();
        this.offset = offset;
        this.selectionLength = selectionLength;
        this.offsetStrategy = offsetStrategy;
    }

    public int getOffset() {
        return offset;
    }

    public int getOffsetStrategy() {
        return offsetStrategy;
    }

    public int getSelectionLength() {
        return selectionLength;
    }

}
