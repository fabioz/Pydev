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

    private static final class ReverseFullRepIterator implements Iterator<String> {

        private String fullRep;

        public ReverseFullRepIterator(String fullRep) {
            this.fullRep = fullRep;
        }

        public boolean hasNext() {
            return fullRep.length() > 0;
        }

        public String next() {
            if(fullRep.length() == 0){
                throw new RuntimeException("no more items");
            }
            String ret = fullRep;
            int l = fullRep.lastIndexOf('.');
            if(l == -1){
                fullRep = "";
            }else{
                fullRep = fullRep.substring(0, l);
            }
            return ret;
        }

        public void remove() {
            throw new RuntimeException("Not supported");
        }
        
    }
    
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
    private boolean reverse;

    public FullRepIterable(String fullRep) {
        this(fullRep, false);
    }

    /**
     * User is able to specify reverse mode.
     * 
     * If it is the standard mode, 'aa.bb' would be 'aa', 'aa.bb'.
     * 
     * In reverse mode 'aa.bb' would be 'aa.bb', 'aa' 
     * 
     * @param fullRep The dotted string we want to gather in parts
     * @param reverse whether we want to get it in reverse order
     */
    public FullRepIterable(String fullRep, boolean reverse) {
        this.fullRep = fullRep;
        this.reverse = reverse;
    }

    public Iterator<String> iterator() {
        if(!reverse){
            return new FullRepIterator(this.fullRep);
        }else{
            return new ReverseFullRepIterator(this.fullRep);
        }
    }
    
    /**
     * on string 'aa.bb.cc', the head is 'cc' and the tail is 'aa.bb'
     * 
     * head is pos 1 (cc)
     * tail is pos 0 (aa.bb)
     */
    public static String[] headAndTail(String fullRep){
        int i = fullRep.lastIndexOf('.');
        if(i != -1){
            return new String[]{ 
                    fullRep.substring(0, i), 
                    fullRep.substring(i+1)
                    };
        }else{
            return new String[]{ 
                    "",
                    fullRep 
            };
            
        }
    }

}
