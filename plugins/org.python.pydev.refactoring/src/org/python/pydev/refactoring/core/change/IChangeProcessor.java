/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.change;

import org.eclipse.ltk.core.refactoring.Change;
import org.python.pydev.core.MisconfigurationException;

public interface IChangeProcessor {
    Change createChange() throws MisconfigurationException;
}
