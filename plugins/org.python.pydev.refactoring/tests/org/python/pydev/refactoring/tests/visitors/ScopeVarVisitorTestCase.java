/******************************************************************************
* Copyright (C) 2006-2012  IFS Institute for Software and others
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Original authors:
*     Dennis Hunziker
*     Ueli Kistler
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial implementation
******************************************************************************/
/*
 * Copyright (C) 2006, 2007  Dennis Hunziker, Ueli Kistler
 */

package org.python.pydev.refactoring.tests.visitors;

import java.util.Iterator;

import org.eclipse.jface.text.Document;
import org.python.pydev.ast.adapters.ClassDefAdapter;
import org.python.pydev.ast.adapters.FunctionDefAdapter;
import org.python.pydev.ast.adapters.IClassDefAdapter;
import org.python.pydev.ast.adapters.ModuleAdapter;
import org.python.pydev.ast.adapters.SimpleAdapter;
import org.python.pydev.ast.adapters.context.ScopeVariablesVisitor;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class ScopeVarVisitorTestCase extends AbstractIOTestCase {

    public ScopeVarVisitorTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        StringBuffer buffer = new StringBuffer();
        ModuleAdapter module = org.python.pydev.ast.adapters.visitors.VisitorFactory.createModuleAdapter(null, null,
                new Document(data.source),
                new PythonNatureStub(), createVersionProvider());

        for (FunctionDefAdapter func : module.getFunctions()) {

            ScopeVariablesVisitor visitor = org.python.pydev.ast.adapters.visitors.VisitorFactory.createContextVisitor(
                    ScopeVariablesVisitor.class,
                    func.getASTNode(), module, func);

            printAttributes(buffer, visitor, func.getName());
        }
        for (IClassDefAdapter clazz : module.getClasses()) {
            ClassDefAdapter c = (ClassDefAdapter) clazz;
            ScopeVariablesVisitor visitor = org.python.pydev.ast.adapters.visitors.VisitorFactory.createContextVisitor(
                    ScopeVariablesVisitor.class,
                    c.getASTNode(), module, c);

            printAttributes(buffer, visitor, clazz.getName());
        }

        this.setTestGenerated(buffer.toString().trim());

        assertEquals(getExpected(), getGenerated());
    }

    private void printAttributes(StringBuffer buffer, ScopeVariablesVisitor scopeVisitor, String scopeName) {
        Iterator<SimpleAdapter> iter = scopeVisitor.iterator();
        buffer.append("\n# " + scopeName + " " + scopeVisitor.getAll().size() + "\n");
        while (iter.hasNext()) {
            SimpleAdapter adapter = iter.next();
            buffer.append("## " + adapter.getName() + "\n");
        }
    }
}
