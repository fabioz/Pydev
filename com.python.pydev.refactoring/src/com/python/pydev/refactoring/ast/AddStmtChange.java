/*
 * Created on Jun 1, 2006
 */
package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.Document;
import org.python.pydev.core.REF;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;

import com.python.pydev.refactoring.visitors.PrettyPrinter;
import com.python.pydev.refactoring.visitors.PrettyPrinterPrefs;
import com.python.pydev.refactoring.visitors.WriterEraser;

public class AddStmtChange implements IChanges {

    private SimpleNode applyAt;
    private String attr;
    private int pos;
    private stmtType stmt;

    public AddStmtChange(SimpleNode m, String attr, int pos, stmtType stmt) {
        this.applyAt = m;
        this.attr = attr;
        this.pos = pos;
        this.stmt = stmt;
    }

    public SimpleNode apply(SimpleNode initialAst, Document doc) throws Throwable {
        stmtType[] attrObj = (stmtType[]) REF.getAttrObj(applyAt, attr);
        if(attrObj == null || attrObj.length == 0){
            attrObj = new stmtType[]{stmt}; 
        }else{
            throw new RuntimeException("Still not ok");
        }
        
        WriterEraser writerEraser = new WriterEraser();
        PrettyPrinter printer = new PrettyPrinter(new PrettyPrinterPrefs("\n"), writerEraser);
        stmt.accept(printer);
        StringBuffer buffer = writerEraser.getBuffer();
        doc.replace(0, 0, buffer.toString());
        return null;
    }

}
