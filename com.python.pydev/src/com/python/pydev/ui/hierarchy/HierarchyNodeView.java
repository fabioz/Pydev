/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.awt.Color;
import java.awt.geom.Rectangle2D;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolox.swt.PSWTCanvas;
import edu.umd.cs.piccolox.swt.PSWTImage;
import edu.umd.cs.piccolox.swt.PSWTPath;
import edu.umd.cs.piccolox.swt.PSWTText;

/**
 * This class represents the node view, containing:
 * - A rectangle with the bounds (root of the others)
 * - An image to represent it
 * - The text to represent it
 * 
 * @author Fabio
 */
public class HierarchyNodeView {
    public PSWTPath node;
    public HierarchyNodeModel model;
    
    /**
     * Creates the node based on its starting position.
     */
    public HierarchyNodeView(PSWTCanvas canvas, HierarchyNodeModel model, double x, double y) {
    	this.model = model;
        final PSWTImage img = new PSWTImage(canvas, "e:\\eclipse_workspace\\com.python.pydev\\src\\class_obj.gif");
        img.translate(0+x, 5+y);
        Rectangle2D imgRect = img.getBounds().getBounds2D();
        final PSWTText text = new PSWTText(model.name);
        Rectangle2D textRect = text.getBounds().getBounds2D();
        text.translate(imgRect.getWidth()+2+x, 0+y);
        
        final PSWTPath rect = PSWTPath.createRectangle((float)(imgRect.getX()+x), (float)(imgRect.getY()+y), 
                (float)(imgRect.getWidth()+textRect.getWidth()+2), (float)(textRect.getHeight()));
        rect.addChild(img);
        rect.addChild(text);
        
        rect.addInputEventListener(new PBasicInputEventHandler(){
            @Override
            public void mouseEntered(PInputEvent event) {
                rect.setPaint(Color.RED);
            }
            @Override
            public void mouseExited(PInputEvent event) {
                rect.setPaint(Color.WHITE);
            }
        });
        
        this.node = rect;
    }
}
