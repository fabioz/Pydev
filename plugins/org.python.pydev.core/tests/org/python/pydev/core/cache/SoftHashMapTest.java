/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 */
public class SoftHashMapTest extends TestCase {

    public void testSoftHashMap() throws Exception {
        SoftHashMap<Integer, byte[]> softHashMap = new SoftHashMap<Integer, byte[]>();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            softHashMap.put(i, new byte[1024 * 1024]); // 1/2 MB

            int notFound = 0;
            int found = 0;
            for (int j = 0; j < i; j++) {
                Object o = softHashMap.get(j);
                if (o == null) {
                    notFound++;
                } else {
                    found++;
                }
            }
            if (notFound > 0) {
                System.out.println("Not found: " + notFound + " Found: " + found + " Loop: " + i);
            }
            if (notFound > found) {
                assertTrue(i > 1);
                break;
            }
        }
    }

    /**
     * Testing for checking assumptions done about the Queue:
     * 
     * - If the weak reference is collected before the object it references, it won't be added to the queue.
     * - After an object is added to the queue, it'll remain there even if the reference is no longer accessible anywhere else.
     * 
     * 
     * @throws Exception
     */
    public void testQueue() throws Exception {
        //        ReferenceQueue<String> referenceQueue = new ReferenceQueue<String>();
        //        String s = new String("foo");
        //        WeakReference<String> weakReference = new WeakReference<String>(s, referenceQueue);
        //        weakReference = null;
        //        s = null;
        //        System.gc();
        //        synchronized (this) {
        //            this.wait(50);
        //        }
        //        assertTrue(weakReference.get() == null);
        //        
        //        weakReference = null;
        //        System.gc();
        //        synchronized (this) {
        //            this.wait(50);
        //        }
        //        
        //        
        //        System.out.println(referenceQueue.poll());
    }
}
