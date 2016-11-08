package org.python.pydev.parser;

import java.util.List;

import org.python.pydev.parser.fastparser.grammar_fstrings_common.SimpleNode;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammar;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;

import junit.framework.TestCase;

public class FStringsParserTest extends TestCase {

    private Tuple<SimpleNode, List> check(String str) throws ParseException {
        FastCharStream in = new FastCharStream(str.toCharArray());
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);
        SimpleNode ast = fStringsGrammar.file_input();
        //Note: we always try to generate a valid AST and get any errors in getParseErrors().
        List<ParseException> parseErrors = fStringsGrammar.getParseErrors();
        //        System.out.println("\n\n-----\n" + str);
        //        ast.dump("");
        return new Tuple<>(ast, parseErrors);
    }

    private void checkError(String str) throws ParseException {
        Tuple<SimpleNode, List> tup = check(str);
        if (tup.o2 == null || tup.o2.size() == 0) {
            fail("Expected error");
        }
    }

    private void checkError(String str, String... expected) throws ParseException {
        Tuple<SimpleNode, List> tup = check(str);
        if (tup.o2 == null || tup.o2.size() == 0) {
            fail("Expected error");
        }
        for (String s : expected) {
            boolean found = false;
            for (Object o : tup.o2) {
                if (o.toString().contains(s)) {
                    found = true;
                }
            }
            if (!found) {
                for (Object o : tup.o2) {
                    ((Throwable) o).printStackTrace();
                }
                fail("Expected error message with: " + s + "\nAvailable:\n" + StringUtils.join("\n", tup.o2));
            }
        }
    }

    public void testFStringParsing() throws ParseException {
        check("a{text}a{text}b");
        check("{text!a}");
        check("{text!s}");
        check("{text!r}");
        check("{text!r:foo}");
        check("{text:#.6f}");
        check("{text#.6f}");
        check("{text:}");
        check("newline: {call('{}')}");
        check("'.nhunsoeth{'{uoesnth{ueo:{}''}'}");
        check("\".nhunsoeth{'{uoesnth{ueo:{}''}'}");
        check("{text:}");
        check("{call(\"\")}");
        check("slash\\\\{aa}");
        check("special chars !\\:'\"()[]{aa}");

        checkError("{}", "Empty expression not allowed in f-string");
        checkError("{   }", "Empty expression not allowed in f-string");
        checkError("{!}", "Empty expression not allowed in f-string", "Only '!a', '!s' or '!r' accepted.");
        checkError("{!a}", "Empty expression not allowed in f-string");
        checkError("{:}", "Empty expression not allowed in f-string");

        checkError("{text!}", "Only '!a', '!s' or '!r' accepted.");

        checkError("{text", "Unbalanced '{'");
        checkError("{\"a}", "Unbalanced '\"'");
        checkError("{ { }", "Unbalanced '{'");
        checkError("{ ( }", "Unbalanced '('");
        checkError("{ [ }", "Unbalanced '['");
        checkError("{'a}", "Unbalanced \"'\"");
        checkError("{text!x}", "Expecting '!a', '!s' or '!r'. Found: x");
        checkError("{no backslash\\\\n}", "Backslash (\\) not valid inside f-string expressions.");
        checkError("{no backslash'\\\\n'}", "Backslash (\\) not valid inside f-string expressions.");

    }
}
