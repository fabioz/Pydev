/*
 * Created on Mar 23, 2005
 *
 * @author Fabio Zadrozny
 */
package org.python.pydev.editor.correctionassist;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;

/**
 * @author Fabio Zadrozny
 */
public class PythonCorrectionProcessorTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(PythonCorrectionProcessorTest.class);
    }

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testGetOverrideProps() throws BadLocationException {
        int selLength=0;
        int endLineIndex=2;
        int startLineIndex=2;
        
        IDocument doc = new Document(""+
"class TestModel(unittest.TestCase):"+             
"                                   "+        
"    def set"          //setUp override should be bought. 
);

//not finished
//        PySelection ps = new PySelection(doc, startLineIndex, endLineIndex, selLength, false); 
//        File f = null; 
//        PythonNature nature = null;
//        
//        PythonCorrectionProcessor.getOverrideProps(ps, null, f, nature);
    }

}
