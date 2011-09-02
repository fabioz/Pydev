/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Jan 14, 2006
 */
package org.python.pydev.core;

import java.io.File;

public interface ICompletionRequest {

    /**
     * @return the python nature to which this completion is being request.d
     */
    IPythonNature getNature();

    /**
     * @return the editor file for the editor that's requesting this completion.
     * It can be null if the completion is not being called from an editor.
     */
    File getEditorFile();

    /**
     * @return the module for this request or null.
     * 
     * @throws MisconfigurationException 
     */
    IModule getModule() throws MisconfigurationException;

}