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
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.NameTok;


public class ClassInfo extends AbstractInfo{

    private static final long serialVersionUID = 1L;

    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared, String path) {
        ClassInfo info = fromClassDef(def, moduleDeclared);
        info.path = path;
        return info;
        
    }
    
    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared) {
        ClassInfo info = new ClassInfo();
        info.name = ObjectsPool.intern(((NameTok)def.name).id);
        info.moduleDeclared = moduleDeclared;
        return info;
    }

    public int getType() {
        return CLASS_WITH_IMPORT_TYPE;
    }
}