package org.python.pydev.parser.prettyprinter;

import java.io.IOException;

import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;

public class AbstractPrettyPrinterTestBase extends PyParserTestBase{
    
    public static final boolean DEBUG = true;

    protected PrettyPrinterPrefs prefs;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefs("\n");
    }

    public SimpleNode checkPrettyPrintEqual(String s, String expected) throws Exception, IOException {
        return checkPrettyPrintEqual(s, prefs, expected);
        
    }
    public SimpleNode checkPrettyPrintEqual(String s) throws Exception, IOException {
        return checkPrettyPrintEqual(s, s);
    }
    
    /**
     * @param s
     * @return 
     * @throws Exception
     * @throws IOException
     */
    public static SimpleNode checkPrettyPrintEqual(String s, PrettyPrinterPrefs prefs, String expected) throws Exception, IOException {
        SimpleNode node = parseLegalDocStr(s);
        final WriterEraser stringWriter = makePrint(prefs, node);

        assertEquals(expected, stringWriter.getBuffer().toString());
        return node;
    }

    /**
     * @param prefs
     * @param node
     * @return
     * @throws Exception
     */
    public static WriterEraser makePrint(PrettyPrinterPrefs prefs, SimpleNode node) throws Exception {
        Module m = (Module) node;
        
        final WriterEraser stringWriter = new WriterEraser();
        PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
        m.accept(printer);
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString()+"'");
        }
        assertTrue(! printer.state.inStmt());
//        assertTrue("Should not be in record:"+printer.auxComment, ! printer.auxComment.inRecord());
        return stringWriter;
    }

}
