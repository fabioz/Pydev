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
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;
import org.python.pydev.editor.PyEdit;
import org.python.pydev.ui.ImageCache;
import org.python.pydev.ui.UIConstants;

/**
 * Outline page, displays the structure of the document in the editor window 
 *
 * Partition outlining:
 * PyDocumentProvider already partitions the document into strings/comments/other
 * RawPartition is the simplest outline that shows this "raw" document partitioning
 *
 * Design notes:
 * a good (and only one that subclasses ContentOutlinePage) 
 * example of Eclipse's internal outline page is
 * org.eclipse.ui.extenaltools.internal.ant.editor.outline
 * see PlantyEditor, and PlantyContentOutlinePage
 * 
 **/
public class PyOutlinePage extends ContentOutlinePage  {

	ViewerSorter sortByNameSorter;

	PyEdit editorView;
	IDocument document;
	IOutlineModel model;
	ImageCache imageCache = new ImageCache();
	
	// listeners to rawPartition
	ISelectionChangedListener selectionListener;

	public PyOutlinePage(PyEdit editorView) {
		super();
		this.editorView = editorView;
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
	public void createRawPartitionOutline() { // TODO public to suppress the warnings, otherwise not currently used
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
		model = new ParsedModel(this, editorView.getParser());
		tree.setAutoExpandLevel(AbstractTreeViewer.ALL_LEVELS);
		tree.setContentProvider(new ParsedContentProvider());
		tree.setLabelProvider(new ParsedLabelProvider(imageCache));
		tree.setInput(model.getRoot());
	}
		
	/*
	 * called when model has structural changes, refreshes all items underneath
	 * @param items: items to refresh, or null for the whole tree
	 * tries to preserve the scrolling
	 */
	public void refreshItems(Object[] items) {
		TreeViewer viewer = getTreeViewer();
		Tree treeWidget = viewer.getTree();
		ScrollBar bar = treeWidget.getVerticalBar();
		int barPosition = 0;
		if (bar != null) {
			barPosition = bar.getSelection();
		}
		if (items == null)
			viewer.refresh();
		else 
			for (int i=0; i< items.length; i++) {
				viewer.refresh(items[i]);
			}
		if (barPosition != 0) {
			bar.setSelection(Math.min(bar.getMaximum(), barPosition));
		}
	}
	public void refreshAll() {
	}
	
	/**
	 * called when a single item changes
	 */
	public void updateItems(Object[] items) {
		getTreeViewer().update(items, null);
	}

	/**
	 * @args doSort : sort or not?
	 */
	public void setAlphaSort(boolean doSort) {
		if (sortByNameSorter == null) {
			sortByNameSorter = new ViewerSorter() {
				public int compare(Viewer viewer, Object e1, Object e2) {
					return model.compare(e1, e2);
				}
			};
		}
		getTreeViewer().setSorter(doSort ? sortByNameSorter : null);
	}
	
	private void createActions() {
		// Sort by name
		Action sortByName = new Action("Sort by name", IAction.AS_CHECK_BOX ) {
			public void run() {
				setAlphaSort(isChecked());
			}
		};
		sortByName.setToolTipText("Sort by name");
		// Collapse all
		Action collapseAll = new Action("Collapse all", IAction.AS_PUSH_BUTTON) {
			public void run() {
				getTreeViewer().collapseAll();
			}
		};
		
		try {
			sortByName.setImageDescriptor(imageCache.getDescriptor(UIConstants.ALPHA_SORT));
			collapseAll.setImageDescriptor(imageCache.getDescriptor(UIConstants.COLLAPSE_ALL));
		} catch (MalformedURLException e) {
			System.err.println("missing icon");
			e.printStackTrace();
		}

		// Add actions to the toolbar
		IToolBarManager toolbarManager = getSite().getActionBars().getToolBarManager();
		toolbarManager.add(sortByName);
		toolbarManager.add(collapseAll);
	}
	
	/**
	 * create the outline view widgets
	 */
	public void createControl(Composite parent) {
		super.createControl(parent); // this creates a tree viewer
//		createRawPartitionOutline();
		createParsedOutline();
		// selecting an item in the outline scrolls the document
		final TreeViewer tree = getTreeViewer();
		selectionListener = new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				StructuredSelection sel = (StructuredSelection)tree.getSelection();
				IOutlineModel.SelectThis newSel = model.selectionChanged(sel);
				if (newSel == null)
					return;
				if (newSel.r != null) {
					editorView.setSelection(newSel.r.getOffset(), newSel.r.getLength());
				}
				else {
					IDocumentProvider provider = editorView.getDocumentProvider();
					IDocument document = provider.getDocument(editorView.getEditorInput());
					try {
						IRegion r = document.getLineInformation(newSel.line - 1);
						// if selecting the whole line, just use the information
						if (newSel.column == IOutlineModel.SelectThis.WHOLE_LINE) {
							newSel.column = 0;
							newSel.length = r.getLength();
						}
						editorView.setSelection(r.getOffset() + newSel.column, newSel.length);
					} catch (BadLocationException e) {
						e.printStackTrace();
					}
				}
			}
		};
		tree.addSelectionChangedListener(selectionListener);		
		createActions();
	}
}
