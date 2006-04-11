/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.awt.geom.Arc2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;

import edu.umd.cs.piccolo.nodes.PPath;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.swt.PSWTCanvas;
import edu.umd.cs.piccolox.swt.PSWTPath;

/**
 * This class is used to view the hierarchy layout of some kind.
 * 
 * Its implementation will allow viewing of simple connectivity for viewing a class hierarchy.
 */
public class HierarchyViewer extends PSWTCanvas{

    public HierarchyViewer(Composite parent, int args) {
        super(parent, args);
    }

    /**
     * Sets the hierarchy initializing on the 'node of interest', that is the initial node here.
     */
    public void setHierarchy(HierarchyNodeModel initialNode){
        Set<HierarchyNodeModel> nodesAdded = new HashSet<HierarchyNodeModel>();

        HierarchyNodeView view = new HierarchyNodeView(this, initialNode.name, 10,10);
        getLayer().addChild(view.node);
        nodesAdded.add(initialNode);
        
        double y = 40;
        double x = 10;
        
        for(HierarchyNodeModel node: initialNode.children){
            view = new HierarchyNodeView(this, node.name, x,y);
            PBounds bounds = view.node.getBounds();
            PSWTPath path = new PSWTPath();
            path.setPathToPolyline(new float[] { 25, (float) (x+(bounds.width/2.0f)) }, new float[] { 25, 40 });
            getLayer().addChild(path);
            
            x = bounds.x+bounds.width+2;
            getLayer().addChild(view.node);
            path.moveToBack();
        }

    }
}










