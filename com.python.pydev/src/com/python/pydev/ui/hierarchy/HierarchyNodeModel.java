/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.util.Assert;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.visitors.NodeUtils;

/**
 * This is an hierarchy node that can have many parents and many children.
 * 
 * @author Fabio
 */
public class HierarchyNodeModel {

	/**
	 * Its name (cannot be null)
	 */
    public String name;
    
    public List<HierarchyNodeModel> parents = new ArrayList<HierarchyNodeModel>();
    public List<HierarchyNodeModel> children = new ArrayList<HierarchyNodeModel>();
    
    /**
     * The module where it was defined (might be null)
     */
	public String moduleName;
	
	/**
	 * The classdef definition (might be null)
	 */
	public ClassDef ast;
    
	public HierarchyNodeModel(String name) {
		this(name, "default", null);
	}
	public HierarchyNodeModel(String pack, ClassDef ast) {
		this(NodeUtils.getRepresentationString(ast), pack, ast);
	}
	
    public HierarchyNodeModel(String name, String pack, ClassDef ast) {
    	Assert.isNotNull(name);
        this.name = name;
        this.moduleName = pack;
        this.ast = ast;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    
    @Override
    public boolean equals(Object obj) {
        final HierarchyNodeModel h = (HierarchyNodeModel)obj;
		return name.equals(h.name) && moduleName.equals(h.moduleName);
    }
    
    @Override
    public String toString() {
    	return "HierarchyNodeModel["+name+" - "+moduleName+"]";
    }
}
