/*
 * Author: atotic
 * Created: Jul 25, 2003
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioningListener;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.rules.DefaultPartitioner;
import org.eclipse.jface.viewers.StructuredSelection;
import org.python.pydev.editor.model.AbstractNode;


/**
 * RawPartitionModel represents the partitions inside the document
 * 
 * events-listen: document changed/document partitioning changed
 * events-broadcast: 
 * the model is a Position[], the replica of the document model
 */
class RawPartitionModel implements IOutlineModel {
	private IDocument document;
	private Position[] positions = null;
	private IDocumentPartitioningListener documentChangeListener;
	private IDocumentListener documentListener;
	private PyOutlinePage outline;
	
	RawPartitionModel(PyOutlinePage outline, IDocument document) {
		this.document = document;
		this.outline = outline;
		updatePositions();
		createDocumentListeners();
	}

	public void dispose() {
		// remove the listeners
		document.removeDocumentPartitioningListener(documentChangeListener);
		document.removeDocumentListener(documentListener);
	}
	
	public Object getRoot() {
		return this;
	}
	
	public String getText(Position p) {
		try {
			return document.get(p.offset, p.length);
		} catch (BadLocationException e) {
			e.printStackTrace();
			return "";
		}
	}

	/**
	 * listen to teh changes in the document, and update tree accordingly
	 */
	void createDocumentListeners() {
		// partition update strategy 
		// refresh the whole tree when partition changes
		// refresh particular items when document changes

		// items are added/deleted when partitioning changes
		documentChangeListener = new IDocumentPartitioningListener() {
			public void documentPartitioningChanged(IDocument document) {
				updatePositions();
			}
		};
		document.addDocumentPartitioningListener(documentChangeListener);
	
		documentListener = new IDocumentListener() {
			public void documentChanged(DocumentEvent event) {
				// update items whose text might have changed
				for (int i=0; i< positions.length; i++) {
					if (positions[i].overlapsWith(event.fOffset, event.fLength)) {
						outline.updateItems(new Object[] {positions[i]});
					}
				}
			}
			public void documentAboutToBeChanged(DocumentEvent event) {}		
		};
		document.addDocumentListener(documentListener);
		
	}
	
	void updatePositions() {
		try {
			Position[] newpositions = document.getPositions(DefaultPartitioner.CONTENT_TYPES_CATEGORY);
			if (positions == null ) {
				// initialization
				positions = newpositions;
			}
			else {
				// document partitioning changed, wholesale replace
				positions = newpositions;
				outline.refreshItems(null);			
			}
		} catch (BadPositionCategoryException e) {
			System.err.println("Unexpected error in RawPartitonModel::updatePositions");
			positions = new Position[0];
			e.printStackTrace();
		}			
	}
	
	Position[] getPositions() {
		return positions;
	}

	// IOutlineModel API
	public AbstractNode getSelectionPosition(StructuredSelection sel) {
		// we do not have it, broken in Outline rewrite
		System.err.println("RawOutlineModel can't navigate right now");
		return null;
	}

	public int compare(Object e1, Object e2) {
		String title1 = getText((Position)e1);
		String title2 = getText((Position)e2);
		return title1.compareTo(title2);
	}
}