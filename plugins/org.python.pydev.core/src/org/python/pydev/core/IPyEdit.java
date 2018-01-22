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

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.python.pydev.shared_core.editor.IBaseEditor;
import org.python.pydev.shared_core.parsing.IParserObserver;

/**
 * @author Fabio
 */
public interface IPyEdit extends IParserObserver, IBaseEditor, IPyFormatStdProvider, IGrammarVersionProvider,
        IPyEditOfflineActionListener {

    static public final String EDITOR_ID = "org.python.pydev.editor.PythonEditor";

    /**
     * @return the python nature used in this editor
     * @throws NotConfiguredInterpreterException
     * @throws MisconfigurationException
     */
    @Override
    IPythonNature getPythonNature() throws MisconfigurationException;

    /**
     * Set status message
     */
    void setStatusLineErrorMessage(String msg);

    IProject getProject();

    /* SimpleNode*/ Object getAST();

    File getEditorFile();

    long getAstModificationTimeStamp();

    IFile getIFile();

    boolean isCythonFile();

    /* PyParser */ Object getParser();

}
