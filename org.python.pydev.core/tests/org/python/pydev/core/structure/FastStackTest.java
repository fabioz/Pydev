package org.python.pydev.core.structure;

import java.util.Iterator;

import junit.framework.TestCase;

public class FastStackTest extends TestCase {

    public void testStack() throws Exception {
        FastStack<Integer> stack = new FastStack<Integer>(15);
        stack.push(1);
        assertEquals(1, stack.size());
        stack.pop();
        assertEquals(0, stack.size());
        
        for (int i = 0; i < 50; i++) {
            stack.push(i);
        }
        assertEquals(49, (int)stack.peek());

        Iterator<Integer> it = stack.iterator();
        for (int i = 0; i < 50; i++) {
            assertEquals(i, (int)it.next());
        }
        
        it = stack.topDownIterator();
        for (int i = 49; i >= 0; i--) {
            assertEquals(i, (int)it.next());
        }
        
        assertTrue(!stack.empty());
        
        for (int i = 49; i >= 0; i--) {
            assertEquals(i, (int)stack.pop());
        }
        
        assertTrue(stack.empty());
        
        FastStack<Integer> stack2 = new FastStack<Integer>();
        stack2.push(1);
        stack2.push(2);
        
        FastStack<Integer> stack3 = new FastStack<Integer>();
        stack3.push(3);
        stack3.push(4);
        
        stack.addAll(stack2);
        assertEquals(2, stack.size());
        stack.addAll(stack3);
        assertEquals(4, stack.size());
        
        it = stack.iterator();
        for (int i = 0; i < 4; i++) {
            assertEquals(i+1, (int)it.next());
        }
        
        stack = stack.createCopy();
        it = stack.iterator();
        for (int i = 0; i < 4; i++) {
            assertEquals(i+1, (int)it.next());
        }
        assertEquals(4, stack.size());
        
        

    }
}
