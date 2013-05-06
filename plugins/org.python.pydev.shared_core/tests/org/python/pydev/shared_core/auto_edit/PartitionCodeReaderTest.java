package org.python.pydev.shared_core.auto_edit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.python.pydev.shared_core.partitioner.PartitionCodeReader;
import org.python.pydev.shared_core.testutils.TestUtils;

public class PartitionCodeReaderTest extends TestCase {

    public void testPartitionCodeReader() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader(IDocument.DEFAULT_CONTENT_TYPE);
        Document document = new Document("aaaa bbbb");

        document.addPositionCategory(IDocument.DEFAULT_CONTENT_TYPE);
        document.addPositionCategory("cat1");

        document.addPosition("cat1", new TypedPosition(0, 4, "cat1"));
        document.addPosition(IDocument.DEFAULT_CONTENT_TYPE, new TypedPosition(4, 5, IDocument.DEFAULT_CONTENT_TYPE));

        reader.configureForwardReader(document, 0, document.getLength() - 1);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(" ", "b", "b", "b"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, document.getLength());
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("b", "b", "b", "b", " "), TestUtils.listToExpected(found));
    }
}
