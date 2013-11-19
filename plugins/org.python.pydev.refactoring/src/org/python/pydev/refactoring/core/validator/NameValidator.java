/******************************************************************************
* Copyright (C) 2006-2013  IFS Institute for Software and others
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
 *
 * IFS Institute for Software, HSR Rapperswil, Switzerland
 * 
 */

package org.python.pydev.refactoring.core.validator;

import java.util.Arrays;
import java.util.List;

import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;
import org.python.pydev.refactoring.ast.adapters.FunctionDefAdapter;
import org.python.pydev.refactoring.ast.visitors.CannotCreateContextRuntimeException;
import org.python.pydev.refactoring.messages.Messages;

public class NameValidator {
    // match invalid keywords (Python pocket reference)
    // Self is not really a keyword, but 
    private static final List<String> KEYWORDS;

    static {
        KEYWORDS = Arrays.asList(new String[] { "and", "assert", "break", "class", "continue", "def", "del", "elif",
                "else", "except", "exec", "finally", "for", "from", "global", "if", "import", "in", "is", "lambda",
                "not", "or", "pass", "print", "raise", "return", "try", "while", "yield" });
    }

    private static final String NAME_PATTERN = "[a-zA-Z_][a-zA-Z_0-9]*";

    private AbstractScopeNode<?> scopeNode;
    private RefactoringStatus status;

    public NameValidator(RefactoringStatus status, AbstractScopeNode<?> scope) {
        this.status = status;
        this.scopeNode = scope;
    }

    public void validateUniqueVariable(String name) {
        try {
            if (scopeNode.alreadyUsedName(name)) {
                status.addWarning(Messages.format(Messages.validationNameAlreadyUsed, name));
            }
        } catch (CannotCreateContextRuntimeException e) {
            status.addWarning("Unable to check if name is already used.\nReason: " + e.getMessage());
        }
    }

    public void validateVariableName(String name) {
        validateName(name);
        validateNotKeyword(name);
    }

    public void validateMethodName(String name) {
        validateName(name);
        validateNotKeyword(name);
    }

    private void validateNotKeyword(String name) {
        if (KEYWORDS.contains(name)) {
            status.addFatalError(Messages.format(Messages.validationReservedKeyword, name));
        }
    }

    private void validateName(String name) {
        if (name.equals("")) {
            status.addFatalError(Messages.validationNameIsEmpty);
            return;
        }

        if (!name.matches(NAME_PATTERN)) {
            status.addFatalError(Messages.format(Messages.validationContainsInvalidChars, name));
        }
    }

    public void validateUniqueFunction(String name) {
        AbstractScopeNode<?> parentAdapter = scopeNode.getParent();

        if (parentAdapter != null) {
            for (FunctionDefAdapter function : parentAdapter.getFunctions()) {
                if (function.getName().compareTo(name) == 0) {
                    status.addWarning(Messages.format(Messages.validationNameAlreadyUsed, name));
                }
            }
        }

    }
}
