/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.structure;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.python.pydev.shared_core.structure.FastStack;

import junit.framework.TestCase;

public class FastStackTest extends TestCase {

    public void testStackIteratorRemove() throws Exception {
        FastStack<Integer> stack = new FastStack<Integer>(15);
        stack.push(10);
        stack.push(20);
        stack.push(30);
        Iterator<Integer> it = stack.topDownIterator();
        List<Integer> found = new ArrayList<Integer>();

        //Should be able to pop while in this iterator!
        while (it.hasNext()) {
            Integer next = it.next();
            found.add(next);
            stack.pop();
        }
        List<Integer> expected = new ArrayList<Integer>();
        expected.add(30);
        expected.add(20);
        expected.add(10);
        assertEquals(expected, found);

        assertEquals(stack.size(), 0);

    }

    public void testStack() throws Exception {
        FastStack<Integer> stack = new FastStack<Integer>(15);
        stack.push(1);
        assertEquals(1, stack.size());
        stack.pop();
        assertEquals(0, stack.size());

        for (int i = 0; i < 50; i++) {
            stack.push(i);
        }
        assertEquals(49, (int) stack.peek());

        Iterator<Integer> it = stack.iterator();
        for (int i = 0; i < 50; i++) {
            assertEquals(i, (int) it.next());
        }

        it = stack.topDownIterator();
        for (int i = 49; i >= 0; i--) {
            assertEquals(i, (int) it.next());
        }

        assertTrue(!stack.empty());

        for (int i = 49; i >= 0; i--) {
            assertEquals(i, (int) stack.pop());
        }

        assertTrue(stack.empty());

        FastStack<Integer> stack2 = new FastStack<Integer>(5);
        stack2.push(1);
        stack2.push(2);

        FastStack<Integer> stack3 = new FastStack<Integer>(1);
        stack3.push(3);
        stack3.push(4);

        stack.addAll(stack2);
        assertEquals(2, stack.size());
        stack.addAll(stack3);
        assertEquals(4, stack.size());

        it = stack.iterator();
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, (int) it.next());
        }

        stack = stack.createCopy();
        it = stack.iterator();
        for (int i = 0; i < 4; i++) {
            assertEquals(i + 1, (int) it.next());
        }
        assertEquals(4, stack.size());

    }
}
