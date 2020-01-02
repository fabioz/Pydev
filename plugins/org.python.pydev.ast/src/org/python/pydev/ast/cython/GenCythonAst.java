package org.python.pydev.ast.cython;

import org.python.pydev.core.cython.IGenCythonAst;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public class GenCythonAst implements IGenCythonAst {

    @Override
    public ParseOutput genCythonAst(Object parserInfo) {
        return new GenCythonAstImpl((ParserInfo) parserInfo).genCythonAst();

    }

    @Override
    public String genCythonJson(Object parserInfo) {
        return new GenCythonAstImpl((ParserInfo) parserInfo).genCythonJson();
    }

}
