/*
 * Created on 31/07/2005
 */
package com.python.pydev.analysis.visitors;

import java.util.Iterator;
import java.util.Stack;

/**
 * used to pass a stack starting at the top (the stack iterator works as the list iterator, and that is
 * not good always good for iterating through a stack)
 *  
 * @author Fabio
 */
class TopDownIterator<E> implements Iterator<E>{

    private Stack<E> scope;
    private int curr;
    public TopDownIterator(Stack<E> scope) {
        this.scope = scope;
        this.curr = scope.size();
    }

    public boolean hasNext() {
        return curr > 0;
    }

    public E next() {
        curr--;
        return scope.get(curr);
    }

    public void remove() {
        throw new RuntimeException("not supported");
    }
    
}

public class TopDownStackIteratable<E> implements Iterable<E>{

    private Stack<E> scope;

    public TopDownStackIteratable(Stack<E> scope) {
        this.scope = scope;
    }

    public Iterator<E> iterator() {
        return new TopDownIterator<E>(scope);
    }
    
    

}
