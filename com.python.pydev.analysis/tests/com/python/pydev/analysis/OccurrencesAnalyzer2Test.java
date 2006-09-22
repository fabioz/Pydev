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
            analyzer2.testErrorNotShownOnClassFromMethod();
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
