package org.python.pydev.parser.grammar_cython;

import org.python.pydev.core.ExtensionHelper;
import org.python.pydev.core.cython.IGenCythonAst;
import org.python.pydev.parser.PyParser.ParserInfo;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

/**
 * In this class we get the default interpreter and use cython to generate a json tree, read
 * that output and generate our own AST afterwards.
 */
public class PyParserCython {

    private ParserInfo info;

    public PyParserCython(ParserInfo info) {
        this.info = info;
    }

    public ParseOutput parse() {
        IGenCythonAst participant = (IGenCythonAst) ExtensionHelper.getParticipant(ExtensionHelper.GEN_CYTHON_AST,
                false);
        if (participant == null) {
            throw new RuntimeException(ExtensionHelper.GEN_CYTHON_AST + " not registered.");
        }
        ParseOutput parseOutput = participant.genCythonAst(this.info);
        return parseOutput;
    }

}
