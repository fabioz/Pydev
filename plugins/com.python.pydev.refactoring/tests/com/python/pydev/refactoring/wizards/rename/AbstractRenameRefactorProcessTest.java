/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.refactoring.wizards.rename;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.python.pydev.parser.visitors.scope.ASTEntry;

import com.python.pydev.analysis.scopeanalysis.AstEntryScopeAnalysisConstants;

public class AbstractRenameRefactorProcessTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testSort() throws Exception {
        List<ASTEntry> initial = new ArrayList<ASTEntry>();
        createRegular(initial);
        createString(initial);
        createString(initial);
        createComment(initial);
        createRegular(initial);
        createRegular(initial);

        initial = AbstractRenameWorkspaceRefactorProcess.sortOccurrences(initial);

        ArrayList<ASTEntry> expected = new ArrayList<ASTEntry>();
        createRegular(expected);
        createRegular(expected);
        createRegular(expected);
        createString(expected);
        createString(expected);
        createComment(expected);

        compare(initial, expected);
    }

    public void testSort2() throws Exception {
        List<ASTEntry> initial = new ArrayList<ASTEntry>();
        createString(initial);
        createString(initial);
        createRegular(initial);
        createRegular(initial);
        createRegular(initial);
        createComment(initial);

        initial = AbstractRenameWorkspaceRefactorProcess.sortOccurrences(initial);

        ArrayList<ASTEntry> expected = new ArrayList<ASTEntry>();
        createRegular(expected);
        createRegular(expected);
        createRegular(expected);
        createString(expected);
        createString(expected);
        createComment(expected);

        compare(initial, expected);
    }

    private void compare(List<ASTEntry> initial, ArrayList<ASTEntry> expected) {
        StringBuffer buf1 = new StringBuffer();
        for (int i = 0; i < initial.size(); i++) {
            ASTEntry o1 = initial.get(i);
            buf1.append(o1.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0));
        }

        StringBuffer buf2 = new StringBuffer();
        for (int i = 0; i < expected.size(); i++) {
            ASTEntry o1 = expected.get(i);
            buf2.append(o1.getAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION, 0));
        }

        assertEquals(buf1.toString(), buf2.toString());
    }

    private void createString(List<ASTEntry> initial) {
        ASTEntry entry = new ASTEntry(null);
        entry.setAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION,
                AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_STRING);
        add(entry, initial);
    }

    private void createComment(List<ASTEntry> initial) {
        ASTEntry entry = new ASTEntry(null);
        entry.setAdditionalInfo(AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_LOCATION,
                AstEntryScopeAnalysisConstants.AST_ENTRY_FOUND_IN_COMMENT);
        add(entry, initial);
    }

    private void createRegular(List<ASTEntry> initial) {
        add(new ASTEntry(null), initial);
    }

    private void add(ASTEntry entry, List<ASTEntry> initial) {
        initial.add(entry);
    }
}
