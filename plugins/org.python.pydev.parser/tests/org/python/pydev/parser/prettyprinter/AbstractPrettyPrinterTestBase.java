package org.python.pydev.parser.prettyprinter;

import java.io.File;
import java.io.IOException;

import org.python.pydev.core.REF;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.visitors.comparator.DifferException;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator;

public class AbstractPrettyPrinterTestBase extends PyParserTestBase{
    
    public static boolean DEBUG = false;

    protected PrettyPrinterPrefs prefs;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefs("\n");
    }

    public SimpleNode checkPrettyPrintEqual(String s, String expected) throws Error {
        return checkPrettyPrintEqual(s, prefs, expected);
        
    }
    public SimpleNode checkPrettyPrintEqual(String s) throws Error {
        return checkPrettyPrintEqual(s, s);
    }
    
    /**
     * @param s
     * @return 
     * @throws Exception
     * @throws IOException
     */
    public static SimpleNode checkPrettyPrintEqual(String s, PrettyPrinterPrefs prefs, String expected) throws Error {
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
    public static WriterEraser makePrint(PrettyPrinterPrefs prefs, SimpleNode node) throws Error {
        Module m = (Module) node;
        
        final WriterEraser stringWriter = new WriterEraser();
        PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
        try {
            m.accept(printer);
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString().replace(' ', '.').replace('\t', '^')+"'");
        }
        assertTrue(! printer.state.inStmt());
//        assertTrue("Should not be in record:"+printer.auxComment, ! printer.auxComment.inRecord());
        return stringWriter;
    }

    

    /**
     * @param file
     * @throws Exception 
     */
    protected void parseAndReparsePrettyPrintedFilesInDir(File file) throws Exception {
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            parseAndPrettyPrintFile(f);
        }
    }

    protected void parseAndPrettyPrintFile(File f) throws Error, Exception {
        if(f.getAbsolutePath().toLowerCase().endsWith(".py")){
            SimpleNode original = parseLegalDocStr(REF.getFileContents(f), f);
            if(original == null){
                fail("Error\nUnable to generate the AST for the file:"+f);
            }
            WriterEraser writer = PrettyPrinterTest.makePrint(prefs, original);
            SimpleNode node = null;
            try {
                node = parseLegalDocStr(writer.getBuffer().toString());
            } catch (Throwable e) {
                System.out.println("\n\n\n----------------- Initial contents:-------------------------\n");
                System.out.println(original);
                System.out.println("\n\n--------------Pretty-printed contents:------------------\n");
                System.out.println(writer.getBuffer().toString());
                System.out.println("\n\n\n");
                System.out.println("File: "+f);
                e.printStackTrace();
                
                fail("Error\nUnable to pretty-print regenerated file:"+f);
            }
            SimpleNodeComparator comparator = new SimpleNodeComparator();
            try {
                comparator.compare(original, node);
            } catch (DifferException e) {
                System.out.println("Compare did not suceed:"+f);
            }
        }
    }

}
