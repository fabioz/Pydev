/**
 * Copyright (c) 2005-2013 by Appcelerator, Inc. All Rights Reserved.
 * Licensed under the terms of the Eclipse Public License (EPL).
 * Please see the license.txt included with this distribution for details.
 * Any modifications to this file must keep this entire header intact.
 */
package org.python.pydev.core.cache;

import org.python.pydev.shared_core.cache.LRUCache;

import junit.framework.TestCase;

public class LRUCacheTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LRUCacheTest.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test() throws Exception {

    }

    public void testRegular() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<Integer, Integer>(2);
        cache.add(1, 1);
        cache.add(2, 2);
        cache.add(3, 3);
        assertNull(cache.getObj(1));

        cache.add(4, 4);
        assertNull(cache.getObj(2));
    }
}
