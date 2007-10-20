/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.refactoring.ast.adapters.IASTNodeAdapter;

public class EndOffset extends AbstractOffsetStrategy {

	public EndOffset(IASTNodeAdapter<? extends SimpleNode> adapter, IDocument doc) {
		super(adapter, doc);
	}

	protected int getLine() {
		int endLine = adapter.getNodeLastLine() - 1;
		if (endLine < 0)
			endLine = 0;
		return endLine;
	}

}
