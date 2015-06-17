/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.python.pydev.shared_ui.search;

/**
 * Copy from org.eclipse.search.internal.ui.text.IFileSearchContentProvider
 */
public interface ISearchIndexContentProvider {

    public abstract void elementsChanged(Object[] updatedElements);

    public abstract void clear();

    public static final int GROUP_WITH_PROJECT = 1;
    public static final int GROUP_WITH_FOLDERS = 2;
    public static final int GROUP_WITH_MODULES = 4;

    public abstract void setGroupWith(int groupWithConfiguration);

}