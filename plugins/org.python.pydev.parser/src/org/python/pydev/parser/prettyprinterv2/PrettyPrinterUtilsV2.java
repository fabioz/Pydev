package org.python.pydev.parser.prettyprinterv2;

import java.io.IOException;
import java.util.List;
import java.util.Stack;

import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.SpecialStr;
import org.python.pydev.parser.jython.Token;
import org.python.pydev.parser.jython.ast.ClassDef;
import org.python.pydev.parser.jython.ast.For;
import org.python.pydev.parser.jython.ast.FunctionDef;
import org.python.pydev.parser.jython.ast.If;
import org.python.pydev.parser.jython.ast.Name;
import org.python.pydev.parser.jython.ast.TryExcept;
import org.python.pydev.parser.jython.ast.TryFinally;
import org.python.pydev.parser.jython.ast.VisitorBase;
import org.python.pydev.parser.jython.ast.While;
import org.python.pydev.parser.jython.ast.With;
import org.python.pydev.parser.jython.ast.commentType;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinter.IPrettyPrinterPrefs;

public class PrettyPrinterUtilsV2 extends VisitorBase{

    protected IPrettyPrinterPrefs prefs;
    protected PrettyPrinterDocV2 doc;

    
    public PrettyPrinterUtilsV2(IPrettyPrinterPrefs prefs, PrettyPrinterDocV2 doc) {
        this.prefs = prefs;
        this.doc = doc;
    }

    private void writeSpecialsBefore(SimpleNode node) throws IOException {
        List<Object> specialsBefore = node.specialsBefore;
        if(node == null||specialsBefore == null){
            return;
        }
        writeSpecials(node, specialsBefore);
    }

    private void writeSpecialsAfter(SimpleNode node) throws IOException {
        List<Object> specialsAfter = node.specialsAfter;
        if(node == null||specialsAfter == null){
            return;
        }
        writeSpecials(node, specialsAfter);
    }
    
    private void writeSpecials(SimpleNode node, List<Object> specials) {
        for (Object c : specials){
            if(c instanceof commentType){
                commentType comment = (commentType) c;
                doc.add(comment.beginLine, comment.beginColumn, comment.id, comment);
                
            }else if(c instanceof Name){
                Name name = (Name) c;
                doc.add(name.beginLine, name.beginColumn, name.id, name);
                
            }else if(c instanceof SpecialStr){
                SpecialStr specialStr = (SpecialStr) c;
                doc.add(specialStr.beginLine, specialStr.beginCol, specialStr.toString(), specialStr);
                
            }else if(c instanceof Token){
                Token token = (Token) c;
                doc.add(token.beginLine, token.beginColumn, token.toString(), token);
                
            }else{
                throw new RuntimeException("Unexpected special: '"+c+ "' Class: "+c.getClass()+". Node: "+node);
            }
        }
    }
    
    Stack<Integer> ids = new Stack<Integer>();
    
    /**
     * Writes the specials before and starts recording
     * @throws Exception 
     */
    protected void beforeNode(SimpleNode node) throws Exception {
        this.lastNode = node;
        writeSpecialsBefore(node);
        if(node instanceof stmtType && !isMultiLineStmt((stmtType) node)){
            startStatementPart();
        }
    }

    
    private boolean isMultiLineStmt(stmtType node) {
        return node instanceof ClassDef || node instanceof For || node instanceof FunctionDef || node instanceof If || node instanceof TryExcept || node instanceof TryFinally || node instanceof While ||node instanceof With ;
    }

    protected void afterNode(SimpleNode node) throws IOException {
        if(node instanceof stmtType && !isMultiLineStmt((stmtType) node)){
            endStatementPart(node);
        }
        writeSpecialsAfter(node);
    }

    protected void startStatementPart() {
        ids.push(doc.pushRecordChanges());
    }
    
    protected void endStatementPart(SimpleNode node) {
        List<ILinePart> recordChanges = doc.popRecordChanges(ids.pop());
        
        Tuple<ILinePart, ILinePart> lowerAndHigher = doc.getLowerAndHigerFound(recordChanges);
        
        if(lowerAndHigher != null){
            doc.addStartStatementMark(lowerAndHigher.o1, (stmtType)node);
            doc.addEndStatementMark(lowerAndHigher.o2, (stmtType)node);
        }
    }

    
    protected void indent(SimpleNode node){
        doc.addIndent(node);
    }
    
    protected void dedent(){
        doc.addDedent(lastNode);
    }
    
    protected void indent(Token token) {
        doc.addIndent(token);
    }

    
    
    protected SimpleNode lastNode;
    
    protected Object unhandled_node(SimpleNode node) throws Exception {
        this.lastNode = node;
        return null;
    }

    @Override
    public void traverse(SimpleNode node) throws Exception {
        beforeNode(node);
        node.traverse(this);
        afterNode(node);
    }

    
}
