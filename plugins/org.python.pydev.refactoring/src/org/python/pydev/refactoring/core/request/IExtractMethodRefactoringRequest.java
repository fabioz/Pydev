/******************************************************************************
* Copyright (C) 2007-2012  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.core.request;

import org.python.pydev.refactoring.ast.adapters.AbstractScopeNode;

public interface IExtractMethodRefactoringRequest {

    /**
     * @return The scope containing the current selection.
     */
    AbstractScopeNode<?> getScopeAdapter();
}
