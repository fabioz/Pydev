/*
 * Created on 13/07/2005
 */
package com.python.pydev.fastparser;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Expr;
import org.python.parser.ast.FunctionDef;
import org.python.parser.ast.Module;
import org.python.parser.ast.Name;
import org.python.parser.ast.NameTok;
import org.python.parser.ast.Str;
import org.python.parser.ast.argumentsType;
import org.python.parser.ast.exprType;
import org.python.parser.ast.expr_contextType;
import org.python.parser.ast.stmtType;
import org.python.pydev.core.REF;
import org.python.pydev.parser.PyParser;

public class FastparserTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FastparserTest.class);
    }

    protected void setUp() throws Exception {
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testParseFile0() throws Exception { 
        String fileContents0 = getFileContents0();
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(fileContents0), false, null);
        
        Object[] obj = PyParser.reparseDocument(parserInfo);
        
        
        Module mod = makeFile0ClassDef();
        
        compareAST((SimpleNode) obj[0], mod);
        
        SimpleNode node = FastParser.reparseDocument(fileContents0);
        compareAST((SimpleNode) obj[0], node);
    }

    /**
     * @return
     */
    private Module makeFile0ClassDef() {
        Name name = new Name("object", expr_contextType.Load);
        name.beginColumn = 14;
        name.beginLine = 1;

        Str str = new Str("Class1 docs");
        str.beginColumn = 5;
        str.beginLine = 2;

        Expr expr = new Expr(str);
        expr.beginColumn = 17;
        expr.beginLine = 2;

        ClassDef def = new ClassDef(new NameTok("Class1", NameTok.ClassName), new exprType[]{name},new stmtType[]{ expr});
        def.beginColumn = 1;
        def.beginLine = 1;
        Module mod = new Module(new stmtType[]{def});
        return mod;
    }
    
    public void testParseFile1() throws Exception { 
        String fileContents1 = getFileContents1();
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(fileContents1), false, null);
        
        Object[] obj = PyParser.reparseDocument(parserInfo);
        
        Module mod = makeFile1MethodDef();
        
        compareAST((SimpleNode) obj[0], mod);
        
        SimpleNode node = FastParser.reparseDocument(fileContents1);
        compareAST((SimpleNode) obj[0], node);
    }

    /**
     * @return
     */
    private Module makeFile1MethodDef() {
        FunctionDef def = new FunctionDef(new NameTok("GlobalMethod", NameTok.FunctionName), null, null, null);
        def.beginColumn = 1;
        def.beginLine = 1;
        
        Name name = new Name("param1", expr_contextType.Store);
        name.beginColumn = 18;
        name.beginLine = 1;
        
        def.args = new argumentsType(new exprType[]{name}, null, null, new exprType[]{});
        
        Str str = new Str("GlobalMethod docs");
        str.beginColumn = 5;
        str.beginLine = 2;
        
        Expr expr = new Expr(str);
        expr.beginColumn = 27;
        expr.beginLine = 2;
        
        def.body = new stmtType[]{expr};
        Module mod = new Module(new stmtType[]{def});
        return mod;
    }

    public void testParseFile2() throws Exception { 
        String fileContents2 = getFileContents2();
        makeCompAST(fileContents2);
    }

    public void testParseFile3() throws Exception { 
        String fileContents3 = getFileContents3();
        Module node = (Module) FastParser.reparseDocument(fileContents3);
        MemoVisitor visitor1 = new MemoVisitor();
        visitor1.traverse(node);
        assertEquals(0, visitor1.size());
    }
    
    
    
    public void testParseFile4() throws Exception { 
        String fileContents4 = getFileContents4();
        SimpleNode node = FastParser.reparseDocument(fileContents4);
        compareAST(makeFile0ClassDef(), node);
    }
    
    public void testParseFile5() throws Exception { 
        String fileContents = getFileContents(5);
        SimpleNode node = FastParser.reparseDocument(fileContents);
        MemoVisitor visitor1 = new MemoVisitor();
        visitor1.traverse(node);
        assertEquals(0, visitor1.size());
    }

    public void testParseFile6() throws Exception { 
        String fileContents = getFileContents(6);
        makeCompAST(fileContents);
    }
    
    public void testParseFile7() throws Exception { 
        String fileContents = getFileContents(7);
        makeCompAST(fileContents);
    }
    
    public void testParseFile8() throws Exception { 
        String fileContents = getFileContents(8);
        makeCompAST(fileContents);
    }
    
    public void testParseFile9() throws Exception { 
        String fileContents = getFileContents(9);
        makeCompAST(fileContents);
    }

    /**
     * @param fileContents
     * @throws BadLocationException
     * @throws Exception
     */
    private void makeCompAST(String fileContents) throws BadLocationException, Exception {
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(fileContents), false, null);
        Object[] obj = PyParser.reparseDocument(parserInfo);
        
        SimpleNode node = FastParser.reparseDocument(fileContents);
        compareAST((SimpleNode) obj[0], node);
    }
    

    public void testParseTime() throws Exception { 
        String fileContents2 = getFileContents2();
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(fileContents2), false, null);
        
        long r1 = System.currentTimeMillis();
        //System.out.println("regular start "+new Date(r1));
        int att = 100;
        for (int i = 0; i < att; i++) {
            PyParser.reparseDocument(parserInfo);
        }
        long r2 = System.currentTimeMillis();
        //System.out.println("regular end "+new Date(r2));
        
        
        long s1 = System.currentTimeMillis();
        //System.out.println("simple start "+new Date(s1));
        for (int i = 0; i < att; i++) {
            FastParser.reparseDocument(fileContents2);
        }
        long s2 = System.currentTimeMillis();
        //System.out.println("simple end "+new Date(s2));
        
        double milisr = r2-r1;
        double miliss = s2-s1;
        //System.out.println("milis r "+milisr);
        //System.out.println("milis s "+miliss);
        println("perc "+miliss/milisr);
    }
    
    private void println(String string) {
//        System.out.println(string);
    }

    private void compareAST(SimpleNode o1, SimpleNode o2) throws Exception {
        MemoVisitor visitor1 = new MemoVisitor();
        MemoVisitor visitor2 = new MemoVisitor();
        visitor1.traverse(o1);
        visitor2.traverse(o2);
        
        assertEquals("\n"+visitor1  +"\n!=\n"+visitor2+"\n\n", visitor1, visitor2);
    }


    /**
     * @return Returns the fileContents0.
     */
    private String getFileContents0() {
        return REF.getFileContents(new File("tests/pysrc/filetoparse0.py"));
    }

    /**
     * @return Returns the fileContents1.
     */
    private String getFileContents1() {
        return REF.getFileContents(new File("tests/pysrc/filetoparse1.py"));
    }

    /**
     * @return Returns the fileContents2.
     */
    private String getFileContents2() {
        return REF.getFileContents(new File("tests/pysrc/filetoparse2.py"));
    }


    /**
     * @return Returns the fileContents3.
     */
    private String getFileContents3() {
        return REF.getFileContents(new File("tests/pysrc/filetoparse3.py"));
    }


    /**
     * @return Returns the fileContents4.
     */
    private String getFileContents4() {
        return REF.getFileContents(new File("tests/pysrc/filetoparse4.py"));
    }

    private String getFileContents(int i) {
        return REF.getFileContents(new File("tests/pysrc/filetoparse"+i+".py"));
    }

}
