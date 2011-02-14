/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

/**
 * Defines a part of a line of the document we will build while making the pretty-printing.
 */
public interface ILinePart {

    public int getBeginCol();

    public abstract Object getToken();

    public int getLine();

    public int getLinePosition();

    public void setMarkAsFound();

    public boolean isMarkedAsFound();
}
