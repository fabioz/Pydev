package com.python.pydev.refactoring.refactorer.search;

import java.util.regex.Pattern;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.internal.core.text.TextSearchVisitor;
import org.eclipse.search.ui.text.FileTextSearchScope;

public class PythonTextSearchEngine {


	public static IStatus search(FileTextSearchScope scope, TextSearchRequestor requestor, String searchText, IProgressMonitor monitor) {
		return new PythonTextSearchVisitor(requestor, searchText).search(scope, monitor);
	}

}
