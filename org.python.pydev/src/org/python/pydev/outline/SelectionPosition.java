/*
 * Author: fabioz, refactored by atotic
 * Created on Mar 5, 2004
 * License: Common Public License v1.0
 */
package org.python.pydev.outline;

import org.eclipse.jface.text.IRegion;

/**
 * Simple struct class that describes what to select
 *
 */
public final class SelectionPosition {
	public IRegion r;

	public int line;
	public int column;  // use WHOLE_LINE to select the whole line
	public int length;
	
	public static final int WHOLE_LINE=999;

	SelectionPosition(IRegion r) {
		this.r = r;
	}

	SelectionPosition(int line, int column, int length) {
		this.line = line;
		this.column = column;
		this.length = length;
		this.r = null;
	}
};