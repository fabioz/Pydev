package com.python.pydev.analysis;

/**
 * Tests tokens gotten from imports to see if they really exist there
 */
public class ImportsOcurrencesAnalyzerTest extends AnalysisTestsBase { 

    public static void main(String[] args) {
        try {
            OcurrencesAnalyzerTest analyzer2 = new OcurrencesAnalyzerTest();
            analyzer2.setUp();
            analyzer2.tearDown();
            System.out.println("finished");
            
            
            junit.textui.TestRunner.run(OcurrencesAnalyzerTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }


}
