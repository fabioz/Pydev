/*
 * Created on 07/09/2005
 */
package com.python.pydev.codecompletion.ctxinsensitive;

import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.NameTok;

public class FuncInfo extends AbstractInfo{
    /**
     * the parameters for the function (may be mull)
     */
    public String[] params;

    public static FuncInfo fromFunctionDef(FunctionDef def, String moduleDeclared) {
        FuncInfo info = new FuncInfo();
        info.name = ((NameTok)def.name).id;
        info.moduleDeclared = moduleDeclared;
        return info;
    }
    public int getType() {
        return METHOD_WITH_IMPORT_TYPE;
    }

}