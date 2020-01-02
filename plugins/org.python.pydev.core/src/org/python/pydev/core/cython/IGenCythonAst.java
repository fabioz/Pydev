package org.python.pydev.core.cython;

import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public interface IGenCythonAst {

    ParseOutput genCythonAst(Object parserInfo);

    String genCythonJson(Object parserInfo);
}
