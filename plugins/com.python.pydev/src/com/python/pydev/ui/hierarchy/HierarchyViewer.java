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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.swt.widgets.Composite;
import org.python.pydev.core.Tuple;

import edu.umd.cs.piccolo.event.PBasicInputEventHandler;
import edu.umd.cs.piccolo.event.PInputEvent;
import edu.umd.cs.piccolo.util.PBounds;
import edu.umd.cs.piccolox.swt.PSWTCanvas;
import edu.umd.cs.piccolox.swt.PSWTPath;

/**
 * This class is used to view the hierarchy layout of some kind.
 * 
 * Its implementation will allow viewing of simple connectivity for viewing a class hierarchy.
 */
public class HierarchyViewer extends PSWTCanvas{
    public Set<HierarchyNodeView> allAdded = new HashSet<HierarchyNodeView>();

    public HierarchyViewer(Composite parent, int args) {
        super(parent, args);
    }

    /**
     * Sets the hierarchy initializing on the 'node of interest', that is the initial node here.
     */
    public void setHierarchy(HierarchyNodeModel initialNode){
        this.getLayer().setTransform(new AffineTransform()); //default transform
        this.getLayer().removeAllChildren(); //clear all, as we're setting from the beggining
        allAdded.clear();
        if(initialNode == null){
            initialNode = new HierarchyNodeModel("Invalid");
        }
        
        Set<HierarchyNodeModel> nodesAdded = new HashSet<HierarchyNodeModel>();
        double y = 10;
        double x = 10;

        HierarchyNodeView initial = new HierarchyNodeView(this, initialNode, x,y, Color.LIGHT_GRAY);
        allAdded.add(initial);
        getLayer().addChild(initial.node);
        nodesAdded.add(initialNode);
        final double deltaY = 40;
        
        List<HierarchyNodeView> nodesToAdd = new ArrayList<HierarchyNodeView>();
        nodesToAdd.add(initial);
        addWithDelta(y+deltaY, x, nodesToAdd, deltaY, true);

        nodesToAdd = new ArrayList<HierarchyNodeView>();
        nodesToAdd.add(initial);
        double lastY = addWithDelta(y-deltaY, x, nodesToAdd, -deltaY, false);
        
        this.getLayer().translate(0, -lastY-deltaY-y);
    }

    private double addWithDelta(double y, double initialX, List<HierarchyNodeView> nodesToAdd, double yDelta, boolean addChildren) {
        List<HierarchyNodeView> newRound;
        do{
            double x = initialX;
            newRound  = new ArrayList<HierarchyNodeView>();
            for(HierarchyNodeView v : nodesToAdd){
                Tuple<List<HierarchyNodeView>, Double> tup = addNodesFor(v, addChildren ? v.model.children : v.model.parents, y, x, addChildren);
                for(HierarchyNodeView added : tup.o1){
                    if(!newRound.contains(added)){
                        newRound.add(added);
                    }
                }
                x = tup.o2;
            }
            nodesToAdd = newRound;
            y += yDelta;
            
        }while(newRound.size() > 0);
        return y;
    }

    private Tuple<List<HierarchyNodeView>, Double> addNodesFor(HierarchyNodeView initial, List<HierarchyNodeModel> toAdd, double y, double x, boolean addChildren) {
        ArrayList<HierarchyNodeView> ret = new ArrayList<HierarchyNodeView>();
        HierarchyNodeView view;
        for(HierarchyNodeModel node: toAdd){
            view = new HierarchyNodeView(this, node, x,y);
            if(!allAdded.contains(view)){
                PBounds bounds = addNode(initial, view, addChildren);
                
                x = bounds.x+bounds.width+10;
                getLayer().addChild(view.node);
                ret.add(view);
                allAdded.add(view);
            }else{
                for(HierarchyNodeView added : allAdded){
                    if(added.equals(view)){
                        //we have to get this way because the equals is only from the model point...
                        //which means that we have to get the position of that one now
                        addNode(initial, added, addChildren);
                        break;
                    }
                }
            }
        }
        return new Tuple<List<HierarchyNodeView>, Double>(ret, x);
    }

    private PBounds addNode(HierarchyNodeView from, HierarchyNodeView toNode, boolean addChildren) {
        PBounds bounds = toNode.node.getBounds();
        
        final PSWTPath path = new PSWTPath();
        Point2D to = toNode.node.getCenter();
        Point2D fromP = from.node.getCenter();
        if(!addChildren){
            fromP.setLocation(fromP.getX(), fromP.getY() - (from.node.getHeight()/2.0));
            to.setLocation(to.getX(), to.getY() + (toNode.node.getHeight()/2.0));
        }else{
            fromP.setLocation(fromP.getX(), fromP.getY() + (from.node.getHeight()/2.0));
            to.setLocation(to.getX(), to.getY() - (toNode.node.getHeight()/2.0));
        }
        
        path.setPathToPolyline(new Point2D[]{fromP, to});
        getLayer().addChild(path);
        path.moveToBack();
        path.addInputEventListener(new PBasicInputEventHandler(){
            @Override
            public void mouseEntered(PInputEvent event) {
                path.setStrokeColor(Color.LIGHT_GRAY);
            }
            @Override
            public void mouseExited(PInputEvent event) {
                path.setStrokeColor(Color.BLACK);
            }
        });
        return bounds;
    }
}










