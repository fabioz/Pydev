package org.python.pydev.parser.prettyprinter;

import java.io.File;
import java.io.IOException;

import org.python.pydev.core.REF;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.Module;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterPrefsV2;
import org.python.pydev.parser.prettyprinterv2.PrettyPrinterV2;
import org.python.pydev.parser.visitors.comparator.DifferException;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator;

public class AbstractPrettyPrinterTestBase extends PyParserTestBase{
    
    public static boolean DEBUG = false;

    protected IPrettyPrinterPrefs prefs;
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefsV2("\n", "    ");
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
    public static SimpleNode checkPrettyPrintEqual(String s, IPrettyPrinterPrefs prefs, String expected) throws Error {
        SimpleNode node = parseLegalDocStr(s);

        assertEquals(expected, makePrint(prefs, node));
        return node;
    }

    /**
     * @param prefs
     * @param node
     * @return
     * @throws Exception
     */
    public static String makePrint(IPrettyPrinterPrefs prefs, SimpleNode node) throws Error {
        Module m = (Module) node;
        
        PrettyPrinterV2 printer = new PrettyPrinterV2(prefs);
        String result = "";
        try {
            result = printer.print(m);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+result+"'");
//            System.out.println("'"+result.replace(' ', '.').replace('\t', '^')+"'");
        }
        return result;
        
        //OLD VERSION
//        final WriterEraser stringWriter = new WriterEraser();
//        PrettyPrinter printer = new PrettyPrinter(prefs, stringWriter);
//        try {
//            m.accept(printer);
//        } catch (Exception e) {
//            throw new AssertionError(e);
//        }
//        if(DEBUG){
//            System.out.println("\n\nResult:\n");
//            System.out.println("'"+stringWriter.getBuffer().toString().replace(' ', '.').replace('\t', '^')+"'");
//        }
//        assertTrue(! printer.state.inStmt());
//        return stringWriter.getBuffer().toString();
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
            String result = PrettyPrinterTest.makePrint(prefs, original);
            SimpleNode node = null;
            try {
                node = parseLegalDocStr(result);
            } catch (Throwable e) {
                System.out.println("\n\n\n----------------- Initial contents:-------------------------\n");
                System.out.println(original);
                System.out.println("\n\n--------------Pretty-printed contents:------------------\n");
                System.out.println(result);
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
