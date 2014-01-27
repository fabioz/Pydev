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

package org.python.pydev.refactoring.tests.visitors;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.MisconfigurationException;
import org.python.pydev.refactoring.ast.visitors.rewriter.Rewriter;
import org.python.pydev.refactoring.core.base.RefactoringInfo;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

import com.thoughtworks.xstream.XStream;

public class SelectionExtensionTestCase extends AbstractIOTestCase {

    public SelectionExtensionTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        MockupSelectionConfig config = initConfig();

        RefactoringInfo info = setupInfo(config);
        runSelectionExtension(info);

        assertEquals(getExpected(), getGenerated());
    }

    private void runSelectionExtension(RefactoringInfo info) {
        StringBuilder buffer = new StringBuilder();
        try {
            String source = Rewriter.createSourceFromAST(info.getParsedExtendedSelection().getASTParent(), "\n",
                    new IGrammarVersionProvider() {

                        public int getGrammarVersion() throws MisconfigurationException {
                            return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
                        }
                    });
            buffer.append(source); // normalized source
        } catch (Throwable e) {
            buffer.append("# Invalid selection:\n");
            String[] lines = info.normalizeSourceSelection(info.getExtendedSelection()).split("\\n"); // normalized

            for (String line : lines) {
                if (line.trim().length() != 0) {
                    buffer.append("# " + line + "\n");
                }
            }
        }

        setTestGenerated(buffer.toString());
    }

    private RefactoringInfo setupInfo(MockupSelectionConfig config) throws Throwable {
        IDocument doc = new Document(data.source);

        ITextSelection selection = new TextSelection(doc, data.sourceSelection.getOffset(),
                data.sourceSelection.getLength());
        RefactoringInfo info = new RefactoringInfo(doc, selection, new IGrammarVersionProvider() {

            public int getGrammarVersion() throws MisconfigurationException {
                return IGrammarVersionProvider.GRAMMAR_PYTHON_VERSION_2_7;
            }
        });

        return info;
    }

    private MockupSelectionConfig initConfig() {
        MockupSelectionConfig config = null;
        XStream xstream = new XStream();
        xstream.alias("config", MockupSelectionConfig.class);

        if (data.config.length() > 0) {
            config = (MockupSelectionConfig) xstream.fromXML(data.getConfigContents());
        } else {
            config = new MockupSelectionConfig(0, 0, 0);
        }
        return config;
    }
}
