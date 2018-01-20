/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion;

/**
 * @author Fabio Zadrozny
 */
public interface IPyCompletionProposal2 {

    /**
     * @return the internal display list representation to be used for sorting.
     */
    String getInternalDisplayStringRepresentation();

}
