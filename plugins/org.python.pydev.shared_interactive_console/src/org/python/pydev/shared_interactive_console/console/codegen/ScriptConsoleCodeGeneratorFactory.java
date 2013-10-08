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

import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.jface.viewers.IStructuredSelection;

@SuppressWarnings("rawtypes")
public class ScriptConsoleCodeGeneratorFactory implements IAdapterFactory {
    public Object getAdapter(Object adaptableObject, Class adapterType) {
        if (adapterType == IScriptConsoleCodeGenerator.class) {
            if (adaptableObject instanceof IStructuredSelection) {
                IStructuredSelection selection = (IStructuredSelection) adaptableObject;
                return new StructuredSelectionScriptConsoleCodeGenerator(selection);
            }
        }
        return null;
    }

    public Class[] getAdapterList() {
        return new Class[] { IScriptConsoleCodeGenerator.class };
    }
}
