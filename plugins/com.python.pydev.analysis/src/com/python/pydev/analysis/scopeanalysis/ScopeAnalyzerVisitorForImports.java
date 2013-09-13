/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.scopeanalysis;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.python.pydev.core.IModule;
import org.python.pydev.core.IPythonNature;
import org.python.pydev.core.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.editor.codecompletion.revisited.visitors.Definition;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.structure.Tuple3;
import org.python.pydev.shared_core.structure.Tuple4;

import com.python.pydev.analysis.visitors.Found;
import com.python.pydev.analysis.visitors.ImportChecker.ImportInfo;

/**
 * This scope analyzer works finding definitions that are based on some import.
 */
public final class ScopeAnalyzerVisitorForImports extends ScopeAnalyzerVisitor {

    private SourceModule moduleToFind;

    /**
     * @param importInfo we'll try to find matches for the given import info.
     */
    public ScopeAnalyzerVisitorForImports(IPythonNature nature, String moduleName, IModule current,
            IProgressMonitor monitor, String nameToFind, String[] tokenAndQual, SourceModule moduleToFind)
            throws BadLocationException {
        super(nature, moduleName, current, null, monitor, nameToFind, -1, tokenAndQual);
        this.moduleToFind = moduleToFind;
    }

    @Override
    protected boolean checkToken(Found found, IToken generator, ASTEntry parent) {
        if (found == null) {
            return false;
        }
        //now, there's a catch here... the import checker will make the module 'resolved' for any token it found, even
        //if it doesn't end up matching with the token we're looking for... so, we must keep on going with the
        //import definitions until we actually find what we're looking for.
        ImportInfo info = found.importInfo;
        if (info != null && info.wasResolved) {
            if (info.rep.length() != 0 && info.token.isImport()) {
                //we only actually had a match with a module if the representation found is empty
                Definition definition = info.getModuleDefinitionFromImportInfo(nature, this.completionCache);
                if (definition != null && definition.module.getName().equals(this.moduleToFind.getName())) {
                    return true;
                }

            } else if (info.mod.getName().equals(this.moduleToFind.getName())) {
                //ok, exact (and direct) match
                return true;
            }
        }

        return false;
    }

    /**
     * All the occurrences we find are correct occurrences (because we check if it was found by the module it resolves to)
     */
    @Override
    protected ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> getCompleteTokenOccurrences() {
        ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> ret = new ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>>();

        addImports(ret, importsFound);
        addImports(ret, importsFoundFromModuleName);
        return ret;
    }

    private void addImports(ArrayList<Tuple4<IToken, Integer, ASTEntry, Found>> ret,
            Map<String, List<Tuple3<Found, Integer, ASTEntry>>> map) {
        for (List<Tuple3<Found, Integer, ASTEntry>> fList : map.values()) {
            for (Tuple3<Found, Integer, ASTEntry> foundInFromModule : fList) {
                IToken generator = foundInFromModule.o1.getSingle().generator;

                Tuple4<IToken, Integer, ASTEntry, Found> tup3 = new Tuple4<IToken, Integer, ASTEntry, Found>(generator,
                        0, foundInFromModule.o3,
                        foundInFromModule.o1);
                ret.add(tup3);
            }
        }
    }
}
