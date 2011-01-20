/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 07/09/2005
 */
package com.python.pydev.analysis.additionalinfo;

import org.python.pydev.core.ObjectsPool;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.NameTok;

public class FuncInfo extends AbstractInfo{
    
    private static final long serialVersionUID = 1L;

    /**
     * the parameters for the function (may be mull)
     */
    public String[] params;
    
    public static FuncInfo fromFunctionDef(FunctionDef def, String moduleDeclared, String path) {
        FuncInfo info = fromFunctionDef(def, moduleDeclared);
        info.path = path;
        return info;
    }

    public static FuncInfo fromFunctionDef(FunctionDef def, String moduleDeclared) {
        FuncInfo info = new FuncInfo();
        info.name = ObjectsPool.intern(((NameTok)def.name).id);
        info.moduleDeclared = moduleDeclared;
        return info;
    }
    
    public int getType() {
        return METHOD_WITH_IMPORT_TYPE;
    }

}