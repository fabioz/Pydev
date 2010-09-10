package com.python.pydev.analysis.indexview;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.part.ViewPart;

public class PyIndexView extends ViewPart{

    private Tree tree;
    private TreeViewer treeViewer;

    @Override
    public void createPartControl(Composite parent) {
        tree = new Tree(parent, 0);
        treeViewer = new TreeViewer(tree);
        treeViewer.setContentProvider(new PyIndexContentProvider());
        treeViewer.setInput(new IndexRoot());
        
        tree.addKeyListener(new KeyListener() {
            
            @Override
            public void keyReleased(KeyEvent e) {
                if(e.keyCode == SWT.F5){
                    treeViewer.setInput(new IndexRoot());
                }
            }
            
            @Override
            public void keyPressed(KeyEvent e) {
            }
        });
    }

    @Override
    public void setFocus() {
        this.tree.setFocus();
    }

}
