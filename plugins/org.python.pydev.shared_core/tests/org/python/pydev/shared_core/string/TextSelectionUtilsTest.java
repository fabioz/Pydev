package org.python.pydev.shared_core.string;

import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IRegion;

public class TextSelectionUtilsTest extends TestCase {

    public void testSearch() throws Exception {
        Document document = new Document("aaa bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        List<IRegion> occurrences = ts.searchOccurrences("aaa");
        assertEquals(2, occurrences.size());
    }

    public void testSearch2() throws Exception {
        Document document = new Document("aaa bbb aaa");
        TextSelectionUtils ts = new TextSelectionUtils(document, 0);
        List<IRegion> occurrences = ts.searchOccurrences("a");
        assertEquals(0, occurrences.size());
    }
}
