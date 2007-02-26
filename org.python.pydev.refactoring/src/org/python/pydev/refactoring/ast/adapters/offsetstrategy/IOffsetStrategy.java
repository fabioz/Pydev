package org.python.pydev.refactoring.ast.adapters.offsetstrategy;

import org.eclipse.jface.text.BadLocationException;

public interface IOffsetStrategy {

	public static final int AFTERINIT = 1;

	public final static int BEGIN = 2;

	public final static int END = 4;

	public abstract int getOffset() throws BadLocationException;

}