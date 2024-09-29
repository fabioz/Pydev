/**
 * Copyright (c) 2005-2011 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package com.python.pydev.analysis.refactoring.tdd;

import java.util.List;

import org.python.pydev.ast.refactoring.RefactoringInfo;

public class NullPyCreateAction extends AbstractPyCreateAction {

    @Override
    public TemplateInfo createProposal(RefactoringInfo refactoringInfo, String actTok,
            int locationStrategy,
            List<String> parametersAfterCall) {
        return null;
    }

}
