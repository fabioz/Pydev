/*
 * Created on Feb 27, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.File;

import org.python.pydev.core.REF;
import org.python.pydev.core.TestDependent;
import org.python.pydev.parser.PyParserTestBase;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.visitors.comparator.DifferException;
import org.python.pydev.parser.visitors.comparator.SimpleNodeComparator;

public class PrettyPrinterLibTest extends PyParserTestBase{


    private static boolean MAKE_COMPLETE_PARSE = true;


    public static void main(String[] args) {
        try {
            junit.textui.TestRunner.run(PrettyPrinterLibTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private PrettyPrinterPrefs prefs;
    

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        prefs = new PrettyPrinterPrefs("\n");
    }

    public void testOnCompleteLib() throws Exception {
        File file = new File(TestDependent.PYTHON_LIB);
        if(MAKE_COMPLETE_PARSE){
            parseFilesInDir(file);
        }else{
            System.out.println("COMPLETE LIB NOT PARSED!");
        }
    }
    
    /**
     * @param file
     * @throws Exception 
     */
    private void parseFilesInDir(File file) throws Exception {
        assertTrue(file.exists());
        assertTrue(file.isDirectory());
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if(f.getAbsolutePath().toLowerCase().endsWith(".py")){
                SimpleNode original = parseLegalDocStr(REF.getFileContents(f), f);
                WriterEraser writer = PrettyPrinterTest.makePrint(prefs, original);
                SimpleNode node = null;
                try {
                    node = parseLegalDocStr(writer.getBuffer().toString());
                    System.out.println("succeded:"+f);
                } catch (Throwable e) {
                    e.printStackTrace();
                    fail("Error, unable to pretty-print and regenerate file:"+f);
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

}
