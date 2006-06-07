package org.python.pydev.core.structure;

import java.util.EmptyStackException;
import java.util.LinkedList;
import java.util.NoSuchElementException;

public class FastStack<E> extends LinkedList<E> {
	/**
	 * Creates an empty Stack.
	 */
	public FastStack() {
	}

	/**
	 * Pushes an item onto the top of this stack. This has exactly the same effect as: <blockquote>
	 * 
	 * <pre>
	 * addLast(item)</pre>
	 * 
	 * </blockquote>
	 * 
	 * @param item the item to be pushed onto this stack.
	 * @return the <code>item</code> argument.
	 */
	public E push(E item) {
		addLast(item);
		return item;
	}

	/**
	 * Removes the object at the top of this stack and returns that object as the value of this function.
	 * 
	 * @return The object at the top of this stack (the last item of the <tt>LinkedList</tt> object).
	 * @exception EmptyStackException if this stack is empty.
	 */
	public synchronized E pop() {
		return removeLast();
	}

	/**
	 * Looks at the object at the top of this stack without removing it from the stack.
	 * 
	 * @return the object at the top of this stack.
	 * @exception EmptyStackException if this stack is empty.
	 */
	public synchronized E peek() {
		try {
			return getLast();
		} catch (NoSuchElementException e) {
			throw new EmptyStackException();
		}
	}

	/**
	 * Tests if this stack is empty.
	 * 
	 * @return <code>true</code> if and only if this stack contains no items; <code>false</code> otherwise.
	 */
	public boolean empty() {
		return size() == 0;
	}

	/**
	 * Returns the 1-based position where an object is on this stack. If the object <tt>o</tt> occurs as an item in this stack, this method returns the
	 * distance from the top of the stack of the occurrence nearest the top of the stack; the topmost item on the stack is considered to be at distance
	 * <tt>1</tt>. The <tt>equals</tt> method is used to compare <tt>o</tt> to the items in this stack.
	 * 
	 * @param o the desired object.
	 * @return the 1-based position from the top of the stack where the object is located; the return value <code>-1</code> indicates that the object is not
	 *         on the stack.
	 */
	public synchronized int search(Object o) {
		int i = lastIndexOf(o);

		if (i >= 0) {
			return size() - i;
		}
		return -1;
	}

	public void removeAllElements() {
		this.clear();
	}

	public E elementAt(int i) {
		return this.get(i);
	}

}
