/*
 * Created on 28/07/2005
 */
package org.python.pydev.core;

import java.util.Iterator;

/**
 * iterates through a string so that parts of it are gotten each time in a progressive way based on dots
 * 
 * (e.g.: a.b.c
 * 
 * will return
 * 
 * a
 * a.b
 * a.b.c
 * 
 * @author Fabio
 */
public class FullRepIterable implements Iterable<String>{

    private static final class FullRepIterator implements Iterator<String> {
        private int i = -1;
        private boolean lastStep; //even if there is no point, we should return the last string
        private String fullRep;
        
        public FullRepIterator(String fullRep) {
            this.fullRep = fullRep;
            lastStep = false;
        }

        public boolean hasNext() {
            boolean ret = !lastStep || fullRep.indexOf('.', i) != -1;
            return ret;
        }

        public String next() {
            int j = fullRep.indexOf('.', i);
            if(j == -1){
                lastStep = true;
                return fullRep;
            }
            i = j+1;
            return fullRep.substring(0,j);
        }

        public void remove() {
            throw new RuntimeException("not supported");
        }
    }

    
    
    private String fullRep;

    public FullRepIterable(String fullRep) {
        this.fullRep = fullRep;
    }

    public Iterator<String> iterator() {
        return new FullRepIterator(this.fullRep);
    }

}
