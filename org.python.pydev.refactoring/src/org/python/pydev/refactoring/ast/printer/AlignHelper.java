/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.printer;

public class AlignHelper {

    private int alignment;

    private int alignmentSteps;

    private char alignmentSeparator;

    public AlignHelper() {
        this(4, ' ');
    }

    public AlignHelper(int alignmentSteps, char alignmentSeparator) {
        this.alignmentSteps = alignmentSteps;
        this.alignmentSeparator = alignmentSeparator;
    }

    public void indent() {
        alignment += alignmentSteps;
    }

    public void outdent() {
        alignment -= alignmentSteps;
        if (alignment < 0)
            alignment = 0;
    }

    public String getAlignment() {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < alignment; i++)
            s.append(alignmentSeparator);
        return s.toString();
    }

}
