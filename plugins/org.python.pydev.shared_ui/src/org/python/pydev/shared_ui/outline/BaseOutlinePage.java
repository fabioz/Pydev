/******************************************************************************
* Copyright (C) 2003-2013  Aleksandar Totic and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Aleksandar Totic                      - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
*     Jonah Graham <jonah@kichwacoders.com> - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_ui.outline;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.IDocument;
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
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.IShowInTarget;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.python.pydev.shared_core.callbacks.CallbackWithListeners;
import org.python.pydev.shared_core.callbacks.ICallbackWithListeners;
import org.python.pydev.shared_core.log.Log;
import org.python.pydev.shared_core.model.ErrorDescription;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_ui.EditorUtils;
import org.python.pydev.shared_ui.ImageCache;
import org.python.pydev.shared_ui.UIConstants;
import org.python.pydev.shared_ui.editor.BaseEditor;

@SuppressWarnings({ "rawtypes", "unchecked" })
public abstract class BaseOutlinePage extends ContentOutlinePageWithFilter implements IShowInTarget, IAdaptable {

    public abstract IPreferenceStore getStore();

    protected IDocument document;

    //Important: it must be final (i.e.: never change)
    protected final IOutlineModel model;

    protected final ImageCache imageCache;

    // listeners to rawPartition
    protected ISelectionChangedListener selectionListener;

    protected BaseEditor editorView;

    protected OutlineLinkWithEditorAction linkWithEditor;

    public final ICallbackWithListeners onControlCreated = new CallbackWithListeners();

    public final ICallbackWithListeners onControlDisposed = new CallbackWithListeners();

    protected List createdCallbacksForControls;

    protected final String pluginId;

    public BaseOutlinePage(BaseEditor editorView, ImageCache imageCache, String pluginId) {
        this.imageCache = imageCache;
        this.editorView = editorView;
        this.pluginId = pluginId;
        this.model = (IOutlineModel) editorView.getAdapter(IOutlineModel.class);
        this.model.setOutlinePage(this);
    }

    public IOutlineModel getOutlineModel() {
        return model;
    }

    public BaseEditor getEditor() {
        return editorView;
    }

    /**
     * Parsed partition creates an outline that shows imports/classes/methods
     */
    protected void createParsedOutline() {
        final TreeViewer tree = getTreeViewer();
        IDocumentProvider provider = editorView.getDocumentProvider();
        document = provider.getDocument(editorView.getEditorInput());
        tree.setAutoExpandLevel(2);
        tree.setContentProvider(new ParsedContentProvider());
        tree.setLabelProvider(new ParsedLabelProvider(imageCache));
        tree.setInput(getOutlineModel().getRoot());
    }

    public boolean isDisconnectedFromTree() {
        TreeViewer treeViewer2 = getTreeViewer();
        if (treeViewer2 == null) {
            return true;
        }
        Tree tree = treeViewer2.getTree();
        if (tree == null) {
            return true;
        }
        return tree.isDisposed();
    }

    @Override
    public void dispose() {
        onControlDisposed.call(getTreeViewer());
        if (createdCallbacksForControls != null) {
            for (Object o : createdCallbacksForControls) {
                onControlDisposed.call(o);
            }
            createdCallbacksForControls = null;
        }
        //note: don't dispose on the model (we don't have ownership for it).
        if (selectionListener != null) {
            removeSelectionChangedListener(selectionListener);
        }
        //Note: not disposing of the image cache (the 'global' one is meant to be used). 
        //        if (imageCache != null) {
        //            imageCache.dispose();
        //        }
        if (linkWithEditor != null) {
            linkWithEditor.dispose();
            linkWithEditor = null;
        }
        super.dispose();
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
                if (isDisconnectedFromTree()) {
                    return;
                }

                ScrollBar bar = treeWidget.getVerticalBar();
                int barPosition = 0;
                if (bar != null) {
                    barPosition = bar.getSelection();
                }
                if (items == null) {
                    if (isDisconnectedFromTree()) {
                        return;
                    }
                    viewer.refresh();

                } else {
                    if (isDisconnectedFromTree()) {
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
        } finally {
            relinkAll();
        }
    }

    /**
     * called when a single item changes
     */
    public void updateItems(Object[] items) {
        try {
            unlinkAll();
            if (isDisconnectedFromTree()) {
                return;
            }
            TreeViewer tree = getTreeViewer();
            if (tree != null) {
                tree.update(items, null);
            }
        } finally {
            relinkAll();
        }
    }

    /**
     * Used to hold a link level to know when it should be unlinked or relinked, as calls can be 'cascaded'
     */
    private int linkLevel = 1;

    /**
     * Used for locking link/unlink access.
     */
    private final Object linkLock = new Object();

    /**
     * Stops listening to changes (the linkLevel is used so that multiple unlinks can be called and later
     * multiple relinks should be used)
     */
    public void unlinkAll() {
        synchronized (linkLock) {
            linkLevel--;
            if (linkLevel == 0) {
                removeSelectionChangedListener(selectionListener);
                if (linkWithEditor != null) {
                    linkWithEditor.unlink();
                }
            }
        }
    }

    /**
     * Starts listening to changes again if the number of relinks matches the number of unlinks
     */
    public void relinkAll() {
        synchronized (linkLock) {
            linkLevel++;
            if (linkLevel == 1) {
                addSelectionChangedListener(selectionListener);
                if (linkWithEditor != null) {
                    linkWithEditor.relink();
                }
            } else if (linkLevel > 1) {
                throw new RuntimeException("Error: relinking without unlinking 1st");
            }
        }
    }

    protected void createActions() {
        linkWithEditor = new OutlineLinkWithEditorAction(this, imageCache, pluginId);

        //---- Collapse all
        Action collapseAll = new Action("Collapse all", IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                TreeViewer treeViewer2 = getTreeViewer();
                Tree tree = treeViewer2.getTree();
                tree.setRedraw(false);
                try {
                    getTreeViewer().collapseAll();
                } finally {
                    tree.setRedraw(true);
                }
            }
        };

        //---- Expand all
        Action expandAll = new Action("Expand all", IAction.AS_PUSH_BUTTON) {
            @Override
            public void run() {
                TreeViewer treeViewer2 = getTreeViewer();
                Tree tree = treeViewer2.getTree();
                tree.setRedraw(false);
                try {
                    treeViewer2.expandAll();
                } finally {
                    tree.setRedraw(true);
                }
            }
        };

        collapseAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.COLLAPSE_ALL));
        collapseAll.setId("outline.page.collapse");
        expandAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.EXPAND_ALL));
        expandAll.setId("outline.page.expand");

        // Add actions to the toolbar
        IActionBars actionBars = getSite().getActionBars();
        IToolBarManager toolbarManager = actionBars.getToolBarManager();

        OutlineSortByNameAction action = new OutlineSortByNameAction(this, imageCache, pluginId);
        action.setId("outline.page.sort");
        toolbarManager.add(action);
        toolbarManager.add(collapseAll);
        toolbarManager.add(expandAll);

        IMenuManager menuManager = actionBars.getMenuManager();
        menuManager.add(linkWithEditor);
    }

    /**
     * create the outline view widgets
     */
    @Override
    public void createControl(Composite parent) {
        super.createControl(parent); // this creates a tree viewer
        try {
            createParsedOutline();
            // selecting an item in the outline scrolls the document
            selectionListener = new ISelectionChangedListener() {

                public void selectionChanged(SelectionChangedEvent event) {
                    if (linkWithEditor == null) {
                        return;
                    }
                    try {
                        unlinkAll();
                        StructuredSelection sel = (StructuredSelection) event.getSelection();

                        boolean alreadySelected = false;
                        if (sel.size() == 1) { // only sync the editing view if it is a single-selection
                            IParsedItem firstElement = (IParsedItem) sel.getFirstElement();
                            ErrorDescription errorDesc = firstElement.getErrorDesc();

                            //select the error
                            if (errorDesc != null && errorDesc.message != null) {
                                int len = errorDesc.errorEnd - errorDesc.errorStart;
                                editorView.setSelection(errorDesc.errorStart, len);
                                alreadySelected = true;
                            }
                        }
                        if (!alreadySelected) {
                            ISimpleNode[] node = getOutlineModel().getSelectionPosition(sel);
                            editorView.revealModelNodes(node);
                        }
                    } finally {
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

            tree.addMouseListener(new MouseListener() {

                public void mouseDoubleClick(MouseEvent e) {
                    tryToMakeSelection();
                }

                public void mouseDown(MouseEvent e) {
                }

                public void mouseUp(MouseEvent e) {
                    tryToMakeSelection();
                }
            });

            tree.addKeyListener(new KeyListener() {

                public void keyPressed(KeyEvent e) {
                }

                public void keyReleased(KeyEvent e) {
                    if (e.keyCode == SWT.ARROW_UP || e.keyCode == SWT.ARROW_DOWN) {
                        tryToMakeSelection();
                    }
                }
            });

            onControlCreated.call(getTreeViewer());
            createdCallbacksForControls = callRecursively(onControlCreated, filter, new ArrayList());
        } catch (Throwable e) {
            Log.log(e);
        }
    }

    /**
     * Calls the callback with the composite c and all of its children (recursively).
     */
    private List callRecursively(ICallbackWithListeners callback, Composite c, ArrayList controls) {
        try {
            controls.add(c);
            callback.call(c);
            for (Control child : c.getChildren()) {
                if (child instanceof Composite) {
                    callRecursively(callback, (Composite) child, controls);
                } else {
                    controls.add(child);
                    callback.call(child);
                }
            }
        } catch (Throwable e) {
            Log.log(e);
        }
        return controls;
    }

    public boolean show(ShowInContext context) {
        linkWithEditor.doLinkOutlinePosition(this.editorView, this,
                EditorUtils.createTextSelectionUtils(this.editorView));
        return true;
    }

    public Object getAdapter(Class adapter) {
        if (adapter == IShowInTarget.class) {
            return this;
        }
        return null;
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);
    }

    /**
     * Creates an event of a selection change if it's possible to do so (otherwise returns null)
     */
    private SelectionChangedEvent createSelectionEvent() {
        SelectionChangedEvent event = null;
        ISelection selection = getSelection();
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection s = (IStructuredSelection) selection;
            if (s.iterator().hasNext()) {
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
        if (event != null) {
            selectionChanged(event);
        }
    }

    public ICallbackWithListeners getOnControlCreated() {
        return onControlCreated;
    }

    public ICallbackWithListeners getOnControlDisposed() {
        return onControlDisposed;
    }

}
