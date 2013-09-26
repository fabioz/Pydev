/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.shared_core.editor;

import java.util.Map;

import org.eclipse.jface.text.IDocument;
import org.python.pydev.shared_core.model.IModelListener;
import org.python.pydev.shared_core.parsing.IParserObserver;

public interface IBaseEditor extends IParserObserver {

    IDocument getDocument();

    /**
     * @return the editor input
     */
    /*IEditorInput*/Object getEditorInput();

    /**
     * This map may be used by clients to store info regarding this editor.
     * 
     * Clients should be careful so that this key is unique and does not conflict with other
     * plugins. 
     * 
     * This is not enforced.
     * 
     * The suggestion is that the cache key is always preceded by the class name that will use it.
     */
    Map<String, Object> getCache();

    /**
     * @return whether this edit and the one passed as a parameter have the same input.
     */
    boolean hasSameInput(IBaseEditor edit);

    void addModelListener(IModelListener modelListener);

    void removeModelListener(IModelListener modelListener);
}
