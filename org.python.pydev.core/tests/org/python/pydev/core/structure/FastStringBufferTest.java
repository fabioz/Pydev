package org.python.pydev.core.structure;

import junit.framework.TestCase;

public class FastStringBufferTest extends TestCase{

    private static final int ITERATIONS = 10000;
    private static final int OUTER_ITERATIONS = 50;

    public void testFastString1() throws Exception {
        
        FastStringBuffer fastString = new FastStringBuffer(2);
        fastString.append("bbb");
        assertEquals("bbb", fastString.toString());
        fastString.append("ccc");
        assertEquals("bbbccc", fastString.toString());
        fastString.clear();
        assertEquals("", fastString.toString());
        fastString.append("abc");
        assertEquals("abc", fastString.toString());
        fastString.reverse();
        assertEquals("cba", fastString.toString());
        
        fastString.clear();
        fastString.append("aaa");
        FastStringBuffer other = new FastStringBuffer(3);
        other.append("bbcccdddddddddddddddddddddddddddddd");
        fastString.append(other);
        assertEquals("aaabbcccdddddddddddddddddddddddddddddd", fastString.toString());
        fastString.insert(1, "22");
        assertEquals("a22aabbcccdddddddddddddddddddddddddddddd", fastString.toString());
        fastString.append('$');
        assertEquals("a22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.insert(1, ".");
        assertEquals("a.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.replace(0,1, "xxx");
        assertEquals("xxx.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        fastString.delete(0,1);
        assertEquals("xx.22aabbcccdddddddddddddddddddddddddddddd$", fastString.toString());
        
        char[] charArray = fastString.toString().toCharArray();
        char[] charArray2 = fastString.toCharArray();
        assertEquals(charArray.length, charArray2.length);
        for (int i = 0; i < charArray2.length; i++) {
            assertEquals(charArray[i], charArray2[i]);
            
        }
    }
    
    
//    public void testFastString() throws Exception {
//        
//        long total=0;
//        FastStringBuffer fastString = new FastStringBuffer(50);
//        for(int j=0;j<OUTER_ITERATIONS;j++){
//            final long start = System.nanoTime();
//            
//            
//            fastString.clear();
//            for(int i=0;i<ITERATIONS;i++){
//                fastString.append("test").append("bar").append("foo").append("foo").append("foo").append("foo");
//            }
//            
//            final long end = System.nanoTime();
//            long delta=(end-start)/1000000;
//            total+=delta;
////            System.out.println("Fast: " + delta);
//        }        
//        System.out.println("Fast Total:"+total);
//    }
//    
//    public void testStringBuffer() throws Exception {
//        
//        long total=0;
//        StringBuffer fastString = new StringBuffer(50);
//        for(int j=0;j<OUTER_ITERATIONS;j++){
//            final long start = System.nanoTime();
//            
//            
//            fastString.setLength(0);
//            for(int i=0;i<ITERATIONS;i++){
//                fastString.append("test").append("bar").append("foo").append("foo").append("foo").append("foo");
//            }
//            
//            final long end = System.nanoTime();
//            long delta=(end-start)/1000000;
//            total+=delta;
////            System.out.println("Buffer: " + delta);
//        }   
//        System.out.println("Buffer Total:"+total);
//    }
        
}
