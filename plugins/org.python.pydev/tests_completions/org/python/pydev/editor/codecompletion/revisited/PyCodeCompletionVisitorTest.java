/******************************************************************************
* Copyright (C) 2012-2013  Fabio Zadrozny
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*     Fabio Zadrozny <fabiofz@gmail.com> - initial API and implementation
******************************************************************************/
package org.python.pydev.editor.codecompletion.revisited;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.python.pydev.builder.VisitorMemo;
import org.python.pydev.parser.PythonNatureStub;
import org.python.pydev.shared_core.callbacks.ICallback0;
import org.python.pydev.shared_core.resource_stubs.FileMock;
import org.python.pydev.shared_core.resource_stubs.FolderMock;
import org.python.pydev.shared_core.resource_stubs.ProjectMock;

public class PyCodeCompletionVisitorTest extends TestCase {

    public static void main(String[] args) {

        try {
            PyCodeCompletionVisitorTest test = new PyCodeCompletionVisitorTest();
            test.setUp();
            test.testVisitor();
            test.tearDown();
            System.out.println("Finished");

            junit.textui.TestRunner.run(PyCodeCompletionVisitorTest.class);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public void testVisitor() {
        final List<IResource> changed = new ArrayList<IResource>();

        PyCodeCompletionVisitor visitor = new PyCodeCompletionVisitor() {
            @Override
            public void visitChangedResource(IResource resource, ICallback0<IDocument> document,
                    IProgressMonitor monitor) {
                super.visitChangedResource(resource, document, monitor);
                changed.add(resource);
            }
        };
        ProjectMock project = new ProjectMock();
        project.setNature(new PythonNatureStub());

        FolderMock mod1 = new FolderMock("mod1");
        FileMock initFromMod1 = new FileMock("__init__.py"); //visited

        mod1.addMember(initFromMod1);
        mod1.addMember(new FileMock("a.py")); //visited
        mod1.addMember(new FileMock("unrelated"));

        FolderMock mod2 = new FolderMock("mod2");
        mod1.addMember(mod2);

        mod2.addMember(new FileMock("__init__.py")); // visited

        FolderMock mod3 = new FolderMock("mod3");
        mod1.addMember(mod3);
        mod3.addMember(new FileMock("c.py")); //not visited because mod3 has no __init__.

        project.addMember(mod1);

        visitor.memo = new VisitorMemo();
        visitor.visitAddedResource(initFromMod1, null, null);

        //See comments above for visited.
        assertNames(changed, "__init__.py", "a.py", "__init__.py");
    }

    private void assertNames(List<IResource> changed, String... expected) {
        List<String> names = new ArrayList<String>();
        for (IResource r : changed) {
            names.add(r.getName());
        }
        assertEquals(names, Arrays.asList(expected));
    }

}
