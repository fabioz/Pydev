/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on May 16, 2006
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.core.FullRepIterable;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.core.docutils.PySelection;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;
import org.python.pydev.editor.codecompletion.revisited.visitors.AbstractVisitor;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.NameTok;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_core.structure.Tuple4;

import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.ImportChecker.ImportInfo;

/**
 * This class is used to discover the occurrences of some token having its scope as something important.
 */
public class ScopeAnalyzerVisitor extends ScopeAnalyzerVisitorWithoutImports {

    /**
     * Constructor when we have a PySelection object
     * @throws BadLocationException
     */
    public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current, IProgressMonitor monitor,
            PySelection ps) throws BadLocationException {
        super(nature, moduleName, current, monitor, ps);
    }

    public ScopeAnalyzerVisitor(IPythonNature nature, String moduleName, IModule current, IDocument document,
            IProgressMonitor monitor, String pNameToFind, int absoluteCursorOffset, String[] tokenAndQual)
            throws BadLocationException {
        super(nature, moduleName, current, document, monitor, pNameToFind, absoluteCursorOffset, tokenAndQual);
    }

    /**
     * This is the key in the importsFoundFromModuleName for tokens that were not resolved.
     */
    private static final String UNRESOLVED_MOD_NAME = "__UNRESOLVED__MOD__NAME__!";

    /**
     * It contains import artificially generated, such as module names in ImportFrom
     * E.g.: from os.path import xxx will generate an import for 'os' and an import for 'path'
     * artificially, just to make matches
     */
    protected Map<String, List<Tuple3<Found, Integer, ASTEntry>>> importsFoundFromModuleName = new HashMap<String, List<Tuple3<Found, Integer, ASTEntry>>>();
    /**
     * Same as the importsFoundFromModuleName, but works on the imports that actually become tokens
     * in the namespace.
     */
    protected Map<String, List<Tuple3<Found, Integer, ASTEntry>>> importsFound = new HashMap<String, List<Tuple3<Found, Integer, ASTEntry>>>();

    /**
     * Used to add some Found that is related to an import to a 'global import register'.
     * This is needed because, unlike other regular tokens, we want to find imports that are
     * in diferent contexts as being in the same context.
     * 
     * @param found this is the Found that we want to add to the imports
     * @param map this is the map that contains the imports Found occurrences (it has to be passed,
     * as there is a map for the imports that are actually in the namespace and another for those
     * that are 'artificially' generated).
     */
    private void addFoundToImportsMap(Found found, Map<String, List<Tuple3<Found, Integer, ASTEntry>>> map) {
        ImportInfo info = found.importInfo;
        String modName = UNRESOLVED_MOD_NAME;
        if (info != null && info.mod != null) {
            modName = info.mod.getName();
        }
        List<Tuple3<Found, Integer, ASTEntry>> prev = map.get(modName);
        if (prev == null) {
            prev = new ArrayList<Tuple3<Found, Integer, ASTEntry>>();
            map.put(modName, prev);
        }

        prev.add(new Tuple3<Found, Integer, ASTEntry>(found, -1, peekParent())); //col delta is undefined
    }

    @Override
    public Object visitImportFrom(ImportFrom node) throws Exception {
        Object ret = super.visitImportFrom(node);
        //the import from will generate the tokens that go into the module namespace, but still, it needs to
        //create tokens that will not be used in code-analysis, but will be used in matching tokens
        //regarding its module.
        NameTok tokModName = (NameTok) node.module;
        for (String m : new FullRepIterable(tokModName.id)) {
            if (m.indexOf(".") == -1) {
                aliasType[] names = new aliasType[1];
                NameTok importNameTok = new NameTok(m, NameTok.ImportModule);

                importNameTok.beginLine = tokModName.beginLine;
                importNameTok.beginColumn = tokModName.beginColumn;

                names[0] = new aliasType(importNameTok, null);
                names[0].beginLine = tokModName.beginLine;
                names[0].beginColumn = tokModName.beginColumn;

                Import importTok = new Import(names);
                importTok.beginLine = tokModName.beginLine;
                importTok.beginColumn = tokModName.beginColumn;

                List<IToken> createdTokens = AbstractVisitor.makeImportToken(importTok, null, this.current.getName(),
                        true);
                for (IToken token : createdTokens) {
                    ImportInfo info = this.scope.importChecker.visitImportToken(token, false, this.completionCache);
                    Found found = new Found(token, token, scope.getCurrScopeId(), scope.getCurrScopeItems());
                    found.importInfo = info;

                    checkFound(found, peekParent()); //check if this is actually a match
                    addFoundToImportsMap(found, importsFoundFromModuleName);
                }
            }
        }
        return ret;
    }

    @Override
    public void onImportInfoSetOnFound(Found found) {
        super.onImportInfoSetOnFound(found);
        addFoundToImportsMap(found, importsFound);
    }

    /**
     * Called for each found occurrence in the complete token occurrences
     * 
     * Is used to add other returns to ret
     */
    @Override
    protected void onGetCompleteTokenOccurrences(Tuple3<Found, Integer, ASTEntry> found, Set<IToken> f,
            ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> ret) {

        //other matches for the imports that we had already found.
        Tuple<List<Tuple4<IToken, Integer, ASTEntry, Found>>, List<Tuple4<IToken, Integer, ASTEntry, Found>>> matchingImportEntries = getImportEntries(
                found, f);
        List<Tuple4<IToken, Integer, ASTEntry, Found>> fromModule = matchingImportEntries.o1;
        List<Tuple4<IToken, Integer, ASTEntry, Found>> fromImports = matchingImportEntries.o2;

        ret.addAll(fromModule);
        ret.addAll(fromImports);

        //let's iterate through the imports that are to be recognized as the same import we're checking and get the
        //individual occurrences for each one of those (but this only happens if the context of the current 
        //import is different from the context of that import)
        for (Tuple4<IToken, Integer, ASTEntry, Found> tuple3 : fromImports) {

            try {
                if (!(tuple3.o1 instanceof SourceToken)) {
                    continue;
                }

                SourceToken tok = (SourceToken) tuple3.o1;
                SimpleNode ast = tok.getAst();
                int line = 0;
                int col = 0;
                if (!(ast instanceof Import)) {
                    continue;
                }

                Import import1 = (Import) ast;
                line = import1.names[0].beginLine - 1;
                col = import1.names[0].beginColumn - 1;
                PySelection ps = new PySelection(this.document, line, col);
                ScopeAnalyzerVisitorWithoutImports analyzerVisitorWithoutImports = new ScopeAnalyzerVisitorWithoutImports(
                        this.nature, this.moduleName, this.current, this.monitor, ps);

                SourceModule s = (SourceModule) this.current;
                s.getAst().accept(analyzerVisitorWithoutImports);
                analyzerVisitorWithoutImports.checkFinished();

                //now, let's get the token occurrences for the analyzer that worked without gathering the imports
                ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> completeTokenOccurrences = analyzerVisitorWithoutImports
                        .getCompleteTokenOccurrences();

                for (Tuple4<IToken, Integer, ASTEntry, Found> oc : completeTokenOccurrences) {
                    if (!f.contains(oc.o1) && !oc.o1.isImport()) { //the import should be already added
                        if (oc.o2 < tuple3.o2) {
                            oc.o2 = tuple3.o2;
                        }
                        f.add(oc.o1);
                        ret.add(oc);
                    }
                }

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * This method finds entries for found tokens that are the same import, but that may still not be there
     * because they are either in some other scope or are in the module part of an ImportFrom
     */
    private Tuple<List<Tuple4<IToken, Integer, ASTEntry, Found>>, List<Tuple4<IToken, Integer, ASTEntry, Found>>> getImportEntries(
            Tuple3<Found, Integer, ASTEntry> found, Set<IToken> f) {
        List<Tuple4<IToken, Integer, ASTEntry, Found>> fromModuleRet = new ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>>();
        List<Tuple4<IToken, Integer, ASTEntry, Found>> fromImportsRet = new ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>>();
        if (found.o1.isImport()) {
            //now, as it is an import, we have to check if there are more matching imports found
            String key = UNRESOLVED_MOD_NAME;
            if (found.o1.importInfo != null && found.o1.importInfo.mod != null) {
                key = found.o1.importInfo.mod.getName();
            }
            List<Tuple3<Found, Integer, ASTEntry>> fromModule = importsFoundFromModuleName.get(key);
            List<Tuple3<Found, Integer, ASTEntry>> fromImports = importsFound.get(key);

            checkImportEntries(fromModuleRet, f, fromModule, found.o2);
            checkImportEntries(fromImportsRet, f, fromImports, found.o2);

        }
        return new Tuple<List<Tuple4<IToken, Integer, ASTEntry, Found>>, List<Tuple4<IToken, Integer, ASTEntry, Found>>>(
                fromModuleRet, fromImportsRet);
    }

    /**
     * Checks the import entries for imports that are the same as the one that should be already found.
     */
    private void checkImportEntries(List<Tuple4<IToken, Integer, ASTEntry, Found>> ret, Set<IToken> f,
            List<Tuple3<Found, Integer, ASTEntry>> importEntries, int colDelta) {

        if (importEntries != null) {
            for (Tuple3<Found, Integer, ASTEntry> foundInFromModule : importEntries) {
                IToken generator = foundInFromModule.o1.getSingle().generator;

                Tuple4<IToken, Integer, ASTEntry, Found> tup3 = new Tuple4<IToken, Integer, ASTEntry, Found>(generator,
                        colDelta > foundInFromModule.o2 ? colDelta : foundInFromModule.o2, foundInFromModule.o3,
                        foundInFromModule.o1);

                if (!f.contains(generator)) {
                    f.add(generator);
                    ret.add(tup3);
                }
            }
        }
    }

}
