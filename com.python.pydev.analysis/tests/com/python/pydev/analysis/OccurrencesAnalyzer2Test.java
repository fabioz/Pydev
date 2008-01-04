/*
 * Created on Sep 21, 2006
 * @author Fabio
 */
package com.python.pydev.analysis;

import org.eclipse.jface.text.Document;

import com.python.pydev.analysis.messages.IMessage;

public class OccurrencesAnalyzer2Test extends AnalysisTestsBase {
    
    public static void main(String[] args) {
        try {
            OccurrencesAnalyzer2Test analyzer2 = new OccurrencesAnalyzer2Test();
            analyzer2.setUp();
            analyzer2.testErrorNotShownOnDynamicClass5();
            analyzer2.tearDown();
            System.out.println("finished");
            
            junit.textui.TestRunner.run(OccurrencesAnalyzer2Test.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    public void testErrorNotShownOnDynamicClass() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.getWithAttr.whatever\n"
        );
        checkNoError();

    }
    
    public void testErrorNotShownOnDynamicClass2() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.getWithAttr.whatever.other\n"
        );
        checkNoError();
        
    }
    
    public void testErrorNotShownOnDynamicClass3() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.childGetWithAttr.whatever\n"+
                "print importer.childGetWithAttr.whatever.other\n"
        );
        checkNoError();
        
    }
    
    
    public void testErrorNotShownOnClassFromMethod() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.logger.debug('10')\n"
        );
        checkNoError();
        
    }
    
    public void testErrorNotShownOnNoneClass() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.initialNone.foo\n"
        );
        checkNoError();
        
    }
    
    public void testErrorNotShownOnDynamicClass4() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.globals_struct.bar\n"
        );
        checkNoError();
        
    }
    
    public void testErrorNotShownOnDynamicClass5() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.Struct.bar\n"
        );
        checkNoError();
        
    }
    
    public void testErrorShownOnInitialSetClass() {
        doc = new Document(
                "from extendable.noerr import importer\n"+
                "print importer.initialSet.m1\n"+
                "print importer.initialSet.m2\n"//has error
        );
        IMessage[] messages = checkError(1);
        assertEquals("Undefined variable from import: m2", messages[0].getMessage());
        
    }


}
