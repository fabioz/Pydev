/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 10, 2006
 */
package com.python.pydev.ui.hierarchy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.python.pydev.core.IModule;
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

    public List<HierarchyNodeModel> parents = new ArrayList<HierarchyNodeModel>(5);
    public List<HierarchyNodeModel> children = new ArrayList<HierarchyNodeModel>(5);

    /**
     * The module where it was defined (might be null)
     */
    public String moduleName;

    /**
     * The classdef definition (might be null)
     */
    public ClassDef ast;

    /**
     * The module where this node is defined (might be null)
     */
    public IModule module;

    public HierarchyNodeModel(String name) {
        this(name, "", null);
    }

    public HierarchyNodeModel(String pack, ClassDef ast) {
        this(NodeUtils.getRepresentationString(ast), pack, ast);
    }

    public HierarchyNodeModel(String name, String pack, ClassDef ast) {
        Assert.isNotNull(name);
        if (pack == null) {
            pack = "";
        }
        this.name = name;
        this.moduleName = pack;
        this.ast = ast;
    }

    public HierarchyNodeModel(IModule module, ClassDef def) {
        this(module.getName(), def);
        this.module = module;
    }

    @Override
    public String toString() {
        return "HierarchyNodeModel[" + name + " - " + moduleName + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((moduleName == null) ? 0 : moduleName.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (!(obj instanceof HierarchyNodeModel))
            return false;
        HierarchyNodeModel other = (HierarchyNodeModel) obj;
        if (moduleName == null) {
            if (other.moduleName != null)
                return false;
        } else if (!moduleName.equals(other.moduleName))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

}
