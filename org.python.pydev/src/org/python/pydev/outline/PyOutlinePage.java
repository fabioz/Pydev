/*
 * Author: atotic
 * Author: fabioz
 * 
 * Created: Jul 10, 2003
 * License: Eclipse Public License v1.0
 */
package org.python.pydev.outline;

import java.net.MalformedURLException;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.ErrorDescription;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.UIConstants;

/**
 * Outline page, displays the structure of the document in the editor window. 
 *
 * Partition outlining:<p>
 * PyDocumentProvider already partitions the document into strings/comments/other<p>
 * RawPartition is the simplest outline that shows this "raw" document partitioning<p>
 * raw partition was only used as an example, not useful in production<p>
 *
 * Design notes:
 * a good (and only one that subclasses ContentOutlinePage) 
 * example of Eclipse's internal outline page is
 * org.eclipse.ui.extenaltools.internal.ant.editor.outline
 * see PlantyEditor, and PlantyContentOutlinePage
 * 
 * 
 * @note: tests for the outline page are not directly for the outline page, but for its model, 
 * based on ParsedItems.
 **/
public class PyOutlinePage extends ContentOutlinePage implements IShowInTarget, IAdaptable{

	PyEdit editorView;
	IDocument document;
	IOutlineModel model;
	ImageCache imageCache;
	
	// listeners to rawPartition
	ISelectionChangedListener selectionListener;
	
    private OutlineLinkWithEditorAction linkWithEditor;

	public PyOutlinePage(PyEdit editorView) {
		super();
		this.editorView = editorView;
		imageCache = new ImageCache(PydevPlugin.getDefault().getBundle().getEntry("/"));
	}
	
	public void dispose() {
		if (model != null) {
			model.dispose();
			model = null;
		}
		if (selectionListener != null) {
			removeSelectionChangedListener(selectionListener);
		}
		if (imageCache != null) {
			imageCache.dispose();
		}
        if(linkWithEditor != null){
            linkWithEditor.dispose();
            linkWithEditor = null;
        }
		super.dispose();
	}


	/**
	 * Parsed partition creates an outline that shows imports/classes/methods
	 */
	private void createParsedOutline() {
		final TreeViewer tree = getTreeViewer();
		IDocumentProvider provider = editorView.getDocumentProvider();
		document = provider.getDocument(editorView.getEditorInput());
		model = getParsedModel();
		tree.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		tree.setContentProvider(new ParsedContentProvider());
		tree.setLabelProvider(new ParsedLabelProvider(imageCache));
		tree.setInput(model.getRoot());
	}

	/**
	 * 
	 * @return the parsed model, so that it can be used elsewhere (in navigation)
	 */
	public ParsedModel getParsedModel() {
		return new ParsedModel(this, editorView);
	}
	
	public boolean isDisposed(){
		return getTreeViewer().getTree().isDisposed();
	}
	
	/**
	 * called when model has structural changes, refreshes all items underneath
	 * @param items: items to refresh, or null for the whole tree
	 * tries to preserve the scrolling
	 */
	public void refreshItems(Object[] items) {
		try {
			unlinkAll();
            TreeViewer viewer = getTreeViewer();
            if (viewer != null) {
                Tree treeWidget = viewer.getTree();
                if(isDisposed()){
                	return;
                }

                ScrollBar bar = treeWidget.getVerticalBar();
                int barPosition = 0;
                if (bar != null) {
                    barPosition = bar.getSelection();
                }
                if (items == null){
                    if(isDisposed()){
                    	return;
                    }
                    viewer.refresh();
                
                }else{
                    if(isDisposed()){
                    	return;
                    }
                    for (int i = 0; i < items.length; i++) {
                        viewer.refresh(items[i]);
                    }
                }
                
                if (barPosition != 0) {
                    bar.setSelection(Math.min(bar.getMaximum(), barPosition));
                }
            }
        } catch (SWTException e) {
            //things may be disposed...
            PydevPlugin.log(e);
        }finally{
        	relinkAll();
        }
	}
	
	/**
	 * called when a single item changes
	 */
	public void updateItems(Object[] items) {
        try {
        	unlinkAll();
			if(isDisposed()){
				return;
			}
			TreeViewer tree = getTreeViewer();
			if (tree != null){
				tree.update(items, null);
			}
		} finally {
			relinkAll();
		}
	}

    
    

    /**
     * @return the preference store we should use
     */
    /*package*/IPreferenceStore getStore() {
        return PydevPlugin.getDefault().getPreferenceStore();
    }
    
    @Override
    public TreeViewer getTreeViewer() {
        return super.getTreeViewer();
    }
	
	private void createActions() {
        linkWithEditor = new OutlineLinkWithEditorAction(this, imageCache);
        
		//---- Collapse all
		Action collapseAll = new Action("Collapse all", IAction.AS_PUSH_BUTTON) {
			public void run() {
				getTreeViewer().collapseAll();
			}
		};
		
		//---- Expand all
		Action expandAll = new Action("Expand all", IAction.AS_PUSH_BUTTON) {
		    public void run() {
                getTreeViewer().expandAll();
		    }
		};
		
		try {
			collapseAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.COLLAPSE_ALL));
			expandAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.EXPAND_ALL));
		} catch (MalformedURLException e) {
            PydevPlugin.log("Missing Icon", e);
		}

        // Add actions to the toolbar
		IActionBars actionBars = getSite().getActionBars();
		IToolBarManager toolbarManager = actionBars.getToolBarManager();
        
		toolbarManager.add(new OutlineSortByNameAction(this, imageCache));
		toolbarManager.add(new OutlineHideCommentsAction(this, imageCache));
		toolbarManager.add(new OutlineHideImportsAction(this, imageCache));
		toolbarManager.add(collapseAll);
		toolbarManager.add(expandAll);
        
        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(linkWithEditor);
	}

    
    /**
	 * create the outline view widgets
	 */
	public void createControl(Composite parent) {
		super.createControl(parent); // this creates a tree viewer
		try{
    		createParsedOutline();
    		// selecting an item in the outline scrolls the document
    		selectionListener = new ISelectionChangedListener() {

				public void selectionChanged(SelectionChangedEvent event) {
                    if(linkWithEditor == null){
                        return;
                    }
                    try{
                    	unlinkAll();
	    				StructuredSelection sel = (StructuredSelection)event.getSelection();
	                    if(sel.size() == 1) { // only sync the editing view if it is a single-selection
	                        ParsedItem firstElement = (ParsedItem) sel.getFirstElement();
	                        ErrorDescription errorDesc = firstElement.getErrorDesc();
                            if(errorDesc != null && errorDesc.message != null){
	                            int len = errorDesc.errorEnd-errorDesc.errorStart;
	                            editorView.setSelection(errorDesc.errorStart, len);
	                            return;
	                        }
	                    }
	    				SimpleNode[] node = model.getSelectionPosition(sel);
	    				editorView.revealModelNodes(node);
                    }finally{
                    	relinkAll();
                    }
    			}
    		};
    		addSelectionChangedListener(selectionListener);	
            createActions();
        }catch(Throwable e){
            PydevPlugin.log(e);
        }
	}

    public boolean show(ShowInContext context) {
        linkWithEditor.doLinkOutlinePosition(this.editorView, this);
        return true;
    }

    public Object getAdapter(Class adapter) {
        if(adapter == IShowInTarget.class){
            return this;
        }
        return null;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
    	super.selectionChanged(event);
    }
    
	void unlinkAll() {
		removeSelectionChangedListener(selectionListener);
		if(linkWithEditor != null){
			linkWithEditor.unlink();
		}
	}

	void relinkAll() {
		addSelectionChangedListener(selectionListener);
		if(linkWithEditor != null){
			linkWithEditor.relink();
		}
	}


}
