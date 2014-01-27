/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
*     Reto Schuettel
*     Robin Stocker
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
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
