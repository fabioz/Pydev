/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.request;

import java.util.List;

public interface IRequestProcessor<T> {
	public List<T> getRefactoringRequests();
}
