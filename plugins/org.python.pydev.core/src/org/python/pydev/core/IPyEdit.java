/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 12/06/2005
 */
package org.python.pydev.core;

import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.parsing.IParserObserver;

/**
 * @author Fabio
 */
public interface IPyEdit extends IParserObserver, IBaseEditor, IPyFormatStdProvider {

    /**
     * @return the python nature used in this editor
     * @throws NotConfiguredInterpreterException 
     * @throws MisconfigurationException 
     */
    IPythonNature getPythonNature() throws MisconfigurationException;

    /**
     * Set status message
     */
    void setStatusLineErrorMessage(String msg);

}
