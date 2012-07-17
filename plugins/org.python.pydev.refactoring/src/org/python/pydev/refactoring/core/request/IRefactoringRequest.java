/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.request;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public interface IRefactoringRequest {

    IASTNodeAdapter<? extends SimpleNode> getOffsetNode();

    AdapterPrefs getAdapterPrefs();
}
