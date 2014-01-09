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
import org.python.pydev.refactoring.ast.adapters.ClassDefAdapter;
import org.python.pydev.refactoring.ast.adapters.ModuleAdapter;
import org.python.pydev.refactoring.ast.adapters.SimpleAdapter;
import org.python.pydev.refactoring.ast.visitors.VisitorFactory;
import org.python.pydev.refactoring.ast.visitors.context.ClassDefVisitor;
import org.python.pydev.refactoring.ast.visitors.context.GlobalAttributeVisitor;
import org.python.pydev.refactoring.ast.visitors.context.LocalAttributeVisitor;
import org.python.pydev.refactoring.tests.adapter.PythonNatureStub;
import org.python.pydev.refactoring.tests.core.AbstractIOTestCase;

public class AttributeVisitorTestCase extends AbstractIOTestCase {

    public AttributeVisitorTestCase(String name) {
        super(name);
    }

    @Override
    public void runTest() throws Throwable {
        StringBuffer buffer = new StringBuffer();
        ModuleAdapter module = VisitorFactory.createModuleAdapter(null, null, new Document(data.source),
                new PythonNatureStub(), createVersionProvider());
        GlobalAttributeVisitor globalVisitor = VisitorFactory.createContextVisitor(GlobalAttributeVisitor.class,
                module.getASTNode(), module, module);
        ClassDefVisitor classVisitor = VisitorFactory.createContextVisitor(ClassDefVisitor.class, module.getASTNode(),
                module, module);
        assertTrue(classVisitor.getAll().size() > 0);

        ClassDefAdapter classDefAdapter = (ClassDefAdapter) classVisitor.getAll().get(0);
        LocalAttributeVisitor localVisitor = VisitorFactory.createContextVisitor(LocalAttributeVisitor.class,
                classDefAdapter.getASTNode(), module, classDefAdapter);
        printAttributes(buffer, globalVisitor);
        printAttributes(buffer, localVisitor);

        assertEquals(getExpected(), getGenerated());
    }

    private void printAttributes(StringBuffer buffer, GlobalAttributeVisitor globalVisitor) {
        Iterator<SimpleAdapter> iter = globalVisitor.iterator();
        buffer.append("# " + globalVisitor.getAll().size() + "\n");
        while (iter.hasNext()) {
            SimpleAdapter adapter = iter.next();
            buffer.append("# " + adapter.getParentName() + " " + adapter.getName() + "\n");
        }
        this.setTestGenerated(buffer.toString().trim());
    }
}
