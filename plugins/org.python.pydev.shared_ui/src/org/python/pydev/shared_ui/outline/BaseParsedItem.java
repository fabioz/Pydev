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
package org.python.pydev.shared_ui.outline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.python.pydev.shared_core.model.ErrorDescription;

public abstract class BaseParsedItem implements IParsedItem, Comparable<Object> {

    protected IParsedItem parent;
    protected IParsedItem[] children;
    protected ErrorDescription errorDesc;

    /**
     * Updates the structure of this parsed item (old structure) to be the same as the structure in the passed
     * parsed item (new structure) trying to reuse the existing children (if possible).
     * 
     * This is usually only called when the structure actually changes (different number of nodes). A common case
     * is having a syntax error...
     */
    @Override
    public void updateTo(IParsedItem item) {
        IParsedItem updateToItem = item;
        this.toStringCache = null;
        this.errorDesc = updateToItem.getErrorDesc();

        IParsedItem[] newStructureChildren = updateToItem.getChildren();

        //handle special cases...
        if (this.children == null) {
            this.children = newStructureChildren;
            return;
        }

        if (newStructureChildren.length == 0 || this.children.length == 0) {
            //nothing to actually update... (just set the new children directly)
            this.children = newStructureChildren;
            return;
        }

        ArrayList<IParsedItem> newChildren = new ArrayList<IParsedItem>();

        //ok, something there... let's update the requested children... 
        //(trying to maintain the existing nodes were possible)
        HashMap<String, List<IParsedItem>> childrensCache = new HashMap<String, List<IParsedItem>>();
        for (IParsedItem existing : this.children) {
            String s = existing.toString();
            List<IParsedItem> list = childrensCache.get(s);
            if (list == null) {
                list = new ArrayList<IParsedItem>();
                childrensCache.put(s, list);
            }
            list.add(existing);
        }

        for (IParsedItem n : newStructureChildren) {
            IParsedItem similarChild = getSimilarChild(n, childrensCache);
            if (similarChild != null) {
                similarChild.updateTo(n);
                n = similarChild;
            } else {
                n.setParent(this);
            }
            newChildren.add(n);
        }

        this.children = newChildren.toArray(new IParsedItem[newChildren.size()]);
    }

    @Override
    public void setParent(IParsedItem parsedItem) {
        this.parent = parsedItem;
    }

    private IParsedItem getSimilarChild(IParsedItem n, HashMap<String, List<IParsedItem>> childrensCache) {
        //try to get a similar child from the 'cache'
        List<IParsedItem> list = childrensCache.get(n.toString());
        if (list != null && list.size() > 0) {
            return list.remove(0);
        }
        return null;
    }

    @Override
    public IParsedItem getParent() {
        return parent;
    }

    /**
     * When null, it must be rebuilt!
     */
    protected String toStringCache;

    @Override
    public String toString() {
        if (toStringCache == null) {
            toStringCache = calcToString();
        }
        return toStringCache;
    }

    protected abstract String calcToString();

    @Override
    public ErrorDescription getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(ErrorDescription errorDesc) {
        if (this.errorDesc == null && errorDesc == null) {
            return; // don't clear the caches
        }
        this.toStringCache = null;
        this.errorDesc = errorDesc;
    }

}
