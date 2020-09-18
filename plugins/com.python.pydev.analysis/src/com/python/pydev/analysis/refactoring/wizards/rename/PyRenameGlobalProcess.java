/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
/*
 * Created on Apr 9, 2006
 */
package com.python.pydev.analysis.refactoring.wizards.rename;

import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.ast.codecompletion.revisited.modules.SourceModule;
import org.python.pydev.ast.codecompletion.revisited.visitors.Definition;
import org.python.pydev.ast.refactoring.RefactoringRequest;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Import;
import org.python.pydev.parser.jython.ast.ImportFrom;
import org.python.pydev.parser.jython.ast.aliasType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.visitors.NodeUtils;
import org.python.pydev.parser.visitors.scope.ASTEntry;
import org.python.pydev.shared_core.string.FastStringBuffer;
import org.python.pydev.shared_core.string.StringUtils;

import com.python.pydev.analysis.scopeanalysis.ScopeAnalysis;

public class PyRenameGlobalProcess extends AbstractRenameWorkspaceRefactorProcess {

    /**
     * @param definition
     */
    public PyRenameGlobalProcess(Definition definition) {
        super(definition);
    }

    @Override
    protected boolean getRecheckWhereDefinitionWasFound() {
        return false;
    }

    @Override
    protected List<ASTEntry> findReferencesOnOtherModule(RefactoringStatus status, RefactoringRequest request,
            String initialName, SourceModule module) {
        SimpleNode searchStringsAt = module.getAst();

        List<String> modNames = StringUtils.split(request.moduleName, '.');
        List<String> actualModNames = StringUtils.split(module.getName(), '.');
        FastStringBuffer buf = new FastStringBuffer();
        int i = 0;
        while (i < modNames.size() && i < actualModNames.size() && modNames.get(i).equals(actualModNames.get(i))) {
            i++;
        }
        while (i < modNames.size()) {
            buf.append(modNames.get(i)).append('.');
            i++;
        }
        buf.deleteLast();
        String compare = buf.toString();
        boolean valid = false;
        for (stmtType content : NodeUtils.getBody(searchStringsAt)) {
            if (content instanceof Import) {
                valid = checkImportNames(compare, ((Import) content).names);
            } else if (content instanceof ImportFrom) {
                valid = checkImportNames(compare, ((ImportFrom) content).names);
            }
            if (valid) {
                break;
            }
        }

        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(initialName, module.getAst(), !valid);
        if (ret.size() > 0 && searchStringsAt != null) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.qualifier, searchStringsAt));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.qualifier, searchStringsAt));
        }

        return ret;
    }

    private boolean checkImportNames(String compare, aliasType[] names) {
        for (aliasType name : names) {
            if (compare.equals(NodeUtils.getNameForAlias(name).id)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void findReferencesToRenameOnLocalScope(RefactoringRequest request, RefactoringStatus status) {
        SimpleNode ast = request.getAST();
        //it was found in another module, but we want to keep things local
        List<ASTEntry> ret = ScopeAnalysis.getLocalOccurrences(request.qualifier, ast,
                request.activationToken.isEmpty());
        if (ret.size() > 0) {
            //only add comments and strings if there's at least some other occurrence
            ret.addAll(ScopeAnalysis.getCommentOccurrences(request.qualifier, ast));
            ret.addAll(ScopeAnalysis.getStringOccurrences(request.qualifier, ast));
        }
        addOccurrences(request, ret);
    }

}
