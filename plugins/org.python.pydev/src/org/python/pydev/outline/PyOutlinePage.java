/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Author: fabioz
 * 
 * Created: Jul 10, 2003
 */
package org.python.pydev.outline;

import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.bundle.ImageCache;
import org.python.pydev.core.callbacks.CallbackWithListeners;
import org.python.pydev.core.callbacks.ICallbackWithListeners;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.core.log.Log;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.parser.ErrorDescription;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.plugin.PydevPlugin;
import org.python.pydev.ui.IViewCreatedObserver;
import org.python.pydev.ui.UIConstants;

/**
 * Outline page, displays the structure of the document in the editor window. 
 *
 * Partition outlining:<p>
 * PyDocumentProvider already partitions the document into strings/comments/other<p>
 * RawPartition is the simplest outline that shows this "raw" document partitioning<p>
 * raw partition was only used as an example, not useful in production<p>
 * 
 * @note: tests for the outline page are not directly for the outline page, but for its model, 
 * based on ParsedItems.
 **/
public class PyOutlinePage extends ContentOutlinePageWithFilter implements IShowInTarget, IAdaptable{

    PyEdit editorView;
    IDocument document;
    IOutlineModel model;
    ImageCache imageCache;
    
    // listeners to rawPartition
    ISelectionChangedListener selectionListener;
    
    private OutlineLinkWithEditorAction linkWithEditor;
	public final ICallbackWithListeners<TreeViewer> onTreeViewerCreated = new CallbackWithListeners<TreeViewer>();
	public final ICallbackWithListeners<PyOutlinePage> onDispose = new CallbackWithListeners<PyOutlinePage>();

    public PyOutlinePage(PyEdit editorView) {
        super();
        List<IViewCreatedObserver> participants = ExtensionHelper.getParticipants(
				ExtensionHelper.PYDEV_VIEW_CREATED_OBSERVER);
		for (IViewCreatedObserver iViewCreatedObserver : participants) {
			iViewCreatedObserver.notifyViewCreated(this);
		}
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
        onDispose.call(this);
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
        } catch (Throwable e) {
            //things may be disposed...
            Log.log(e);
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
        
        collapseAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.COLLAPSE_ALL));
        expandAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.EXPAND_ALL));

        // Add actions to the toolbar
        IActionBars actionBars = getSite().getActionBars();
        IToolBarManager toolbarManager = actionBars.getToolBarManager();
        
        toolbarManager.add(new OutlineSortByNameAction(this, imageCache));
        toolbarManager.add(collapseAll);
        toolbarManager.add(expandAll);
        
        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(linkWithEditor);
        menuManager.add(new OutlineHideCommentsAction(this, imageCache));
        menuManager.add(new OutlineHideImportsAction(this, imageCache));
        menuManager.add(new OutlineHideMagicObjectsAction(this, imageCache));
        menuManager.add(new OutlineHideFieldsAction(this, imageCache));
        menuManager.add(new OutlineHideNonPublicMembersAction(this, imageCache));
        menuManager.add(new OutlineHideStaticMethodsAction(this, imageCache));
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
                        
                        boolean alreadySelected = false;
                        if(sel.size() == 1) { // only sync the editing view if it is a single-selection
                            ParsedItem firstElement = (ParsedItem) sel.getFirstElement();
                            ErrorDescription errorDesc = firstElement.getErrorDesc();
                            
                            //select the error
                            if(errorDesc != null && errorDesc.message != null){
                                int len = errorDesc.errorEnd-errorDesc.errorStart;
                                editorView.setSelection(errorDesc.errorStart, len);
                                alreadySelected = true;
                            }
                        }
                        if(!alreadySelected){
                            SimpleNode[] node = model.getSelectionPosition(sel);
                            editorView.revealModelNodes(node);
                        }
                    }finally{
                        relinkAll();
                    }
                }
            };
            addSelectionChangedListener(selectionListener);    
            createActions();
            
            //OK, instead of using the default selection engine, we recreate it only to handle mouse
            //and key events directly, because it seems that sometimes, SWT creates spurious select events
            //when those shouldn't be created, and there's also a risk of creating loops with the selection,
            //as when one selection arrives when we're linked, we have to perform a selection and doing that
            //selection could in turn trigger a new selection, so, we remove that treatment and only start
            //selections from interactions the user did.
            //see: Cursor jumps to method definition when an error is detected
            //https://sourceforge.net/tracker2/?func=detail&aid=2057092&group_id=85796&atid=577329
            TreeViewer treeViewer = getTreeViewer();
            treeViewer.removeSelectionChangedListener(this);
            Tree tree = treeViewer.getTree();
            
            tree.addMouseListener(new MouseListener(){

                public void mouseDoubleClick(MouseEvent e) {
                    tryToMakeSelection();
                }

                public void mouseDown(MouseEvent e) {
                }

                public void mouseUp(MouseEvent e) {
                    tryToMakeSelection();
                }}
            );
            
            tree.addKeyListener(new KeyListener(){

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    if(e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN){
                        tryToMakeSelection();
                    }
                }}
            );
            
        }catch(Throwable e){
            Log.log(e);
        }
        onTreeViewerCreated.call(getTreeViewer());
    }
    
	public boolean show(ShowInContext context) {
        linkWithEditor.doLinkOutlinePosition(this.editorView, this, new PySelection(this.editorView));
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
    
    /**
     * Used to hold a link level to know when it should be unlinked or relinked, as calls can be 'cascaded'
     */
    private int linkLevel = 1;
    
    /**
     * Used for locking link/unlink access.
     */
    private Object lock = new Object();
    
    /**
     * Stops listening to changes (the linkLevel is used so that multiple unlinks can be called and later
     * multiple relinks should be used)
     */
    void unlinkAll() {
        synchronized (lock) {
            linkLevel--;
            if(linkLevel == 0){
                removeSelectionChangedListener(selectionListener);
                if(linkWithEditor != null){
                    linkWithEditor.unlink();
                }
            }
        }
    }

    /**
     * Starts listening to changes again if the number of relinks matches the number of unlinks
     */
    void relinkAll() {
        synchronized (lock) {
            linkLevel++;
            if(linkLevel == 1){
                addSelectionChangedListener(selectionListener);
                if(linkWithEditor != null){
                    linkWithEditor.relink();
                }
            }else if(linkLevel > 1){
                throw new RuntimeException("Error: relinking without unlinking 1st");
            }
        }
    }

    /**
     * Creates an event of a selection change if it's possible to do so (otherwise returns null)
     */
    private SelectionChangedEvent createSelectionEvent() {
        SelectionChangedEvent event = null;
        ISelection selection = getSelection();
        if(selection instanceof IStructuredSelection){
            IStructuredSelection s = (IStructuredSelection) selection;
            if(s.iterator().hasNext()){
                //only make the selection if there's some item selected
                event = new SelectionChangedEvent(getTreeViewer(), selection);
            }
        }
        return event;
    }

    /**
     * Tries to trigger a selection changed event (if a selection is available for doing so)
     */
    private void tryToMakeSelection() {
        SelectionChangedEvent event = createSelectionEvent();
        if(event != null){
            selectionChanged(event);
        }
    }


}
