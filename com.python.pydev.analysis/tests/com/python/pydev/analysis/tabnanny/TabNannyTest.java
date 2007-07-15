package com.python.pydev.analysis.tabnanny;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.pydev.editor.TestIndentPrefs;

import com.python.pydev.analysis.AnalysisPreferencesStub;
import com.python.pydev.analysis.messages.IMessage;

public class TabNannyTest extends TestCase {

    public static void main(String[] args) {
        try {
            TabNannyTest analyzer2 = new TabNannyTest();
            analyzer2.setUp();
            analyzer2.tearDown();
            System.out.println("finished");
            
            junit.textui.TestRunner.run(TabNannyTest.class);
            System.out.println("finished all");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        System.exit(0);
    }

    private AnalysisPreferencesStub prefs;
    
    @Override
    protected void setUp() throws Exception {
        this.prefs = new AnalysisPreferencesStub();
    }

    public void testIterator() throws Exception {
        Document doc = new Document("" +
        		"aaa\n" +
        		"\t\n" +
        		"ccc\n" +
        		""
                );
        
        TabNanny tabNanny = new TabNanny(doc, this.prefs, "", new TestIndentPrefs(true, 4));
        List<IMessage> messages = tabNanny.analyzeDoc();
        assertEquals(1, messages.size());
    }
    
}
