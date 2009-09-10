/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.printer;

public class CallDepth {
    private int nestedCallDepth;

    private int savedNestedCallDepth;

    public void enterCall() {
        nestedCallDepth++;
    }

    public void leaveCall() {
        nestedCallDepth--;
        if(nestedCallDepth < 0)
            nestedCallDepth = 0;
    }

    public boolean inCall() {
        return nestedCallDepth > 0;
    }

    public void disableCallDepth() {
        savedNestedCallDepth = nestedCallDepth;
        nestedCallDepth = 0;
    }

    public void enableCallDepth() {
        nestedCallDepth = savedNestedCallDepth;
    }

    public boolean isCallDepthEnabled() {
        return(savedNestedCallDepth == nestedCallDepth);
    }
}
