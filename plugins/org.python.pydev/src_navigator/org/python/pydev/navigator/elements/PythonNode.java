/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Oct 8, 2006
 * @author Fabio
 */
package org.python.pydev.navigator.elements;

import org.python.pydev.outline.ParsedItem;

public class PythonNode implements Comparable, IWrappedResource {

    /**
     * This is the parent (PythonFile or PythonNode) for this object
     */
    public Object parent;

    /**
     * The entry itself
     */
    public ParsedItem entry;

    /**
     * The pythonfile where this node is contained
     */
    public PythonFile pythonFile;

    /**
     * Constructor
     *
     * @param pythonFile this is the file that contains this node
     * @param parent this is the parent for this item (a PythonFile or another PythonNode)
     * @param e the parsed item that represents this node.
     */
    public PythonNode(PythonFile pythonFile, Object parent, ParsedItem e) {
        this.parent = parent;
        this.entry = e;
        this.pythonFile = pythonFile;
    }

    @Override
    public String toString() {
        return entry.toString();
    }

    @Override
    public int compareTo(Object o) {
        if (!(o instanceof PythonNode)) {
            return 0;
        }
        return entry.compareTo(((PythonNode) o).entry);
    }

    @Override
    public Object getParentElement() {
        return parent;
    }

    @Override
    public ParsedItem getActualObject() {
        return entry;
    }

    @Override
    public PythonSourceFolder getSourceFolder() {
        return pythonFile.getSourceFolder();
    }

    public PythonFile getPythonFile() {
        return pythonFile;
    }

    @Override
    public int getRank() {
        return IWrappedResource.RANK_PYTHON_NODE;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        //return pythonFile.getAdapter(adapter);
        return null;
    }

}
