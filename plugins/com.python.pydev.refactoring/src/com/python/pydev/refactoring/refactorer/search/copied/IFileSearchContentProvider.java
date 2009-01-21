package com.python.pydev.refactoring.refactorer.search.copied;

public interface IFileSearchContentProvider {

	public abstract void elementsChanged(Object[] updatedElements);

	public abstract void clear();

}