/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.util.ArrayList;
import java.util.List;

/**
 * This is an hierarchy node that can have many parents and many children.
 * 
 * @author Fabio
 */
public class HierarchyNodeModel {

    public String name;
    public List<HierarchyNodeModel> parents = new ArrayList<HierarchyNodeModel>();
    public List<HierarchyNodeModel> children = new ArrayList<HierarchyNodeModel>();
    
    public HierarchyNodeModel(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        return name.equals(((HierarchyNodeModel)obj).name);
    }
}
