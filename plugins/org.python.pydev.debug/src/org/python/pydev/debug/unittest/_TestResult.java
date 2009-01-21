package org.python.pydev.debug.unittest;
///*
// * Created on Nov 10, 2004
// *
// * TODO To change the template for this generated file go to
// * Window - Preferences - Java - Code Style - Code Templates
// */
//package org.python.pydev.debug.unittest;
//
///**
// * @author ggheorg
// *
// * TODO To change the template for this generated type comment go to
// * Window - Preferences - Java - Code Style - Code Templates
// */
//public class TestResult {
//    public final static int OK = 0;
//    public final static int FAIL = 1;
//    public final static int ERROR = 2;
//    public String testFile;
//    public String klass;
//    public String method;
//    public int status;
//    public long startTime;
//    public long endTime;
//    
//    public TestResult(String testFile, String klass, String method, int status, long startTime) {
//        this.testFile = testFile;
//        this.klass = klass;
//        this.method = method;
//        this.status = status;
//        this.startTime = startTime;
//    }
//    
//    public void testFailed(String failureType) {
//        //System.out.println("Failure type:" + failureType + ":");
//        if (failureType.equals("FAIL"))
//            status = FAIL;
//        else if (failureType.equals("ERROR"))
//            status = ERROR;
//    }
//
//    public boolean isFailure() {
//        return status != OK;
//    }
//
//    public void testFinished() {
//        endTime = System.currentTimeMillis();
//    }
//    
//    public long testDuration() {
//        return endTime - startTime;
//    }
//}
