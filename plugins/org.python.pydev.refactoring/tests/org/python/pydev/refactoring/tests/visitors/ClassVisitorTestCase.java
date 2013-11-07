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
import org.python.pydev.refactoring.ast.adapters.IClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.ClassDefVisitor;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

/**
 * @author Ueli Kistler
 */
public class ClassVisitorTestCase extends AbstractIOTestCase {

    public ClassVisitorTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        StringBuffer buffer = new StringBuffer();
        ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(data.source),
                new PythonNatureStub(), createVersionProvider());
        ClassDefVisitor visitor = VisitorFactory.createContextVisitor(ClassDefVisitor.class, module.getASTNode(),
                module, module);
        Iterator<IClassDefAdapter> iter = visitor.iterator();

        buffer.append("# " + visitor.getAll().size() + "\n");
        while (iter.hasNext()) {
            IClassDefAdapter adapter = iter.next();
            buffer.append("# " + adapter.getName() + " " + adapter.isNested() + "\n");
        }
        this.setTestGenerated(buffer.toString().trim());

        assertEquals(3, visitor.getAll().size());
        assertEquals(getExpected(), getGenerated());
    }
}
