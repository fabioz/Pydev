/**
 * Copyright (c) 2005-2012 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core;

import junit.framework.TestCase;

/**
 * @author fabioz
 *
 * Outer lock not synched internal
 * 6.837
 * 
 * With outer lock and synched internal
 * 7.2
 * 
 * Only synched internal
 * 7.5
 */
public class ObjectsPoolTest extends TestCase {

    public void testObjectsPool() throws Exception {
        String intern = ObjectsInternPool.intern(new String("foo"));
        assertSame(ObjectsInternPool.intern(new String("foo")), intern);

        //        Timer t = new Timer();
        //        synchronized (ObjectsPool.lock){
        //            for(int i=0;i<100000000;i++){
        //                ObjectsPool.internUnsynched("foo");
        //            }
        //        }
        //        t.printDiff();
    }
}
