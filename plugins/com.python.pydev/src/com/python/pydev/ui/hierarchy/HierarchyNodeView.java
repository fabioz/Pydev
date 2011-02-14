/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

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
    public Color initialColor;
    public List<HierarchyNodeViewListener> listeners = new ArrayList<HierarchyNodeViewListener>();
    
    public HierarchyNodeView(PSWTCanvas canvas, HierarchyNodeModel model, double x, double y) {
        this(canvas, model, x, y, Color.WHITE);
    }

    protected void onClick(PInputEvent event) {
        for(HierarchyNodeViewListener l:listeners){
            l.onClick(this, event);
        }
    }

    public void addListener(HierarchyNodeViewListener l) {
        this.listeners.add(l);
    }

    /**
     * Creates the node based on its starting position.
     */
    public HierarchyNodeView(PSWTCanvas canvas, HierarchyNodeModel model, double x, double y, Color initialColor) {
        this.model = model;
        this.initialColor = initialColor;
        Image classImg = PydevPlugin.getImageCache().get(UIConstants.CLASS_ICON);
        final PSWTImage img = new PSWTImage(canvas, classImg, false); //do not dispose it.
        img.translate(0+x, 5+y);
        Rectangle2D imgRect = img.getBounds().getBounds2D();
        final PSWTText text = new PSWTText(model.name);
        Rectangle2D textRect = text.getBounds().getBounds2D();
        text.translate(imgRect.getWidth()+2+x, 0+y);
        
        final PSWTPath rect = PSWTPath.createRectangle((float)(imgRect.getX()+x-5), (float)(imgRect.getY()+y), 
                (float)(imgRect.getWidth()+textRect.getWidth()+2+10), (float)(textRect.getHeight()));
        rect.addChild(img);
        rect.addChild(text);
        rect.setPaint(HierarchyNodeView.this.initialColor);
        
        rect.addInputEventListener(new PBasicInputEventHandler(){
            @Override
            public void mouseEntered(PInputEvent event) {
                Color c = HierarchyNodeView.this.initialColor;
                Color c1 = new Color(c.getRed()-20, c.getBlue()-20, c.getGreen()-20);
                rect.setPaint(c1);
            }
            @Override
            public void mouseExited(PInputEvent event) {
                rect.setPaint(HierarchyNodeView.this.initialColor);
            }
            
            //simulate the click... not sure why it way not getting mouseClicked...
            long pressedOn = -1;
            @Override
            public void mousePressed(PInputEvent event) {
                pressedOn = System.currentTimeMillis();
            }
            @Override
            public void mouseReleased(PInputEvent event) {
                long delta = System.currentTimeMillis() - pressedOn;
                if(delta < 200){
                    onClick(event);
                }
            }
        });
        
        this.node = rect;
    }
    
    @Override
    public String toString() {
        return "Node View["+model.name+"]";
    }
    
    @Override
    public int hashCode() {
        return model.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return model.equals(((HierarchyNodeView)obj).model);
    }
}
