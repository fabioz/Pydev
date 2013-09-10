/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.ast.adapters;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;

public class ClassDefAdapterFromClassDef implements IClassDefAdapter {

    private ClassDef classDef;
    private AdapterPrefs adapterPrefs;
    private ModuleAdapter module;

    public ClassDefAdapterFromClassDef(ModuleAdapter module, ClassDef classDef, AdapterPrefs adapterPrefs) {
        this.module = module;
        this.classDef = classDef;
        this.adapterPrefs = adapterPrefs;
    }

    public List<SimpleAdapter> getAssignedVariables() {
        throw new RuntimeException("Not implemented");
    }

    public List<SimpleAdapter> getAttributes() {
        throw new RuntimeException("Not implemented");
    }

    public List<String> getBaseClassNames() {
        throw new RuntimeException("Not implemented");
    }

    public List<IClassDefAdapter> getBaseClasses() {
        throw new RuntimeException("Not implemented");
    }

    public FunctionDefAdapter getFirstInit() {
        for (stmtType b : this.classDef.body) {
            if (b instanceof FunctionDef) {
                FunctionDef functionDef = (FunctionDef) b;
                if (((NameTok) functionDef.name).id.equals("__init__")) {
                    return new FunctionDefAdapter(module, null, (FunctionDef) b, adapterPrefs);
                }
            }
        }
        return null;
    }

    public List<FunctionDefAdapter> getFunctions() {
        ArrayList<FunctionDefAdapter> ret = new ArrayList<FunctionDefAdapter>();
        for (stmtType b : this.classDef.body) {
            if (b instanceof FunctionDef) {
                ret.add(new FunctionDefAdapter(module, null, (FunctionDef) b, adapterPrefs));
            }
        }
        return ret;
    }

    public List<FunctionDefAdapter> getFunctionsInitFiltered() {
        ArrayList<FunctionDefAdapter> ret = new ArrayList<FunctionDefAdapter>();
        for (stmtType b : this.classDef.body) {
            if (b instanceof FunctionDef) {
                FunctionDef functionDef = (FunctionDef) b;
                if (((NameTok) functionDef.name).id.equals("__init__")) {
                    continue;
                }
                ret.add(new FunctionDefAdapter(module, null, functionDef, adapterPrefs));
            }
        }
        return ret;
    }

    public String getNodeBodyIndent() {
        throw new RuntimeException("Not implemented");
    }

    public List<PropertyAdapter> getProperties() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasAttributes() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasBaseClass() {
        return false;
    }

    public boolean hasFunctions() {
        throw new RuntimeException("Not implemented");
    }

    public boolean hasFunctionsInitFiltered() {
        return true;
    }

    public boolean hasInit() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isNested() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isNewStyleClass() {
        throw new RuntimeException("Not implemented");
    }

    public String getName() {
        return ((NameTok) this.classDef.name).id;
    }

    public String getParentName() {
        throw new RuntimeException("Not implemented");
    }

    public ClassDef getASTNode() {
        throw new RuntimeException("Not implemented");
    }

    public SimpleNode getASTParent() {
        throw new RuntimeException("Not implemented");
    }

    public ModuleAdapter getModule() {
        throw new RuntimeException("Not implemented");
    }

    public int getNodeFirstLine() {
        throw new RuntimeException("Not implemented");
    }

    public int getNodeIndent() {
        throw new RuntimeException("Not implemented");
    }

    public int getNodeLastLine() {
        throw new RuntimeException("Not implemented");
    }

    public AbstractNodeAdapter<? extends SimpleNode> getParent() {
        throw new RuntimeException("Not implemented");
    }

    public SimpleNode getParentNode() {
        throw new RuntimeException("Not implemented");
    }

    public boolean isModule() {
        throw new RuntimeException("Not implemented");
    }

}
