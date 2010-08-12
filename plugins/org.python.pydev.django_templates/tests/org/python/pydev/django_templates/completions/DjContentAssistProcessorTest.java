package org.python.pydev.django_templates.completions;

import org.eclipse.jface.text.Document;
import org.python.pydev.django_templates.IDjConstants;

import junit.framework.TestCase;

public class DjContentAssistProcessorTest extends TestCase {

    public void testExtractPrefix() {
        DjContentAssistProcessor djContentAssistProcessor = new DjContentAssistProcessor(
                IDjConstants.CONTENT_TYPE_DJANGO_HTML, null);
        Document doc = new Document();
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 1), "");
        
        doc = new Document("" +
        		"test" +
        		"");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 1), "t");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 2), "te");
        assertEquals(djContentAssistProcessor.extractPrefix(doc, 5), "");
        
    }

}
