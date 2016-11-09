package org.python.pydev.parser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.FStringsAST;
import org.python.pydev.parser.fastparser.grammar_fstrings_common.SimpleNode;
import org.python.pydev.parser.grammar_fstrings.FStringsGrammar;
import org.python.pydev.parser.jython.FastCharStream;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.shared_core.string.StringUtils;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.utils.ArrayUtils;

import junit.framework.TestCase;

public class FStringsParserTest extends TestCase {

    private Tuple<FStringsAST, List> check(String str) throws ParseException {
        FastCharStream in = new FastCharStream(str.toCharArray());
        FStringsGrammar fStringsGrammar = new FStringsGrammar(in);
        FStringsAST ast = fStringsGrammar.f_string();
        //Note: we always try to generate a valid AST and get any errors in getParseErrors().
        List<ParseException> parseErrors = fStringsGrammar.getParseErrors();
        //        System.out.println("\n\n-----\n" + str);
        //        ast.dump("");
        return new Tuple<>(ast, parseErrors);
    }

    private Tuple<FStringsAST, List> checkExprs(String str, Set<String> exprs)
            throws ParseException, BadLocationException {
        Tuple<FStringsAST, List> ret = check(str);
        //        ret.o1.dump();
        Document doc = new Document(str);
        Set<String> found = new HashSet<>();
        for (SimpleNode b : ret.o1.getBalancedExpressions()) {
            String contents = b.getContentsFromString(str, doc);
            found.add(contents);
        }
        assertEquals(exprs, found);
        return ret;
    }

    private void checkError(String str, String... expected) throws ParseException {
        Tuple<FStringsAST, List> tup = check(str);
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

    public void testFStringParsing() throws ParseException, BadLocationException {
        checkExprs("{{'c':20}}", ArrayUtils.asSet("{'c':20}"));

        checkExprs("a{text}a{text2}b", ArrayUtils.asSet("text", "text2"));
        checkExprs("{text!a}", ArrayUtils.asSet("text"));
        checkExprs("{text!s}", ArrayUtils.asSet("text"));
        checkExprs("{text!r}", ArrayUtils.asSet("text"));
        checkExprs("{text!r:foo}", ArrayUtils.asSet("text"));
        checkExprs("{text:#.6f}", ArrayUtils.asSet("text"));
        checkExprs("{text#.6f}", ArrayUtils.asSet("text#.6f"));
        checkExprs("{text:}", ArrayUtils.asSet("text"));
        checkExprs("newline: {call('{}')}", ArrayUtils.asSet("call('{}')"));
        checkExprs("'.nhunsoeth{'{uoesnth{ueo:{}''}'}", ArrayUtils.asSet("'{uoesnth{ueo:{}''}'"));
        checkExprs("\".nhunsoeth{'{uoesnth{ueo:{}''}'}", ArrayUtils.asSet("'{uoesnth{ueo:{}''}'"));
        checkExprs("{text:}", ArrayUtils.asSet("text"));
        checkExprs("{call(\"\")}", ArrayUtils.asSet("call(\"\")"));
        checkExprs("slash\\\\{aa}", ArrayUtils.asSet("aa"));
        checkExprs("special chars !\\:'\"()[]{aa}", ArrayUtils.asSet("aa"));
        checkExprs("multi\nline\n{aaa}", ArrayUtils.asSet("aaa"));

        checkError("{}", "Empty expression not allowed in f-string");
        checkError("{   }", "Empty expression not allowed in f-string");
        checkError("{!}", "Empty expression not allowed in f-string", "Only '!a', '!s' or '!r' accepted.");
        checkError("{!a}", "Empty expression not allowed in f-string");
        checkError("{:}", "Empty expression not allowed in f-string");

        checkError("{text!}", "Only '!a', '!s' or '!r' accepted.");

        checkError("}", "Single '}' not allowed");
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
