/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.concurrency;

/*
  File: Semaphore.java

  Originally written by Doug Lea and released into the public domain.
  This may be used for any purposes whatsoever without acknowledgment.
  Thanks for the assistance and support of Sun Microsystems Labs,
  and everyone contributing, testing, and using this code.

  History:
  Date       Who                What
  11Jun1998  dl               Create public version
   5Aug1998  dl               replaced int counters with longs
  24Aug1999  dl               release(n): screen arguments
*/

/**
 * Base class for counting semaphores.
 * Conceptually, a semaphore maintains a set of permits.
 * Each acquire() blocks if necessary
 * until a permit is available, and then takes it. 
 * Each release adds a permit. However, no actual permit objects
 * are used; the Semaphore just keeps a count of the number
 * available and acts accordingly.
 * <p>
 * A semaphore initialized to 1 can serve as a mutual exclusion
 * lock. 
 * <p>
 * Different implementation subclasses may provide different
 * ordering guarantees (or lack thereof) surrounding which
 * threads will be resumed upon a signal.
 * <p>
 * The default implementation makes NO 
 * guarantees about the order in which threads will 
 * acquire permits. It is often faster than other implementations.
 * <p>
 * <b>Sample usage.</b> Here is a class that uses a semaphore to
 * help manage access to a pool of items.
 * <pre>
 * class Pool {
 *   static final MAX_AVAILABLE = 100;
 *   private final Semaphore available = new Semaphore(MAX_AVAILABLE);
 *   
 *   public Object getItem() throws InterruptedException { // no synch
 *     available.acquire();
 *     return getNextAvailableItem();
 *   }
 *
 *   public void putItem(Object x) { // no synch
 *     if (markAsUnused(x))
 *       available.release();
 *   }
 *
 *   // Not a particularly efficient data structure; just for demo
 *
 *   protected Object[] items = ... whatever kinds of items being managed
 *   protected boolean[] used = new boolean[MAX_AVAILABLE];
 *
 *   protected synchronized Object getNextAvailableItem() { 
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (!used[i]) {
 *          used[i] = true;
 *          return items[i];
 *       }
 *     }
 *     return null; // not reached 
 *   }
 *
 *   protected synchronized boolean markAsUnused(Object item) { 
 *     for (int i = 0; i < MAX_AVAILABLE; ++i) {
 *       if (item == items[i]) {
 *          if (used[i]) {
 *            used[i] = false;
 *            return true;
 *          }
 *          else
 *            return false;
 *       }
 *     }
 *     return false;
 *   }
 *
 * }
 *</pre>
 * <p>[<a href="http://gee.cs.oswego.edu/dl/classes/EDU/oswego/cs/dl/util/concurrent/intro.html"> Introduction to this package. </a>]
**/

public class Semaphore {
    /** current number of available permits **/
    protected long permits_;

    /** 
     * Create a Semaphore with the given initial number of permits.
     * Using a seed of one makes the semaphore act as a mutual exclusion lock.
     * Negative seeds are also allowed, in which case no acquires will proceed
     * until the number of releases has pushed the number of permits past 0.
    **/
    public Semaphore(long initialPermits) {
        permits_ = initialPermits;
    }

    /** Wait until a permit is available, and take one **/
    public void acquire() {
        synchronized (this) {
            while (permits_ <= 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
            --permits_;
        }
    }

    //    This is the original code -- it was changed for a version that will ignore InterruptedExceptions.
    //
    //    /** Wait until a permit is available, and take one **/
    //    public void acquire() throws InterruptedException {
    //        if(Thread.interrupted())
    //            throw new InterruptedException();
    //        synchronized(this){
    //            try{
    //                while(permits_ <= 0)
    //                    wait();
    //                --permits_;
    //            }catch(InterruptedException ex){
    //                notify();
    //                throw ex;
    //            }
    //        }
    //    }

    /** Release a permit **/
    public synchronized void release() {
        ++permits_;
        notify();
    }

    /**
     * Return the current number of available permits.
     * Returns an accurate, but possibly unstable value,
     * that may change immediately after returning.
     **/
    public synchronized long permits() {
        return permits_;
    }

}
