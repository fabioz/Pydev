package org.python.pydev.core.performanceeval;

/**
 * This class is a helper in performance evaluation
 */
public class Timer {

    private long start;

    public Timer(){
        this.start = System.currentTimeMillis();
    }

    public void printDiffMillis() {
        System.out.println("Time Elapsed (millis):"+getDiff());
    }
    
    public void printDiff() {
        printDiff(null);
    }

    private long getDiff() {
        long old = this.start;
        long newStart = System.currentTimeMillis();
        long diff = (newStart-old);
        start = newStart;
        return diff;
    }

    public void printDiff(String msg) {
        double secs = getDiff()/1000.0d;
        if(msg != null){
            System.out.println("Time Elapsed for:"+msg+" (secs):"+secs);
        }else{
            System.out.println("Time Elapsed (secs):"+secs);
        }
    }
}
