/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.builder.todo;

import java.util.Arrays;

import org.eclipse.jface.text.Document;

import junit.framework.TestCase;

/**
 * @author Fabio
 *
 */
public class PyTodoVisitorTest extends TestCase {

    public void testTodoMatching() throws Exception {
        Document document = new Document("'TODO'");
        PyTodoVisitor todoVisitor = new PyTodoVisitor();
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("TODO");
        assertEquals(0, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("#TODO");
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("'TODO");
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("'''TODO'''&'TODO'");
        assertEquals(2, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("#TODO TODO");
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("#TODOTODO");
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("#TODO\n#TODO");
        assertEquals(2, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());

        document = new Document("#TODO\nTODO");
        assertEquals(1, todoVisitor.computeTodoMarkers(document, Arrays.asList("TODO")).size());
    }
}
