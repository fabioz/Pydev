/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on 21/08/2005
 */
package com.python.pydev.analysis.visitors;

import org.python.pydev.core.ICodeCompletionASTManager;
import org.python.pydev.core.ICompletionCache;
import org.python.pydev.core.ICompletionState;
import org.python.pydev.core.IDefinition;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.structure.CompletionRecursionException;
import org.python.pydev.editor.codecompletion.revisited.AbstractASTManager;
import org.python.pydev.editor.codecompletion.revisited.CompletionStateFactory;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.structure.Tuple3;

import com.python.pydev.analysis.scopeanalysis.AbstractScopeAnalyzerVisitor;

/**
 * The import checker not only generates information on errors for unresolved modules, but also gathers
 * dependency information so that we can do incremental building of dependent modules.
 * 
 * @author Fabio
 */
public final class ImportChecker {

    /**
     * This is the nature we are analyzing
     */
    private final IPythonNature nature;

    /**
     * this is the name of the module that we are analyzing
     */
    private final String moduleName;

    private final AbstractScopeAnalyzerVisitor visitor;

    /**
     * This is the information stored about some import:
     * Contains the actual module, the representation in the current module and whether it was resolved or not.
     */
    public static class ImportInfo {
        /**
         * This is the module where this info was found
         */
        public final IModule mod;
        /**
         * This is the token that relates to this import info (in the module it was found)
         */
        public final IToken token;
        /**
         * This is the representation where it was found 
         */
        public final String rep;
        /**
         * Determines whether it was resolved or not (if not resolved, the other attributes may be null)
         */
        public final boolean wasResolved;

        /**
         * Should we use the definition cache?
         */
        private boolean useActualDefinitionCache = false;

        /**
         * This is the cache for the definitions.
         */
        private IDefinition[] definitionCache;

        public ImportInfo(IModule mod, String rep, IToken token, boolean wasResolved) {
            this.mod = mod;
            this.rep = rep;
            this.token = token;
            this.wasResolved = wasResolved;
        }

        @Override
        public String toString() {
            FastStringBuffer buffer = new FastStringBuffer(wasResolved ? 40 : 80);
            buffer.append("ImportInfo(");
            buffer.append(" Resolved:");
            buffer.append(wasResolved);
            if (wasResolved) {
                buffer.append(" Rep:");
                buffer.append(rep);
                buffer.append(" Mod:");
                buffer.append(mod != null ? mod.getName() : "null");
            }
            buffer.append(")");
            return buffer.toString();
        }

        public IDefinition[] getDefinitions(IPythonNature nature, ICompletionCache completionCache) throws Exception {
            if (useActualDefinitionCache) {
                return definitionCache;
            }
            useActualDefinitionCache = true;

            if (this.mod != null) {
                definitionCache = this.mod.findDefinition(
                        CompletionStateFactory.getEmptyCompletionState(this.rep, nature, completionCache), -1, -1,
                        nature);
            } else {
                definitionCache = new IDefinition[0];

            }

            return definitionCache;
        }

        /**
         * @return the definition that matches this import info.
         */
        public Definition getModuleDefinitionFromImportInfo(IPythonNature nature, ICompletionCache completionCache) {
            try {
                IDefinition[] definitions = getDefinitions(nature, completionCache);
                int len = definitions.length;
                for (int i = 0; i < len; i++) {
                    IDefinition definition = definitions[i];
                    if (definition instanceof Definition) {
                        Definition d = (Definition) definition;
                        if (d.module != null && d.value.length() == 0 && d.ast == null) {
                            return d;
                        }

                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }

    /**
     * constructor - will remove all dependency info on the project that we will start to analyze
     */
    public ImportChecker(AbstractScopeAnalyzerVisitor visitor, IPythonNature nature, String moduleName) {
        this.nature = nature;
        this.moduleName = moduleName;
        this.visitor = visitor;
    }

    /**
     * @param token MUST be an import token
     * @param reportUndefinedImports 
     * 
     * @return the module where the token was found and a String representing the way it was found 
     * in the module.
     * 
     * Note: it may return information even if the token was not found in the representation required. This is useful
     * to get dependency info, because it is actually dependent on the module, event though it does not have the
     * token we were looking for.
     */
    public ImportInfo visitImportToken(IToken token, boolean reportUndefinedImports, ICompletionCache completionCache) {
        return visitImportToken(reportUndefinedImports, token, moduleName, nature, visitor, completionCache);
    }

    /**
     * This is so that we can use it without actually being in some visit.
     */
    public static ImportInfo visitImportToken(boolean reportUndefinedImports, IToken token, String moduleName,
            IPythonNature nature, AbstractScopeAnalyzerVisitor visitor, ICompletionCache completionCache) {
        //try to find it as a relative import
        boolean wasResolved = false;
        Tuple3<IModule, String, IToken> modTok = null;
        String checkForToken = "";
        if (token instanceof SourceToken) {

            ICodeCompletionASTManager astManager = nature.getAstManager();
            ICompletionState state = CompletionStateFactory.getEmptyCompletionState(token.getRepresentation(), nature,
                    completionCache);

            try {
                modTok = astManager.findOnImportedMods(new IToken[] { token }, state, moduleName, visitor.current);
            } catch (CompletionRecursionException e1) {
                modTok = null;//unable to resolve it
            }
            if (modTok != null && modTok.o1 != null) {
                checkForToken = modTok.o2;
                if (modTok.o2.length() == 0) {
                    wasResolved = true;

                } else {
                    try {
                        checkForToken = AbstractASTManager.getTokToSearchInOtherModule(modTok);
                        if (astManager.getRepInModule(modTok.o1, checkForToken, nature) != null) {
                            wasResolved = true;
                        }
                    } catch (CompletionRecursionException e) {
                        //not resolved...
                    }
                }
            }

            if (!wasResolved && moduleName != null && moduleName.length() > 0) {
                if (moduleName.equals(token.getRepresentation())
                        || moduleName.equals(token.getRepresentation() + ".__init__")) {
                    wasResolved = true;
                    modTok = new Tuple3<IModule, String, IToken>(visitor.current, "", token);
                    checkForToken = modTok.o2;
                }

            }

            //if it got here, it was not resolved
            if (!wasResolved && reportUndefinedImports) {
                visitor.onAddUnresolvedImport(token);
            }

        }

        //might still return a modTok, even if the token we were looking for was not found.
        if (modTok != null) {
            return new ImportInfo(modTok.o1, checkForToken, modTok.o3, wasResolved);
        } else {
            return new ImportInfo(null, null, null, wasResolved);
        }
    }

}
