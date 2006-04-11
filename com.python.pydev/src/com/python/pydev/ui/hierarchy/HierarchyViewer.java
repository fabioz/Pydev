/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.awt.geom.Point2D;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;

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

        HierarchyNodeView initial = new HierarchyNodeView(this, initialNode, 10,10);
        getLayer().addChild(initial.node);
        nodesAdded.add(initialNode);
        
        double y = 40;
        double x = 10;
        
        Set<HierarchyNodeView> nodesToAdd = new HashSet<HierarchyNodeView>();
        nodesToAdd.add(initial);
        
        for(HierarchyNodeView v : nodesToAdd){
        	addNodesFor(initialNode, initial, y, x);
        }
        

    }

	private Set<HierarchyNodeView> addNodesFor(HierarchyNodeModel initialNode, HierarchyNodeView initial, double y, double x) {
		HashSet<HierarchyNodeView> ret = new HashSet<HierarchyNodeView>();
		HierarchyNodeView view;
		for(HierarchyNodeModel node: initialNode.children){
            view = new HierarchyNodeView(this, node, x,y);
            
            PBounds bounds = addNode(initial, view);
            
            x = bounds.x+bounds.width+2;
            getLayer().addChild(view.node);
            ret.add(view);
        }
		return ret;
	}

	private PBounds addNode(HierarchyNodeView from, HierarchyNodeView view) {
		PBounds bounds = view.node.getBounds();
		
		PSWTPath path = new PSWTPath();
		path.setPathToPolyline(new Point2D[]{from.node.getCenter(), view.node.getCenter()});
		getLayer().addChild(path);
		path.moveToBack();
		return bounds;
	}
}










