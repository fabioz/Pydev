package org.python.pydev.parser;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.ast.Expr;

public interface IGrammar2 {

    Expr eval_input() throws ParseException;

}
