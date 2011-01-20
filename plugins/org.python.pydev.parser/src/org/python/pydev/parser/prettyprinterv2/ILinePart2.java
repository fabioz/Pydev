/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.parser.prettyprinterv2;

/**
 * Defines a line part that has an associated string.
 */
public interface ILinePart2 extends ILinePart {

    public abstract void setString(String string);

    public abstract String getString();

}