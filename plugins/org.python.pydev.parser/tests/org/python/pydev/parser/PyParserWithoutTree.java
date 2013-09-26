package org.python.pydev.parser;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.shared_core.model.ISimpleNode;
import org.python.pydev.shared_core.structure.Tuple;

public class PyParserWithoutTree extends TestCase {

    public void testGenerateParseError() throws Exception {
        String contents = "" + "'''class Bar:\n" + "    '''\n" + "    class Foo:\n"
                + "        a+b #should not be code-formatted\n" + "    '''\n" + "'''\n" + "\n";
        IGrammarVersionProvider grammarProvider = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IPythonNature.GRAMMAR_PYTHON_VERSION_2_7;
            }
        };
        Tuple<ISimpleNode, Throwable> tuple = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(contents),
                grammarProvider, false));

        assertTrue("Found: " + tuple.o2, tuple.o2 instanceof ParseException);

        tuple = PyParser.reparseDocument(new PyParser.ParserInfo(new Document("a = 10"), grammarProvider, false));

        assertTrue("Found: " + tuple.o2, tuple.o2 == null);
    }
}
