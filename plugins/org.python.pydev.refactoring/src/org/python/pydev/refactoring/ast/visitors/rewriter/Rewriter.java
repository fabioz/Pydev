/******************************************************************************
* Copyright (C) 2009-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.ast.visitors.rewriter;

import org.python.pydev.core.IGrammarVersionProvider;
import org.python.pydev.core.IIndentPrefs;
import org.python.pydev.editor.autoedit.DefaultIndentPrefs;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.factory.AdapterPrefs;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;

/**
 * This class just provides an interface for using the rewriter.
 */
public final class Rewriter {

    public static String reparsed(String source, AdapterPrefs adapterPrefs) {
        try {
            SimpleNode root = VisitorFactory.getRootNodeFromString(source, adapterPrefs.versionProvider);
            return createSourceFromAST(root, adapterPrefs);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public static String createSourceFromAST(SimpleNode root, AdapterPrefs adapterPrefs) {
        return createSourceFromAST(root, false, adapterPrefs);
    }

    public static String createSourceFromAST(SimpleNode root, String endLineDelim,
            IGrammarVersionProvider versionProvider) {
        return createSourceFromAST(root, false, new AdapterPrefs(endLineDelim, versionProvider));
    }

    public static String createSourceFromAST(SimpleNode root, boolean ignoreComments, AdapterPrefs adapterPrefs) {
        IGrammarVersionProvider versionProvider = adapterPrefs.versionProvider;
        IIndentPrefs indentPrefs = DefaultIndentPrefs.get(adapterPrefs.projectAdaptable);
        String endLineDelim = adapterPrefs.endLineDelim;

        PrettyPrinterPrefsV2 prettyPrinterPrefs = PrettyPrinterV2.createDefaultPrefs(versionProvider, indentPrefs,
                endLineDelim);

        PrettyPrinterV2 printer = new PrettyPrinterV2(prettyPrinterPrefs);
        try {
            return printer.print(root);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
