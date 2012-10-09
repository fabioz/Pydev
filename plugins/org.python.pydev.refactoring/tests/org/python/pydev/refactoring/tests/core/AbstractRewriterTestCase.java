/* 
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 * Copyright (C) 2007  Reto Schuettel, Robin Stocker
 */

package org.python.pydev.refactoring.tests.core;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.refactoring.ast.visitors.rewriter.Rewriter;

public abstract class AbstractRewriterTestCase extends AbstractIOTestCase {

    public AbstractRewriterTestCase(String name) {
        this(name, false);
    }

    public AbstractRewriterTestCase(String name, boolean ignoreEmptyLines) {
        super(name, ignoreEmptyLines);
    }

    protected void runRewriter() throws Throwable {
        setTestGenerated(Rewriter.reparsed(data.source, new AdapterPrefs("\n", new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        })));
    }

}
