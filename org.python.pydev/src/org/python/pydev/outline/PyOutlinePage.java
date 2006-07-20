/*
 * Author: atotic
 * Created: Jul 10, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import java.net.MalformedURLException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.commentType;
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
 **/
public class PyOutlinePage extends ContentOutlinePage  {

	private static final String PREF_ALPHA_SORT = "org.python.pydev.OUTLINE_ALPHA_SORT";

    private static final String PREF_HIDE_COMMENTS = "org.python.pydev.OUTLINE_HIDE_COMMENTS";
    
    private static final String PREF_HIDE_IMPORTS = "org.python.pydev.OUTLINE_HIDE_IMPORTS";

    ViewerSorter sortByNameSorter;

	PyEdit editorView;
	IDocument document;
	IOutlineModel model;
	ImageCache imageCache;
	
	// listeners to rawPartition
	ISelectionChangedListener selectionListener;

    private ViewerFilter hideCommentsFilter;

    private ViewerFilter hideImportsFilter;

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
			getTreeViewer().removeSelectionChangedListener(selectionListener);
		}
		if (imageCache != null) {
			imageCache.dispose();
		}
		super.dispose();
	}

	/*
	 * Raw partition creates an outline that shows document's partitions
	 * created by PyPartitionScanner: strings/commments
	 * 
	 * here we implement all the event handlers for the viewer (selection, document updates)
	 */
	public void createRawPartitionOutline() { // public to suppress the warnings, otherwise not currently used
		final TreeViewer tree = getTreeViewer();
		IDocumentProvider provider = editorView.getDocumentProvider();
		document = provider.getDocument(editorView.getEditorInput());
		model = new RawPartitionModel(this, document);
		tree.setContentProvider(new RawPartitionContentProvider());
		tree.setLabelProvider(new RawPartitionLabelProvider(document));
		tree.setInput(model.getRoot());		
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
	
	/*
	 * called when model has structural changes, refreshes all items underneath
	 * @param items: items to refresh, or null for the whole tree
	 * tries to preserve the scrolling
	 */
	public void refreshItems(Object[] items) {
		try {
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
        }
	}
	public void refreshAll() {
	}
	
	/**
	 * called when a single item changes
	 */
	public void updateItems(Object[] items) {
        if(isDisposed()){
        	return;
        }
		TreeViewer tree = getTreeViewer();
		if (tree != null){
			tree.update(items, null);
		}
	}

    /**
     * Should we hide the comments?
     */
    protected void setHideComments(boolean doHideComments) {
        getStore().setValue(PREF_HIDE_COMMENTS, doHideComments);
        ViewerFilter filter = getHideCommentsFilter();
        if(doHideComments){
            getTreeViewer().addFilter(filter);
        }else{
            getTreeViewer().removeFilter(filter);
        }
    }

    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideCommentsFilter() {
        if(hideCommentsFilter == null){
            hideCommentsFilter = new ViewerFilter(){
       
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if(element instanceof ParsedItem){
                        ParsedItem item = (ParsedItem) element;
                        if(!(item.astThis.node instanceof commentType)){
                            return true;
                        }
                        
                    }
                    return false;
                }
                
            };
        }
        return hideCommentsFilter;
    }

    
    /**
     * Should we hide the comments?
     */
    protected void setHideImports(boolean doHideImports) {
        getStore().setValue(PREF_HIDE_IMPORTS, doHideImports);
        ViewerFilter filter = getHideImportsFilter();
        if(doHideImports){
            getTreeViewer().addFilter(filter);
        }else{
            getTreeViewer().removeFilter(filter);
        }
    }
    
    /**
     * @return the filter used to hide comments
     */
    private ViewerFilter getHideImportsFilter() {
        if(hideImportsFilter == null){
            hideImportsFilter = new ViewerFilter(){
                
                @Override
                public boolean select(Viewer viewer, Object parentElement, Object element) {
                    if(element instanceof ParsedItem){
                        ParsedItem item = (ParsedItem) element;
                        SimpleNode n = item.astThis.node;
                        if(n instanceof ImportFrom || n instanceof Import){
                            return false;
                        }
                        
                    }
                    return true;
                }
                
            };
        }
        return hideImportsFilter;
    }
    

    
	/**
	 * @param doSort : sort or not?
	 */
	public void setAlphaSort(boolean doSort) {
        getStore().setValue(PREF_ALPHA_SORT, doSort);
		if (sortByNameSorter == null) {
			sortByNameSorter = new ViewerSorter() {
				@SuppressWarnings("unchecked")
                public int compare(Viewer viewer, Object e1, Object e2) {
					return ((Comparable)e1).compareTo(e2);
				}
			};
		}
		getTreeViewer().setSorter(doSort ? sortByNameSorter : null);
	}

    /**
     * @return the preference store we should use
     */
    private IPreferenceStore getStore() {
        return PydevPlugin.getDefault().getPreferenceStore();
    }
	
	private void createActions() {
		//---- Sort by name
		Action sortByName = new Action("Sort by name", IAction.AS_CHECK_BOX ) {
			public void run() {
				setAlphaSort(isChecked());
			}
		};
		sortByName.setToolTipText("Sort by name");
        
		
		//---- Hide comments
		Action hideComments = new Action("Hide Comments", IAction.AS_CHECK_BOX) {
		    public void run() {
		        setHideComments(isChecked());
		    }
		};
		
		
		//---- Hide comments
		Action hideImports = new Action("Hide Imports", IAction.AS_CHECK_BOX) {
		    public void run() {
		        setHideImports(isChecked());
		    }
		};
        
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
		
		restorePreferences(sortByName, hideComments, hideImports);
		try {
			sortByName.setImageDescriptor(imageCache.getDescriptor(UIConstants.ALPHA_SORT));
			collapseAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.COLLAPSE_ALL));
			expandAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.EXPAND_ALL));
            hideComments.setImageDescriptor(imageCache.getDescriptor(UIConstants.COMMENT_BLACK));
            hideImports.setImageDescriptor(imageCache.getDescriptor(UIConstants.IMPORT_ICON));
		} catch (MalformedURLException e) {
			System.err.println("missing icon");
			e.printStackTrace();
		}

		// Add actions to the toolbar
		IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
		toolbarManager.add(sortByName);
		toolbarManager.add(hideComments);
		toolbarManager.add(hideImports);
		toolbarManager.add(collapseAll);
		toolbarManager.add(expandAll);
	}

    
    /**
     * @param sortByName the action to sort by name
     * @param hideComments the action to hide the comments
     * @param hideImports the action to hide the imports
     */
    private void restorePreferences(Action sortByName, Action hideComments, Action hideImports) {
        sortByName.setChecked(getStore().getBoolean(PREF_ALPHA_SORT));
        hideComments.setChecked(getStore().getBoolean(PREF_HIDE_COMMENTS));
        hideImports.setChecked(getStore().getBoolean(PREF_HIDE_IMPORTS));
        
        setAlphaSort(sortByName.isChecked());
        setHideComments(hideComments.isChecked());
        setHideImports(hideImports.isChecked());
    }
	
    /**
	 * create the outline view widgets
	 */
	public void createControl(Composite parent) {
		super.createControl(parent); // this creates a tree viewer
		try{
    		createParsedOutline();
    		// selecting an item in the outline scrolls the document
    		final TreeViewer tree = getTreeViewer();
    		selectionListener = new ISelectionChangedListener() {
    			public void selectionChanged(SelectionChangedEvent event) {
    				StructuredSelection sel = (StructuredSelection)tree.getSelection();
    				SimpleNode node = model.getSelectionPosition(sel);
    				editorView.revealModelNode(node);
    			}
    		};
    		tree.addSelectionChangedListener(selectionListener);	
            createActions();
        }catch(Throwable e){
            PydevPlugin.log(e);
        }
	}

}
