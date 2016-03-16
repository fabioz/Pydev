/******************************************************************************
* Copyright (C) 2012-2013  Jonah Graham and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Jonah Graham <jonah@kichwacoders.com> - initial API and implementation
*     Fabio Zadrozny <fabiofz@gmail.com>    - ongoing maintenance
******************************************************************************/
package org.python.pydev.shared_interactive_console.console.codegen;

import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.python.pydev.shared_core.string.FastStringBuffer;

/**
 * A code generator for an IStructuredSelection object.
 * 
 * @see IScriptConsoleCodeGenerator
 */
public class StructuredSelectionScriptConsoleCodeGenerator implements IScriptConsoleCodeGenerator {

    private final IStructuredSelection selection;

    public StructuredSelectionScriptConsoleCodeGenerator(IStructuredSelection selection) {
        this.selection = selection;
    }

    private IScriptConsoleCodeGenerator getPyConsoleCodeGenerator(Object node) {
        return PythonSnippetUtils.getScriptConsoleCodeGeneratorAdapter(node);
    }

    private boolean hasPyCode(Object node) {
        IScriptConsoleCodeGenerator generator = getPyConsoleCodeGenerator(node);
        if (generator == null) {
            return false;
        }
        return generator.hasPyCode();
    }

    private String getPyCode(Object node) {
        IScriptConsoleCodeGenerator generator = getPyConsoleCodeGenerator(node);
        if (generator == null) {
            return null;
        }
        if (!generator.hasPyCode()) {
            return null;
        }
        return generator.getPyCode();
    }

    @Override
    public String getPyCode() {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection iStructuredSelection = (IStructuredSelection) selection;
            if (iStructuredSelection.isEmpty()) {
                return null;
            }
            @SuppressWarnings("rawtypes")
            List list = iStructuredSelection.toList();
            if (list.size() == 1) {
                return getPyCode(list.get(0));
            } else {
                FastStringBuffer sb = new FastStringBuffer();
                sb.append("(");
                for (Object object : list) {
                    String pyCode = getPyCode(object);
                    if (pyCode == null || pyCode.length() == 0) {
                        return null;
                    }
                    sb.append(pyCode);
                    sb.append(", ");
                }
                if (sb.endsWith(", ")) {
                    sb.deleteLastChars(2); // get rid of last ", "
                }
                sb.append(")");
                return sb.toString();
            }
        }

        return null;
    }

    @Override
    public boolean hasPyCode() {
        if (selection instanceof IStructuredSelection) {
            IStructuredSelection iStructuredSelection = (IStructuredSelection) selection;
            if (iStructuredSelection.isEmpty()) {
                return false;
            }
            @SuppressWarnings("rawtypes")
            List list = iStructuredSelection.toList();
            for (Object object : list) {
                if (!hasPyCode(object)) {
                    return false;
                }
            }
            return true;
        }

        return false;
    }

}
