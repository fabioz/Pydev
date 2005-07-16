/*
 * Created on 13/07/2005
 */
package com.python.pydev.fastparser;

import java.io.File;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.Expr;
import org.python.parser.ast.Module;
import org.python.parser.ast.Name;
import org.python.parser.ast.Str;
import org.python.parser.ast.exprType;
import org.python.parser.ast.expr_contextType;
import org.python.parser.ast.stmtType;
import org.python.pydev.core.REF;
import org.python.pydev.parser.PyParser;

public class FastparserTest extends TestCase {

    public static void main(String[] args) {
        junit.textui.TestRunner.run(FastparserTest.class);
    }

    private String fileContents;

    protected void setUp() throws Exception {
        super.setUp();
        File file = new File("tests/pysrc/filetoparse0.py");
        fileContents = REF.getFileContents(file);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testParse() throws Exception { 
        PyParser.ParserInfo parserInfo = new PyParser.ParserInfo(new Document(fileContents), false, null);
        
        Object[] obj = PyParser.reparseDocument(parserInfo);
        
        
        Name name = new Name("object", expr_contextType.Load);
        name.beginColumn = 14;
        name.beginLine = 1;

        Str str = new Str("Class1 docs");
        str.beginColumn = 5;
        str.beginLine = 2;

        Expr expr = new Expr(str);
        expr.beginColumn = 17;
        expr.beginLine = 2;

        ClassDef def = new ClassDef("Class1", new exprType[]{name},new stmtType[]{ expr});
        def.beginColumn = 1;
        def.beginLine = 1;
        Module mod = new Module(new stmtType[]{def});
        
        compareAST((SimpleNode) obj[0], mod);
        
        SimpleNode node = FastParser.reparseDocument(fileContents);
        compareAST((SimpleNode) obj[0], node);
    }

    private void compareAST(SimpleNode o1, SimpleNode o2) throws Exception {
        MemoVisitor visitor1 = new MemoVisitor();
        MemoVisitor visitor2 = new MemoVisitor();
        visitor1.traverse(o1);
        visitor2.traverse(o2);
        
        assertEquals(visitor1, visitor2);
    }

}
