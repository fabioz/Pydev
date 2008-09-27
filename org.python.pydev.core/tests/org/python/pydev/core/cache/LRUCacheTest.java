package org.python.pydev.core.cache;

import junit.framework.TestCase;

public class LRUCacheTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(LRUCacheTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void test() throws Exception {
        
    }
    public void testRegular() throws Exception {
        LRUCache<Integer, Integer> cache = new LRUCache<Integer, Integer>(2);
        cache.add(1,1);
        cache.add(2,2);
        cache.add(3,3);
        assertNull(cache.getObj(1));
        
        cache.add(4,4);
        assertNull(cache.getObj(2));
        
        //there is only 3 and 4 now
        cache.startGrowAsNeeded(Integer.MAX_VALUE);
        cache.add(5,5);
        cache.add(6,6);
        assertNotNull(cache.getObj(3));
        assertNotNull(cache.getObj(4));
        
        cache.stopGrowAsNeeded();
        assertEquals(2, cache.cache.size());
        cache.startGrowAsNeeded(10);
        try {
            cache.startGrowAsNeeded(10);
            fail("We cannot make one start on the top of the other... when using this, it should be synched to avoid it.");
        } catch (Exception e) {
            //ok, expected
        }
        cache.stopGrowAsNeeded();
    }
}
