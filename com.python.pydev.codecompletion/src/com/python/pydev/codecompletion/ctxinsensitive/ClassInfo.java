/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import org.python.parser.ast.ClassDef;
import org.python.parser.ast.NameTok;


public class ClassInfo extends AbstractInfo{

    private static final long serialVersionUID = 1L;

    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared) {
        ClassInfo info = new ClassInfo();
        info.name = ((NameTok)def.name).id;
        info.moduleDeclared = moduleDeclared;
        return info;
    }

    public int getType() {
        return CLASS_WITH_IMPORT_TYPE;
    }
}