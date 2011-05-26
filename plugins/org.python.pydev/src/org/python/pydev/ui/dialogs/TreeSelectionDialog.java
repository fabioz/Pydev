/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 15, 2006
 */
package org.python.pydev.ui.dialogs;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.python.pydev.core.StringMatcher;

/**
 * This class extends the 'default' element tree selection dialog so that the user is able to filter the matches
 * on the tree (As the org.eclipse.ui.dialogs.ElementListSelectionDialog).
 * 
 * @author Fabio
 */
public class TreeSelectionDialog extends ElementTreeSelectionDialog{

    private ILabelProvider labelProvider;
    protected DefaultFilterMatcher fFilterMatcher = new DefaultFilterMatcher();
    protected ITreeContentProvider contentProvider;
    protected String initialFilter = "";
    
    /**
     * Give subclasses a chance to decide if they want to update the contents of the tree in a thread or not. 
     */
    protected boolean updateInThread = true;
    
    
    class UpdateJob extends Thread{
        IProgressMonitor monitor = new NullProgressMonitor(); //only thing it implements is the canceled
        
        public UpdateJob(){
            setPriority(Thread.MIN_PRIORITY);
            setName("TreeSelectionDialog: UpdateJob");
        }
        @Override
        public void run() {
            try {
                sleep(300);
            } catch (InterruptedException e) {
                //ignore
            }
            if(!monitor.isCanceled()){
                Display display = Display.getDefault();
                display.asyncExec(new Runnable(){
    
                    public void run() {
                        if(!monitor.isCanceled()){
                            doFilterUpdate(monitor);
                        }
                    }
                    
                });
            }
        }
		public void cancel(){
            this.monitor.setCanceled(true);
        }
    }
    
    /**
     * Updates the current filter with the text field text.
     */
    protected void doFilterUpdate(IProgressMonitor monitor) {
        if(text != null && !text.isDisposed()){
            //Must check if it's disposed, as this will be run asynchronously.
        	setFilter(text.getText(), monitor, true);
        	onFinishUpdateJob();
        }
    }

    /**
     * Subclasses may override to do something when the update job is finished.
     */
    protected void onFinishUpdateJob() {
    	
    }
    
    public TreeSelectionDialog(Shell parent, ILabelProvider labelProvider, ITreeContentProvider contentProvider) {
        super(parent, labelProvider, contentProvider);

        this.labelProvider = labelProvider;
        this.contentProvider = contentProvider;
    }
    
    public void setInitialFilter(String initialFilter){
        this.initialFilter = initialFilter;
    }
    
    private int fWidth = 60;
    protected Text text;
    private UpdateJob updateJob;


    @Override
    protected Control createDialogArea(Composite parent) {
        Control control = super.createDialogArea(parent);
        getTreeViewer().expandAll();
        
        getTreeViewer().addFilter(new ViewerFilter(){

            @Override
            public boolean select(Viewer viewer, Object parentElement, Object element) {
                return matchItemToShowInTree(element);
            }}
        );
        getTreeViewer().setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
        getTreeViewer().expandAll();

        if(this.initialFilter.length() > 0){
            this.text.setText(this.initialFilter);
            this.text.setSelection(this.initialFilter.length());
            this.setFilter(this.initialFilter, new NullProgressMonitor(), true);
        }
        
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
            	if(updateInThread){
	                if(updateJob != null){
	                    updateJob.cancel(); //cancel it if it was already in progress
	                }
	                updateJob = new UpdateJob();
	                updateJob.start();
            	}else{
            		doFilterUpdate(new NullProgressMonitor());
            	}
            }

        };
        text.addListener(SWT.Modify, listener);

        text.addKeyListener(new KeyListener() {
            public void keyPressed(KeyEvent e) {
                if (e.keyCode == SWT.ARROW_DOWN){
                    Tree tree = getTreeViewer().getTree();
					tree.setFocus();
                    updateSelectionIfNothingSelected(tree);
                }
            }

            public void keyReleased(KeyEvent e) {
            }
        });

        return label;
    }

    
    
    //filtering things...
    protected void setFilter(String text, IProgressMonitor monitor, boolean updateFilterMatcher) {
        if(monitor.isCanceled())
            return;
        
        if(updateFilterMatcher){
        	//just so that subclasses may already treat it.
	        if(fFilterMatcher.lastPattern.equals(text)){
	            //no actual change...
	            return;
	        }
	        fFilterMatcher.setFilter(text);
	        if(monitor.isCanceled())
	            return;
        }

        getTreeViewer().getTree().setRedraw(false);
        getTreeViewer().getTree().getParent().setRedraw(false);
        try {
            if(monitor.isCanceled())
                return;
            getTreeViewer().refresh();
            if(monitor.isCanceled())
                return;
            getTreeViewer().expandAll();
        } finally {
            getTreeViewer().getTree().setRedraw(true);
            getTreeViewer().getTree().getParent().setRedraw(true);
        }
    }
    
    protected class DefaultFilterMatcher {
        public StringMatcher fMatcher;
        public String lastPattern;

        public DefaultFilterMatcher(){
            setFilter("");
            
        }
        public void setFilter(String pattern) {
            setFilter(pattern, true, false);
        }
        
        private void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
            fMatcher = new StringMatcher(pattern + '*', ignoreCase, ignoreWildCards);
            this.lastPattern = pattern;
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

    
    /*
     * @see SelectionStatusDialog#computeResult()
     */
    @SuppressWarnings("unchecked")
    protected void computeResult() {
        IStructuredSelection selection = (IStructuredSelection) getTreeViewer().getSelection();
        List list = selection.toList();
        if(list.size() > 0){
            setResult(list);
        }else{
            TreeItem[] items = getTreeViewer().getTree().getItems();
            if(items.length == 1){
                //there is only one item filtered in the tree.
                list = new ArrayList();
                list.add(items[0].getData());
                setResult(list);
            }
        }
    }

    /**
     * In the default implementation, an item goes to the tree if the filter can properly match
     * it (but subclasses may override if their understanding of what goes into the tree is
     * not decided solely by that).
     */
	protected boolean matchItemToShowInTree(Object element) {
		return fFilterMatcher.match(element);
	}

	protected void updateSelectionIfNothingSelected(Tree tree) {
		TreeItem[] sel = tree.getSelection();
		if(sel == null || sel.length == 0){
			TreeItem[] items = tree.getItems();
			if(items != null && items.length > 0){
				tree.setSelection(items[0]);
			}
		}
	}


}
