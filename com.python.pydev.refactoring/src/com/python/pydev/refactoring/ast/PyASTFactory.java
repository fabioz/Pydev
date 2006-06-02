/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Pass;
import org.python.pydev.parser.jython.ast.exprType;
import org.python.pydev.parser.jython.ast.stmtType;

public class PyASTFactory {

    /**
     * @param nameStr the name of the class definition
     * @return a class definition with a single 'pass'
     */
    public static ClassDef makePassClassDef(String nameStr) {
        NameTok name = new NameTok(nameStr, NameTok.ClassName);
        name.addSpecial(":", true);
        Pass pass = new Pass();
        pass.addSpecial("pass", false);
        ClassDef classDef = new ClassDef(name, new exprType[0], new stmtType[]{pass});
        return classDef;
    }

}
