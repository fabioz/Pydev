/* 
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.tests.codegenerator.generatedocstring;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.editors.text.StorageDocumentProvider;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.IDocumentProvider;

public 	class TextEditorStub extends AbstractTextEditor {
	private IDocument document;
	private ITextSelection selection;
	
	private int selectionOffset;
	private int selectionLength;

	public TextEditorStub(IDocument document, ITextSelection selection) {
		this.document = document;
		this.selection = selection;
	}
	
	@Override
	public void selectAndReveal(int start, int length) {
		this.selectionOffset = start;
		this.selectionLength = length;
	}
	
	public int getSelectionOffset() {
		return selectionOffset;
	}
	
	public int getSelectionLength() {
		return selectionLength;
	}
	
	@Override
	public IDocumentProvider getDocumentProvider() {
		return new StorageDocumentProvider() {
			@Override
			public IDocument getDocument(Object element) {
				return document;
			}
		};
	}
	
	@Override
	public ISelectionProvider getSelectionProvider() {
		return new ISelectionProvider() {
			public ISelection getSelection() {
				return selection;
			}
			
			public void addSelectionChangedListener(ISelectionChangedListener l) {
			}

			public void removeSelectionChangedListener(ISelectionChangedListener l) {
			}

			public void setSelection(ISelection selection) {
			}
		};
	}
}
