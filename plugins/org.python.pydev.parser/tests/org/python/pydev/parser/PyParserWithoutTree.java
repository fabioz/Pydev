/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.parser;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ParseException;
import org.python.pydev.shared_core.parsing.BaseParser.ParseOutput;

public class PyParserWithoutTree extends TestCase {

    public void testGenerateParseError() throws Exception {
        String contents = "" + "'''class Bar:\n" + "    '''\n" + "    class Foo:\n"
                + "        a+b #should not be code-formatted\n" + "    '''\n" + "'''\n" + "\n";
        IGrammarVersionProvider grammarProvider = new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IPythonNature.GRAMMAR_PYTHON_VERSION_2_7;
            }
        };
        ParseOutput tuple = PyParser.reparseDocument(new PyParser.ParserInfo(new Document(contents),
                grammarProvider, false));

        assertTrue("Found: " + tuple.error, tuple.error instanceof ParseException);

        tuple = PyParser.reparseDocument(new PyParser.ParserInfo(new Document("a = 10"), grammarProvider, false));

        assertTrue("Found: " + tuple.error, tuple.error == null);
    }
}
