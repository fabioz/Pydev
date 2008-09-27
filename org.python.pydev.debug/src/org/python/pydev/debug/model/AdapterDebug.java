package org.python.pydev.debug.model;

public class AdapterDebug {

    public static void print(Object askedfor, Class adapter) {
        if(false){
            System.out.println(askedfor.getClass().getName() + " requests "+adapter.toString());
        }
    }

    public static void printDontKnow(Object askedfor, Class adapter) {
        if(false){
            System.out.println("DONT KNOW: "+askedfor.getClass().getName() + " requests "+adapter.toString());
        }
    }

    
}
