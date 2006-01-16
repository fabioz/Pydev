/*
 * Created on Jan 15, 2006
 */
package com.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;

/**
 * This class extends the 'default' element tree selection dialog so that the user is able to filter the matches
 * on the tree (As the org.eclipse.ui.dialogs.ElementListSelectionDialog).
 * 
 * @author Fabio
 */
public class TreeSelectionDialog extends ElementTreeSelectionDialog{

    private ILabelProvider labelProvider;
    private DefaultFilterMatcher fFilterMatcher = new DefaultFilterMatcher();
    private ITreeContentProvider contentProvider;


    public TreeSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
        super(parent, labelProvider, contentProvider);
        this.labelProvider = labelProvider;
        this.contentProvider = contentProvider;
    }
    
    private int fWidth = 60;
    private Text text;


    @Override
    protected Control createDialogArea(Composite parent) {
        Control control = super.createDialogArea(parent);
        getTreeViewer().expandAll();
        
        getTreeViewer().addFilter(new ViewerFilter(){

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return fFilterMatcher.match(element);
            }}
        );
        getTreeViewer().setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
        getTreeViewer().expandAll();

        return control;
    }
    
    @Override
    protected Label createMessageArea(Composite composite) {
        Label label = super.createMessageArea(composite);
        
        //ok, after the label, we have to create the edit so that the user can filter the results
        text = new Text(composite, SWT.BORDER);
        text.setFont(composite.getFont());
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.widthHint = convertWidthInCharsToPixels(fWidth);
        text.setLayoutData(data);
        
        
        Listener listener = new Listener() {
            public void handleEvent(Event e) {
                setFilter(text.getText());
            }

        };
        text.addListener(SWT.Modify, listener);

        text.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN)
                    getTreeViewer().getTree().setFocus();
            }

            public void keyReleased(KeyEvent e) {
            }
        });

        return label;
    }

    
    
    //filtering things...
    protected void setFilter(String text) {
        fFilterMatcher.setFilter(text);
        getTreeViewer().getTree().setRedraw(false);
        try {
            getTreeViewer().refresh();
            getTreeViewer().expandAll();
        } finally {
            getTreeViewer().getTree().setRedraw(true);
        }
    }
    
    private class DefaultFilterMatcher {
        public StringMatcher fMatcher;

        public DefaultFilterMatcher(){
            setFilter("");
            
        }
        public void setFilter(String pattern) {
            setFilter(pattern, true, false);
        }
        
        private void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
            fMatcher = new StringMatcher(pattern + '*', ignoreCase, ignoreWildCards);
        }

        public boolean match(Object element) {
            boolean match = fMatcher.match(labelProvider.getText(element));
            if(match){
                return true;
            }
            List<Object> allChildren = getAllChildren(element);
            for (Object object : allChildren) {
                if(fMatcher.match(labelProvider.getText(object))){
                    return true;
                }
                
            }
            return false;
        }
    }
    
    private List<Object> getAllChildren(Object element){
        ArrayList<Object> list = new ArrayList<Object>();
        
        Object[] children = contentProvider.getChildren(element);
        if(children == null){
            return list;
        }
        for (Object object : children) {
            list.add(object);
            list.addAll(getAllChildren(object));
        }
        return list;
    }


}
