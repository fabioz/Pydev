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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.jgraph.JGraph;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.DefaultGraphModel;
import org.jgraph.graph.GraphConstants;
import org.jgraph.graph.GraphLayoutCache;
import org.jgraph.graph.GraphModel;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public class GraphView extends JFrame {

    /**
     * 
     */
    private static final long serialVersionUID = -3925214828169513991L;

    private JPanel boxPanel = null;

    private JFileChooser fc = null;

    private JGraph graph = null;

    private JScrollPane graphpane = null;

    private JPanel navPane = null;

    private JButton saveImage = null;

    private JButton loadPython = null;

    /**
     * This method initializes
     * 
     */
    public GraphView() {
        super("Python AST Viewer");
        initialize();
    }

    /**
     * This method initializes this
     * 
     */
    private void initialize() {
        this.fc = new JFileChooser();
        this.setContentPane(getBoxPanel());
        this.setSize(new Dimension(640, 480));
        this.pack();
        this.setExtendedState(javax.swing.JFrame.MAXIMIZED_BOTH);
    }

    /**
     * This method initializes boxPanel
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getBoxPanel() {
        if (boxPanel == null) {
            boxPanel = new JPanel();
            boxPanel.setLayout(new BorderLayout());
            boxPanel.add(getNavPane(), BorderLayout.NORTH);
            boxPanel.add(getGraphpane(), BorderLayout.CENTER);
        }
        return boxPanel;
    }

    /**
     * This method initializes graphpane
     * 
     * @return javax.swing.JScrollPane
     */
    private JScrollPane getGraphpane() {
        if (graphpane == null) {
            graphpane = new JScrollPane();
            this.graph = setupGraph();
            graphpane.setViewportView(this.graph);
        }
        return graphpane;
    }

    /**
     * This method initializes navPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getNavPane() {
        if (navPane == null) {
            navPane = new JPanel();
            navPane.setLayout(new FlowLayout());
            navPane.add(getSaveImage());
            navPane.add(getLoadPython());
        }
        return navPane;
    }

    /**
     * This method initializes saveImage
     * 
     * @return javax.swing.JButton
     */
    private JButton getSaveImage() {
        if (saveImage == null) {
            saveImage = new JButton();
            saveImage.setText("Save as PNG");
            saveImage.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent event) {

                    FileFilter filter = new PNGFilter();
                    try {
                        fc.addChoosableFileFilter(filter);
                        int returnVal = fc.showSaveDialog(GraphView.this);
                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = fc.getSelectedFile();
                            createImage(file.getAbsolutePath());
                        }
                    } finally {
                        fc.removeChoosableFileFilter(filter);
                    }
                }
            });

        }
        return saveImage;
    }

    /**
     * This method initializes loadPython
     * 
     * @return javax.swing.JButton
     */
    private JButton getLoadPython() {
        if (loadPython == null) {
            loadPython = new JButton();
            loadPython.setText("Load Python source");
            loadPython.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent event) {
                    FileFilter filter = new PythonFilter();
                    try {

                        fc.addChoosableFileFilter(filter);

                        int returnVal = fc.showOpenDialog(GraphView.this);

                        if (returnVal == JFileChooser.APPROVE_OPTION) {
                            File file = fc.getSelectedFile();
                            loadGraph(file.getAbsolutePath());
                        }
                    } catch (Throwable e) {
                        JOptionPane msgbox = new JOptionPane("Error writing file: " + e.getMessage());
                        msgbox.setVisible(true);
                    } finally {
                        fc.removeChoosableFileFilter(filter);
                    }
                }
            });
        }
        return loadPython;
    }

    private JGraph setupGraph() {
        // Construct Model and Graph
        GraphModel model = new DefaultGraphModel();
        JGraph graph = new JGraph(model);

        // Control-drag should clone selection
        graph.setCloneable(true);

        // Enable edit without final RETURN keystroke
        graph.setInvokesStopCellEditing(true);

        // When over a cell, jump to its default port (we only have one, anyway)
        graph.setJumpToDefaultPort(true);

        return graph;
    }

    private void loadGraph(String fileName) throws FileNotFoundException, IOException, Throwable {
        ASTGraph ast = new ASTGraph();
        ParseOutput objects = ast.parseFile(fileName);

        graph.setGraphLayoutCache(new GraphLayoutCache());
        DefaultGraphCell[] cells = ast.generateTree((SimpleNode) objects.ast);

        graph.getGraphLayoutCache().insert(cells);
        graph.clearSelection();
    }

    private void createImage(String imageName) {
        if (graph == null) {
            return;
        }
        try {
            ImageWriter writer;
            writer = new ImageWriter(graph.getImage(null, GraphConstants.DEFAULTINSET), imageName);

            SwingUtilities.invokeLater(writer);
        } catch (Throwable e) {
            JOptionPane msgbox = new JOptionPane("Error writing file: " + e.getMessage());
            msgbox.setVisible(true);
        }
    }

    class PNGFilter extends javax.swing.filechooser.FileFilter {
        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            String filename = file.getName();
            return filename.endsWith(".png");
        }

        @Override
        public String getDescription() {
            return "PNG image (*.png)";
        }
    }

    class PythonFilter extends javax.swing.filechooser.FileFilter {

        @Override
        public boolean accept(File file) {
            if (file.isDirectory()) {
                return true;
            }
            String filename = file.getName();
            return filename.endsWith(".py");
        }

        @Override
        public String getDescription() {
            return "Python Source code (*.py)";
        }
    }

} // @jve:decl-index=0:visual-constraint="10,10"
