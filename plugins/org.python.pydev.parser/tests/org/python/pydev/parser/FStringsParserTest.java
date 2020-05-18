package org.python.pydev.parser;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
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

    private Tuple<FStringsAST, List> checkNoError(String string) throws ParseException {
        Tuple<FStringsAST, List> check = check(string);
        if (check.o2.size() != 0) {
            List o2 = check.o2;
            throw new AssertionError(
                    "Expected no errors. Found: \n" + StringUtils.join("\n", o2));
        }
        return check;
    }

    private Tuple<FStringsAST, List> checkExprs(String str, Set<String> exprs)
            throws ParseException, BadLocationException {
        Tuple<FStringsAST, List> ret = checkNoError(str);
        IDocument doc = new Document(str);
        // ret.o1.dump(doc);
        Set<String> found = new HashSet<>();
        for (SimpleNode b : ret.o1.getBalancedExpressionsToBeEvaluatedInRegularGrammar()) {
            String contents = b.getContentsFromString(doc);
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
        checkExprs("\\N{foo}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{foo}}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{}}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{\\N{foo}}}", ArrayUtils.asSet());
        checkExprs("\\N{", ArrayUtils.asSet());
        checkExprs("\\N{foo} {foo}", ArrayUtils.asSet("foo"));
        checkExprs("\\N{foo}{foo}", ArrayUtils.asSet("foo"));
        checkExprs("\\N{foo}\\N{foo}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{foo}}\\N{foo}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{}} \\N{foo}", ArrayUtils.asSet());
        checkExprs("\\N{\\N{\\N{foo}}} \\N{foo}", ArrayUtils.asSet());
        checkExprs("\\N{foo", ArrayUtils.asSet());
        checkExprs("\\N{foo} \\N{\\N{\\N{foo}}}", ArrayUtils.asSet());
        checkExprs("\\N{foo} \\N{foo} {foo}", ArrayUtils.asSet("foo"));

        checkExprs("{foo=}", ArrayUtils.asSet("foo"));
        checkExprs("\\N{foo} {foo=}", ArrayUtils.asSet("foo"));
        checkExprs("\\N{foo}{foo=}", ArrayUtils.asSet("foo"));
        checkExprs("\\N{foo} \\N{foo} {foo=}", ArrayUtils.asSet("foo"));

        checkExprs("{val:{width}.{precision}f}", ArrayUtils.asSet("val", "width", "precision"));
        checkExprs("{a:>{width}}", ArrayUtils.asSet("a", "width"));
        checkExprs("{a:>{{width}}}", ArrayUtils.asSet("a"));
        checkExprs("{{{test}", ArrayUtils.asSet("test"));
        checkExprs("{{{test}}}", ArrayUtils.asSet("test"));
        checkExprs("{a:{width}}", ArrayUtils.asSet("a", "width"));
        checkExprs("{{name:{var1}.{var2}s}} {{message}}", ArrayUtils.asSet("var1", "var2"));
        checkExprs("name:{var1}.{var2}s message", ArrayUtils.asSet("var1", "var2"));

        checkExprs("{var:>{width}}", ArrayUtils.asSet("var", "width"));
        checkExprs("{{'c':20}}", ArrayUtils.asSet()); // {{ is just a single '{' char and }} is a single '}' char, so, this is just text.
        checkExprs("{'c':20}", ArrayUtils.asSet("'c'")); // Just a 'c' char
        checkExprs("{c:20}", ArrayUtils.asSet("c")); // c name
        checkExprs("{call(b,c)}", ArrayUtils.asSet("call(b,c)"));
        checkExprs("{call(b|c)}", ArrayUtils.asSet("call(b|c)"));
        checkExprs("{call({b},c)}", ArrayUtils.asSet("call({b},c)"));

        checkExprs("{val:{width}.{precision}f}", ArrayUtils.asSet("val", "width", "precision"));
        checkExprs("a{ {text} }", ArrayUtils.asSet("{text}")); // {text} == set([text])
        checkExprs("a{text}a{text2}b", ArrayUtils.asSet("text", "text2"));
        checkExprs("{text!a}", ArrayUtils.asSet("text"));
        checkExprs("{text!s}", ArrayUtils.asSet("text"));
        checkExprs("{text!r}", ArrayUtils.asSet("text"));
        checkExprs("{text!r:foo}", ArrayUtils.asSet("text"));
        checkExprs("{text:#.6f}", ArrayUtils.asSet("text"));
        checkExprs("{text#.6f}", ArrayUtils.asSet("text#.6f"));
        checkExprs("{text:}", ArrayUtils.asSet("text"));
        checkExprs("newline: {call({b})}", ArrayUtils.asSet("call({b})"));
        checkExprs("newline: {call({b},{c})}", ArrayUtils.asSet("call({b},{c})"));
        checkExprs("newline: {call(\"{b}\")}", ArrayUtils.asSet("call(\"{b}\")"));
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
