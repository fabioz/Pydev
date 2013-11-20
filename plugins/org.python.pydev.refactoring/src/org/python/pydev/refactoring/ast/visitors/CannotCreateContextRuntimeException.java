/******************************************************************************
* Copyright (C) 2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.refactoring.ast.visitors;

public class CannotCreateContextRuntimeException extends RuntimeException {

    private String msg;

    public CannotCreateContextRuntimeException(Exception e) {
        super(e);
        this.msg = e.getMessage();
    }

    @Override
    public String toString() {
        return msg;
    }

    @Override
    public String getMessage() {
        return msg;
    }

}
