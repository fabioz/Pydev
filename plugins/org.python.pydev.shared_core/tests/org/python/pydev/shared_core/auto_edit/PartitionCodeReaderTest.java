package org.python.pydev.shared_core.auto_edit;

import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.TypedPosition;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.rules.IPartitionTokenScanner;
import org.eclipse.jface.text.rules.RuleBasedPartitionScanner;
import org.python.pydev.shared_core.partitioner.PartitionCodeReader;
import org.python.pydev.shared_core.testutils.TestUtils;

public class PartitionCodeReaderTest extends TestCase {

    public void testPartitionCodeReader() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader(IDocument.DEFAULT_CONTENT_TYPE);
        Document document = new Document("aaaa bbbb");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(0, 4, "cat1"));
        document.addPosition(category, new TypedPosition(4, 5, IDocument.DEFAULT_CONTENT_TYPE));

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

    public void testPartitionCodeReader2() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(3, 3, "cat1"));
        document.addPosition(category, new TypedPosition(8, 3, "cat1"));

        reader.configureForwardReader(document, 0, document.getLength() - 1);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("a", " ", "b", "b", " ", "c"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, document.getLength());
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("c", " ", "b", "b", " ", "a"), TestUtils.listToExpected(found));
    }

    public void testPartitionCodeReaderBoundaries() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);

        document.addPosition(category, new TypedPosition(3, 3, "cat1"));
        document.addPosition(category, new TypedPosition(8, 3, "cat1"));

        reader.configureForwardReader(document, 4, 6);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(" ", "b"), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, 5);
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected("b", " ", "a"), TestUtils.listToExpected(found));
    }

    public void testPartitionCodeReaderEmpty() throws Exception {
        PartitionCodeReader reader = new PartitionCodeReader("cat1");
        Document document = new Document("aaaa bbbb ccc");
        String category = setupDocument(document);
        reader.configureForwardReader(document, 4, 6);
        List<Character> found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(), TestUtils.listToExpected(found));

        reader.configureBackwardReader(document, 5);
        found = new ArrayList<Character>();
        while (true) {
            int read = reader.read();
            if (read == PartitionCodeReader.EOF) {
                break;
            }
            found.add((char) read);
        }
        assertEquals(TestUtils.listToExpected(), TestUtils.listToExpected(found));
    }

    public String setupDocument(Document document) {
        IPartitionTokenScanner scanner = new RuleBasedPartitionScanner();
        FastPartitioner partitioner = new FastPartitioner(scanner, new String[] { IDocument.DEFAULT_CONTENT_TYPE });
        String[] managingPositionCategories = partitioner.getManagingPositionCategories();
        String category = managingPositionCategories[0];
        document.setDocumentPartitioner(partitioner);
        document.addPositionCategory(category);
        return category;
    }
}
