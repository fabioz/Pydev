/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Author: atotic
 * Created on Apr 30, 2004
 */
package org.python.pydev.debug.model;

/**
 * IVariableLocator knows how to produce location information
 * for CMD_GET_VARIABLE
 * 
 * The location is specified as:
 * 
 * thread_id, stack_frame, LOCAL|GLOBAL, attribute*
 */
public interface IVariableLocator {

    public String getThreadId();

    public String getPyDBLocation();

}
