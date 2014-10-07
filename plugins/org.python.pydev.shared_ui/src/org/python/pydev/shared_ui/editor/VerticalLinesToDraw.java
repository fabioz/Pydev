/**
 * Copyright (c) 2014 by Brainwy Software LTDA. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.shared_ui.editor;

import org.eclipse.swt.graphics.GC;

public final class VerticalLinesToDraw {

    public final int x0;
    public final int x1;
    public final int y0;
    public final int y1;

    public VerticalLinesToDraw(int x0, int y0, int x1, int y1) {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
    }

    public void drawLine(GC gc) {
        gc.drawLine(this.x0, this.y0, this.x1, this.y1);
    }

    public VerticalLinesToDraw copyChangingYOffset(int lineHeight) {
        return new VerticalLinesToDraw(this.x0, this.y0 + lineHeight, this.x1, this.y1 + lineHeight);
    }

}
