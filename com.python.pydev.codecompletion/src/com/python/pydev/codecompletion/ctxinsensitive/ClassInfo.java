/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import org.python.parser.ast.ClassDef;
import org.python.parser.ast.NameTok;

import sun.management.MethodInfo;

public class ClassInfo extends AbstractInfo{
    /**
     * the methods in this class (may be null)
     */
    public MethodInfo[] methods;

    public static ClassInfo fromClassDef(ClassDef def, String moduleDeclared) {
        ClassInfo info = new ClassInfo();
        info.name = ((NameTok)def.name).id;
        info.moduleDeclared = moduleDeclared;
        return info;
    }
}