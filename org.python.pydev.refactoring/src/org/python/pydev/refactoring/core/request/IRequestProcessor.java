package org.python.pydev.refactoring.core.request;

import java.util.List;

public interface IRequestProcessor<T> {
	public List<T> getRefactoringRequests();
}
