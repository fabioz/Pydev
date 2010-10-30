package org.python.pydev.debug.pyunit;

import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

public abstract class ViewPartWithOrientation extends ViewPart {

    protected Composite fParent;
    protected int fCurrentOrientation;
    
    protected int VIEW_ORIENTATION_HORIZONTAL = 0;
    protected int VIEW_ORIENTATION_VERTICAL = 1;

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
