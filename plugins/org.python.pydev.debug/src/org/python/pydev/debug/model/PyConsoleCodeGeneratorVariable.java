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
package org.python.pydev.debug.model;

import org.python.pydev.shared_interactive_console.console.codegen.IScriptConsoleCodeGenerator;

public class PyConsoleCodeGeneratorVariable implements IScriptConsoleCodeGenerator {

    private final PyVariable variable;

    public PyConsoleCodeGeneratorVariable(PyVariable variable) {
        this.variable = variable;
    }

    @Override
    public String getPyCode() {
        return variable.name;

        /*
         * For now this class is only useful as a demonstration of the 
         * IScriptConsoleCodeGenerator and related classes. Dragging anything
         * other than a toplevel node out of the Variables view does not produce
         * anything interesting.
         * 
         * TODO? Write code that can determine a full access to this variable
         * name e.g. if a list called name is in the variables view and the
         * index 0 is dragged into the console, the ideal thing to return here
         * is "name[0]". At the moment I don't think enough information is
         * returned from pydevd_vars to determine the difference between types
         * of objects. Doing this is possible for simple cases, but for all
         * cases it may be difficult or impossible. Consider, for example, dict
         * entries which need resolving by id(). Perhaps an access to a pydev
         * function, so if you drop a dict key you get a line like this:
         * pydevd_resolver.DictResolver().resolve(dict_name, 'a (12345678)')
         * Of course for a complex hierarchy of objects the line would be
         * quite unreadable. 
         */
    }

    @Override
    public boolean hasPyCode() {
        return variable != null && variable.name != null && variable.name.length() > 0;
    }
}
