/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.debug.pyunit;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.python.pydev.core.callbacks.CallbackWithListeners;
import org.python.pydev.core.callbacks.ICallbackWithListeners;

public abstract class ViewPartWithOrientation extends ViewPart {

    protected Composite fParent;
    protected int fCurrentOrientation;
    
    protected int VIEW_ORIENTATION_HORIZONTAL = 0;
    protected int VIEW_ORIENTATION_VERTICAL = 1;
    
    @SuppressWarnings("rawtypes")
    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();
    
    @SuppressWarnings("rawtypes")
    public final ICallbackWithListeners onDispose = new CallbackWithListeners();
    

    public void createPartControl(Composite parent) {
        fParent= parent;
        addResizeListener(parent);
    }
    
    protected void updateOrientation() {
        Point size= fParent.getSize();
        if (size.x != 0 && size.y != 0) {
            if (size.x > size.y){
                setNewOrientation(VIEW_ORIENTATION_HORIZONTAL);
            }else{
                setNewOrientation(VIEW_ORIENTATION_VERTICAL);
            }
        }
    }
    
    private void addResizeListener(Composite parent) {
        parent.addControlListener(new ControlListener() {
            public void controlMoved(ControlEvent e) {
            }
            public void controlResized(ControlEvent e) {
                updateOrientation();
            }
        });
    }
    
    protected abstract void setNewOrientation(int orientation);

}
