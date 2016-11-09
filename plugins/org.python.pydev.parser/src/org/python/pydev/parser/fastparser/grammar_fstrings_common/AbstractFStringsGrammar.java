package org.python.pydev.parser.fastparser.grammar_fstrings_common;

import java.util.ArrayList;
import java.util.List;

import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.parser.jython.Token;

public class AbstractFStringsGrammar {

    protected void jjtreeOpenNodeScope(SimpleNode node) {

    }

    protected void jjtreeCloseNodeScope(SimpleNode node) {

    }

    /**
     * List with the errors we handled during the parsing
     */
    private final List<ParseException> parseErrors = new ArrayList<ParseException>();

    /**
     * @return a list with the parse errors. Note that the returned value is not a copy, but the actual
     * internal list used to store the errors.
     */
    public List<ParseException> getParseErrors() {
        return parseErrors;
    }

    /**
     * Adds some parse exception to the list of parse exceptions found.
     */
    protected void addParseError(ParseException e) {
        parseErrors.add(e);
    }

    protected void addParseError(Token t, String msg) {
        addParseError(new ParseException(msg, t));
    }

    protected void addParseError(ParseException e, String msg) {
        if (e.currentToken != null) {
            parseErrors.add(new ParseException(msg, e.currentToken.beginLine, e.currentToken.beginColumn));
        } else {
            parseErrors.add(new ParseException(msg));
        }
    }

    protected void errorBackSlashInvalidInFStrings(Token t) {
        addParseError(new ParseException("Backslash (\\) not valid inside f-string expressions.", t));
    }

    protected void errorIfTextIsNotASR(Token t) {
        if (!t.image.equals("a") && !t.image.equals("s") && !t.image.equals("r")) {
            addParseError(new ParseException("Expecting '!a', '!s' or '!r'. Found: " + t.image, t));
        }
    }

    protected void errorPyExprEmpty(SimpleNode node) {
        addParseError(new ParseException("Empty expression not allowed in f-string", node.beginLine, node.beginColumn));
    }

    protected void errorTypeConversionEmpty(SimpleNode node) {
        addParseError(new ParseException("Only '!a', '!s' or '!r' accepted.", node.beginLine, node.beginColumn));
    }

}
