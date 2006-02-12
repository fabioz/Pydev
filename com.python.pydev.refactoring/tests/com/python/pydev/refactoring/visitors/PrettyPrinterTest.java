/*
 * Created on Feb 11, 2006
 */
package com.python.pydev.refactoring.visitors;

import java.io.BufferedWriter;
import java.io.StringWriter;

import org.python.parser.SimpleNode;
import org.python.parser.ast.Module;
import org.python.pydev.parser.PyParserTestBase;

public class PrettyPrinterTest  extends PyParserTestBase{

    private static final boolean DEBUG = true;

    public static void main(String[] args) {
        try {
            PrettyPrinterTest test = new PrettyPrinterTest();
            test.setUp();
            test.testComments1();
            test.tearDown();
            System.out.println("Finished");
            junit.textui.TestRunner.run(PrettyPrinterTest.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void testNoComments() throws Exception {
        String s = ""+
        "class Class1:\n" +
        "    def met1(self,a):\n" +
        "        pass\n";
        SimpleNode node = parseLegalDocStr(s);
        Module m = (Module) node;
        
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterPrefs("\n"), bufferedWriter);
        m.accept(printer);
        bufferedWriter.flush();
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString()+"'");
        }
        assertEquals(s, stringWriter.getBuffer().toString());
    }
    
    public void testComments1() throws Exception {
        String s = "#comment00\n" +
        "class Class1:#comment0\n" +
        "    #comment1\n" +
        "    def met1(self,a):#comment2\n" +
        "        pass#comment3\n" +
        "\n";
        SimpleNode node = parseLegalDocStr(s);
        Module m = (Module) node;

        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterPrefs("\n"), bufferedWriter);
        m.accept(printer);
        bufferedWriter.flush();
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString()+"'");
        }
        assertEquals(s, stringWriter.getBuffer().toString());
    }
    
    public void testComments2() throws Exception {
        String s = ""+
        "class Foo(object):#test comment\n" +
        "\n" +
        "    def m1(self,a,#c1\n" +
        "        b):#c2\n" +
        "        pass\n";
        SimpleNode node = parseLegalDocStr(s);
        Module m = (Module) node;
        
        StringWriter stringWriter = new StringWriter();
        BufferedWriter bufferedWriter = new BufferedWriter(stringWriter);
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterPrefs("\n"), bufferedWriter);
        m.accept(printer);
        bufferedWriter.flush();
        if(DEBUG){
            System.out.println("\n\nResult:\n");
            System.out.println("'"+stringWriter.getBuffer().toString()+"'");
        }
        assertEquals(s, stringWriter.getBuffer().toString());
        
    }


}
