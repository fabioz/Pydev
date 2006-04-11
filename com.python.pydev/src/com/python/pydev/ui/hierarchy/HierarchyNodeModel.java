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
	private String pack;
    
	public HierarchyNodeModel(String name) {
		this(name, "default");
	}
    public HierarchyNodeModel(String name, String pack) {
        this.name = name;
        this.pack = pack;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        final HierarchyNodeModel h = (HierarchyNodeModel)obj;
		return name.equals(h.name) && pack.equals(h.pack);
    }
}
