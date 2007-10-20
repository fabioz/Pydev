/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;

public interface IOffsetStrategy {

	public static final int AFTERINIT = 1;

	public final static int BEGIN = 2;

	public final static int END = 4;

	public abstract int getOffset() throws BadLocationException;

}
