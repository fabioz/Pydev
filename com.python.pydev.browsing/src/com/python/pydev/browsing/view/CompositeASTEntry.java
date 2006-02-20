package com.python.pydev.browsing.view;

import org.eclipse.core.resources.IFile;
import org.python.pydev.parser.visitors.scope.ASTEntry;

public class CompositeASTEntry {
	private ASTEntry entry;
	private IFile file;
	
	public CompositeASTEntry( ASTEntry entry, IFile file ) {
		this.entry = entry;
		this.file = file;
	}

	public ASTEntry getEntry() {
		return entry;
	}

	public void setEntry(ASTEntry entry) {
		this.entry = entry;
	}

	public IFile getFile() {
		return file;
	}

	public void setFile(IFile file) {
		this.file = file;
	}
	
	@Override
	public boolean equals(Object arg0) {	
		if( arg0 instanceof CompositeASTEntry ) {
			return ((CompositeASTEntry)arg0).getFile().equals(file);
		}
		return false;
	}
	
	@Override
	public int hashCode() {		
		return file.hashCode();
	}
}
