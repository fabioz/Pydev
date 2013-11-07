/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler 
 */

package ch.hsr.ukistler.astgraph;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;

import org.jgraph.graph.DefaultEdge;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.shared_core.structure.FastStack;

/**
 * Traverse AST and generate the tree (shown by JGraph)
 * 
 * @author Ueli Kistler
 * 
 */
public class GraphVisitor extends VisitorBase {

    private static final int DEPTH_STEP = 30;

    private static final int INDENT_STEP = 40;

    private Color nodeColor;

    private List<DefaultGraphCell> cells;

    private FastStack<DefaultGraphCell> stack;

    int depth;

    int indent;

    public GraphVisitor() throws IOException {
        cells = new ArrayList<DefaultGraphCell>();
        stack = new FastStack<DefaultGraphCell>(50);
        depth = 0;
        indent = 0;
        nodeColor = Color.GRAY;
    }

    private DefaultEdge createConnection(DefaultGraphCell cell) {
        DefaultEdge edge = new DefaultEdge();
        edge.setSource(stack.peek().getChildAt(0));
        edge.setTarget(cell);

        // Set Arrow Style for edge
        int arrow = GraphConstants.ARROW_TECHNICAL;
        GraphConstants.setLineEnd(edge.getAttributes(), arrow);
        GraphConstants.setEndFill(edge.getAttributes(), true);
        return edge;
    }

    public DefaultGraphCell createVertex(String name, double x, double y, Color bg, boolean raised) {

        // Create vertex with the given name
        DefaultGraphCell cell = new DefaultGraphCell(name);

        GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createEtchedBorder());
        GraphConstants.setBounds(cell.getAttributes(), new Rectangle2D.Double(x, y, 10, 10));
        GraphConstants.setResize(cell.getAttributes(), true);
        GraphConstants.setAutoSize(cell.getAttributes(), true);

        // Set fill color
        if (bg != null) {
            GraphConstants.setOpaque(cell.getAttributes(), true);
            GraphConstants.setGradientColor(cell.getAttributes(), bg);
        }

        // Set raised border
        if (raised)
            GraphConstants.setBorder(cell.getAttributes(), BorderFactory.createRaisedBevelBorder());
        else
            // Set black border
            GraphConstants.setBorderColor(cell.getAttributes(), Color.black);

        return cell;
    }

    private void decrementPosition() {
        stack.pop();
        indent -= INDENT_STEP;
    }

    public DefaultGraphCell[] getCells() {
        return cells.toArray(new DefaultGraphCell[0]);
    }

    private void incrementPosition(DefaultGraphCell cell) {
        stack.push(cell);
        indent += INDENT_STEP;
        depth += DEPTH_STEP;
    }

    private void parentAddPort() {
        Point2D point = new Point2D.Double(0, GraphConstants.PERMILLE);
        stack.peek().addPort(point);
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        node.traverse(this);
        decrementPosition();

    }

    @Override
    protected Object unhandled_node(SimpleNode node) throws Exception {
        DefaultGraphCell cell = null;
        String caption = node.toString();

        parentAddPort();

        cell = createVertex(caption, indent, depth, nodeColor, false);
        DefaultEdge edge = createConnection(cell);

        cells.add(cell);
        cells.add(edge);

        incrementPosition(cell);

        return null;
    }

    /**
     * Entry point
     */
    @Override
    public Object visitModule(Module node) throws Exception {
        // String caption = node.toString();
        DefaultGraphCell moduleCell = createVertex("Module", indent, depth, nodeColor, false);
        cells.add(moduleCell);

        incrementPosition(moduleCell);
        traverse(node);

        return null;
    }

}
