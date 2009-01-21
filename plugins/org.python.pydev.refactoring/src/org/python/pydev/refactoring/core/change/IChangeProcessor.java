/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.change;

import org.eclipse.ltk.core.refactoring.Change;

public interface IChangeProcessor {

    public Change createChange();
}
