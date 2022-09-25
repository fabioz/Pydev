/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Dec 21, 2004
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.ast.codecompletion.revisited.visitors;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.Str;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.shared_core.string.FullRepIterable;

/**
 * @author Fabio Zadrozny
 */
public abstract class AbstractVisitor extends VisitorBase {

    /**
     * The constants below may be combined for a single request
     */
    public static final int GLOBAL_TOKENS = 1;

    public static final int WILD_MODULES = 2;

    public static final int ALIAS_MODULES = 4;

    public static final int MODULE_DOCSTRING = 8;

    /**
     * This constant cannot be combined with any of the others
     */
    public static final int INNER_DEFS = 16;

    protected final List<IToken> tokens = new ArrayList<IToken>();

    /**
     * Module being visited.
     */
    protected String moduleName;

    protected final IPythonNature nature;

    /**
     * This is the module we are visiting: just a weak reference so that we don't create a cycle (let's
     * leave things easy for the garbage collector).
     */
    protected final WeakReference<IModule> module;

    public AbstractVisitor(IPythonNature nature, IModule module) {
        this.nature = nature;
        this.module = new WeakReference<IModule>(module);
    }

    /**
     * Adds a token with a docstring.
     *
     * @param node
     */
    protected SourceToken addToken(SimpleNode node) {
        //add the token
        SourceToken t = makeToken(node, moduleName, nature, module.get());
        this.tokens.add(t);
        return t;
    }

    public static SourceToken makeToken(SimpleNode node, String moduleName, IPythonNature nature, IModule module) {
        return new SourceToken(node, NodeUtils.getRepresentationString(node), NodeUtils.getNodeArgs(node),
                NodeUtils.getNodeDocString(node), moduleName, nature, module);
    }

    public static SourceToken makeToken(SimpleNode node, String rep, String moduleName, IPythonNature nature,
            IModule module) {
        return new SourceToken(node, rep, NodeUtils.getNodeArgs(node), NodeUtils.getNodeDocString(node), moduleName,
                nature, module);
    }

    /**
     * same as make token, but returns the full representation for a token, instead of just a 'partial' name
     */
    public static SourceToken makeFullNameToken(SimpleNode node, String moduleName, IPythonNature nature,
            IModule module) {
        return new SourceToken(node, NodeUtils.getFullRepresentationString(node), NodeUtils.getNodeArgs(node),
                NodeUtils.getNodeDocString(node), moduleName, nature, module);
    }

    /**
     * This function creates source tokens from a wild import node.
     *
     * @param node the import node
     * @param tokens OUT used to add the source token
     * @param moduleName the module name
     *
     * @return the tokens list passed in or the created one if it was null
     */
    public static IToken makeWildImportToken(ImportFrom node, List<IToken> tokens, String moduleName,
            IPythonNature nature, IModule module) {
        if (tokens == null) {
            tokens = new ArrayList<IToken>();
        }
        SourceToken sourceToken = null;
        if (isWildImport(node)) {
            sourceToken = new SourceToken(node, ((NameTok) node.module).id, "", "", moduleName, nature, module);
            tokens.add(sourceToken);
        }
        return sourceToken;
    }

    public static List<IToken> makeImportToken(SimpleNode node, List<IToken> tokens, String moduleName,
            boolean allowForMultiple, IPythonNature nature, IModule module) {
        if (node instanceof Import) {
            return makeImportToken((Import) node, tokens, moduleName, allowForMultiple, nature, module);
        }
        if (node instanceof ImportFrom) {
            ImportFrom i = (ImportFrom) node;
            if (isWildImport(i)) {
                makeWildImportToken(i, tokens, moduleName, nature, module);
                return tokens;
            }
            return makeImportToken((ImportFrom) node, tokens, moduleName, allowForMultiple, nature, module);
        }

        throw new RuntimeException("Unable to create token for the passed import (" + node + ")");
    }

    /**
     * This function creates source tokens from an import node.
     *
     * @param node the import node
     * @param moduleName the module name where this token was found
     * @param tokens OUT used to add the source tokens (may create many from a single import)
     * @param allowForMultiple is used to indicate if an import in the format import os.path should generate one token for os
     * and another for os.path or just one for both with os.path
     *
     * @return the tokens list passed in or the created one if it was null
     */
    public static List<IToken> makeImportToken(Import node, List<IToken> tokens, String moduleName,
            boolean allowForMultiple, IPythonNature nature, IModule module) {
        aliasType[] names = node.names;
        return makeImportToken(node, tokens, names, moduleName, "", nature, module);
    }

    /**
     * The same as above but with ImportFrom
     */
    public static List<IToken> makeImportToken(ImportFrom node, List<IToken> tokens, String moduleName,
            boolean allowForMultiple, IPythonNature nature, IModule module) {
        aliasType[] names = node.names;
        String importName = ((NameTok) node.module).id;

        return makeImportToken(node, tokens, names, moduleName, importName, nature, module);
    }

    /**
     * This class is the same as a regular source token, just used to know that this
     * is a token that was created to identify a part of an import declaration.
     *
     * E.g.:
     *
     * import os.path
     *
     * Will create an 'os' part -- which is leaked to the namespace (but we must
     * identify that because we don't want to report import redefinitions nor unused
     * variables for those).
     *
     * See: https://sourceforge.net/tracker/index.php?func=detail&aid=2879058&group_id=85796&atid=577329
     * and  https://sourceforge.net/tracker/index.php?func=detail&aid=2008026&group_id=85796&atid=577329
     */
    public static class ImportPartSourceToken extends SourceToken {

        private static final long serialVersionUID = 1L;

        public ImportPartSourceToken(SimpleNode node, String rep, String doc, String args, String parentPackage,
                String originalRep, boolean originalHasRep, IPythonNature nature, IModule module) {
            super(node, rep, doc, args, parentPackage, originalRep, originalHasRep, nature, module);
        }
    }

    /**
     * The same as above
     */
    private static List<IToken> makeImportToken(SimpleNode node, List<IToken> tokens, aliasType[] names,
            String moduleName,
            String initialImportName, IPythonNature nature, IModule module) {
        if (tokens == null) {
            tokens = new ArrayList<IToken>();
        }

        if (initialImportName.length() > 0) {
            initialImportName = initialImportName + ".";
        }

        for (int i = 0; i < names.length; i++) {
            aliasType aliasType = names[i];

            String name = null;
            String original = ((NameTok) aliasType.name).id;

            if (aliasType.asname != null) {
                name = ((NameTok) aliasType.asname).id;
            }

            if (name == null) {
                FullRepIterable iterator = new FullRepIterable(original);
                Iterator<String> it = iterator.iterator();
                while (it.hasNext()) {
                    String rep = it.next();
                    SourceToken sourceToken;
                    if (it.hasNext()) {
                        sourceToken = new ImportPartSourceToken(node, rep, "", "", moduleName, initialImportName + rep,
                                true, nature, module);

                    } else {
                        sourceToken = new SourceToken(node, rep, "", "", moduleName, initialImportName + rep, true,
                                nature, module);
                    }
                    tokens.add(sourceToken);
                }
            } else {
                SourceToken sourceToken = new SourceToken(node, name, "", "", moduleName, initialImportName + original,
                        false, nature, module);
                tokens.add(sourceToken);
            }

        }
        return tokens;
    }

    public static boolean isString(SimpleNode ast) {
        if (ast instanceof Str) {
            return true;
        }
        return false;
    }

    /**
     * @param node the node to analyze
     * @return whether it is a wild import
     */
    public static boolean isWildImport(ImportFrom node) {
        return node.names.length == 0;
    }

    /**
     * @param node the node to analyze
     * @return whether it is an alias import
     */
    public static boolean isAliasImport(ImportFrom node) {
        return node.names.length > 0;
    }

    public List<IToken> getTokens() {
        return this.tokens;
    }

    /**
     * This method transverses the ast and returns a list of found tokens.
     *
     * @param ast
     * @param which
     * @param state
     * @param name
     * @param onlyAllowTokensIn__all__: only used when checking global tokens: if true, if a token named __all__ is available,
     * only the classes that have strings that match in __all__ are available.
     * @return
     * @throws Exception
     */
    public static List<IToken> getTokens(SimpleNode ast, int which, String moduleName, ICompletionState state,
            boolean onlyAllowTokensIn__all__, IPythonNature nature, IModule module) {
        AbstractVisitor modelVisitor;
        if (which == INNER_DEFS) {
            modelVisitor = new InnerModelVisitor(moduleName, state, nature, module);
        } else {
            modelVisitor = new GlobalModelVisitor(which, moduleName, onlyAllowTokensIn__all__, nature, module);
        }

        if (ast != null) {
            try {
                ast.accept(modelVisitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            modelVisitor.finishVisit();
            return modelVisitor.tokens;
        } else {
            return new ArrayList<IToken>();
        }
    }

    /**
     * This method traverses the ast and returns a model visitor that has the list of found tokens (and other related info, such as __all__, etc.)
     * @param sourceModule
     */
    public static GlobalModelVisitor getGlobalModuleVisitorWithTokens(SimpleNode ast, int which, String moduleName,
            boolean onlyAllowTokensIn__all__, IPythonNature nature, SourceModule sourceModule) {
        if (which == INNER_DEFS) {
            throw new RuntimeException("Only globals for getting the GlobalModelVisitor");
        }
        GlobalModelVisitor modelVisitor = new GlobalModelVisitor(which, moduleName, onlyAllowTokensIn__all__,
                nature, sourceModule);

        if (ast != null) {
            try {
                ast.accept(modelVisitor);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            modelVisitor.finishVisit();
            return modelVisitor;
        } else {
            return modelVisitor;
        }
    }

    /**
     * This method is available so that subclasses can do some post-processing before the tokens are actually
     * returned.
     */
    protected void finishVisit() {
        /**Empty**/
    }

}
