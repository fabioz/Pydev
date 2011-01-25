/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Mar 22, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.core.structure;

/**
 * @author Fabio Zadrozny
 */
public class CompletionRecursionException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 4134633236965099432L;

    /**
     * @param string
     */
    public CompletionRecursionException(String string) {
        super(string);
    }

}
