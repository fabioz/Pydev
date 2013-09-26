/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.editor.codecompletion.revisited;

import org.eclipse.core.resources.IProject;
import org.python.pydev.shared_core.structure.CollectionFactory;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class ProjectModulesManagerTest extends TestCase {

    public void testProjectDeps() throws Exception {
        ProjectStub p1 = new ProjectStub("p1", "p1", new IProject[0], new IProject[0]);
        ProjectStub p2 = new ProjectStub("p2", "p2", new IProject[0], new IProject[0]);
        ProjectStub p3 = new ProjectStub("p3", "p3", new IProject[0], new IProject[0]);
        ProjectStub p4 = new ProjectStub("p4", "p4", new IProject[0], new IProject[0]);

        p1.setReferencedProjects(p2);
        p2.setReferencedProjects(p3, p4);
        p3.setReferencedProjects(p4);

        assertEquals(CollectionFactory.createHashSet(p2, p3, p4), ProjectModulesManager.getReferencedProjects(p1));
        assertEquals(CollectionFactory.createHashSet(p3, p4), ProjectModulesManager.getReferencedProjects(p2));
        assertEquals(CollectionFactory.createHashSet(p4), ProjectModulesManager.getReferencedProjects(p3));
        assertEquals(CollectionFactory.createHashSet(), ProjectModulesManager.getReferencedProjects(p4));

        p4.setReferencingProjects(p2, p3);
        p3.setReferencingProjects(p2);
        p2.setReferencingProjects(p1);
        p1.setReferencingProjects(p2); //create a cycle here!!

        assertEquals(CollectionFactory.createHashSet(p2), ProjectModulesManager.getReferencingProjects(p1));
        assertEquals(CollectionFactory.createHashSet(p1), ProjectModulesManager.getReferencingProjects(p2));

    }
}
