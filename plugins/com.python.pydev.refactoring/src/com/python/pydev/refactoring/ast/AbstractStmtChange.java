package com.python.pydev.refactoring.ast;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.DocumentChange;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.text.edits.MultiTextEdit;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.text.edits.TextEditGroup;
import org.python.pydev.core.Tuple;
import org.python.pydev.parser.jython.SimpleNode;
import org.python.pydev.parser.jython.ast.stmtType;
import org.python.pydev.parser.prettyprinter.PrettyPrinter;
import org.python.pydev.parser.prettyprinter.PrettyPrinterPrefs;
import org.python.pydev.parser.prettyprinter.WriterEraser;
import org.python.pydev.parser.visitors.NodeUtils;


public abstract class AbstractStmtChange implements IChanges {
    
    /**
     * This method returns a document change where changes should be added 
     * @param doc the document this change is linked to
     * @return a tuple with the document change and the root of the text edit change
     */
    protected Tuple<TextChange, MultiTextEdit> getDocChange(IDocument doc) {
        TextChange docChange = new DocumentChange("Add Stmt Change", doc);
        
        MultiTextEdit rootEdit = new MultiTextEdit();
        docChange.setEdit(rootEdit);
        docChange.setKeepPreviewEdits(true);
        Tuple<TextChange, MultiTextEdit> tup = new Tuple<TextChange, MultiTextEdit>(docChange, rootEdit);
        return tup;
    }
    
    
    protected void addTextEdit(String desc, Tuple<TextChange, MultiTextEdit> tup, TextEdit edit) {
        tup.o2.addChild(edit);
        tup.o1.addTextEditGroup(new TextEditGroup(desc, edit));
    }
    
    protected PrettyPrinterPrefs getPrefs(IDocument doc) {
        return new PrettyPrinterPrefs("\n");
    }


    /**
     * @param prefs the preferences when printing the nodes
     * @param after: the offset returned should be after the end of the offset of this node (so this
     * is the node before the statement we're adding)
     * 
     * @param before: the offset returned should be before this stmt (so this is the node after it actually)
     *  
     * @return the end of the statement passed as an absolute offset
     */
    protected int getStmtOffsetEnd(SimpleNode after, stmtType before, IDocument doc, PrettyPrinterPrefs prefs) {
        try {
            
            if(before != null){
                //it's easier to get it this way
                return getOffsetFromNodeBegin(before, doc);
            }
            
            //ok, if we didn't have the node 
            GetLastStmtVisitor getLastStmtVisitor = new GetLastStmtVisitor();
            after.accept(getLastStmtVisitor);
            
            SimpleNode lastNode = getLastStmtVisitor.getLastNode();
            int lineEnd = NodeUtils.getLineEnd(lastNode);
            
            //now, let's get the col end
            WriterEraser writerEraser = new WriterEraser();
            PrettyPrinter printer = new PrettyPrinter(prefs, writerEraser);
            lastNode.accept(printer);
            StringBuffer buffer = writerEraser.getBuffer();
            int col = lastNode.beginColumn + buffer.toString().trim().length();
            
            try {
                int lineOffset = doc.getLineOffset(lineEnd-1);

                
                int offset = lineOffset + col - 1;
                if(offset > doc.getLength()){
                    return doc.getLength();
                }
                
                int lineLength = doc.getLineLength(lineEnd-1);
                String lineDelimiter = doc.getLineDelimiter(lineEnd-1);
                if(offset == lineOffset + lineLength - lineDelimiter.length()){
                    //we want to add it in a new line... (the start of the next line)
                    return doc.getLineOffset(lineEnd);
                }
                
                return offset;
            } catch (BadLocationException e) {
                return doc.getLength();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    /**
     * @return the offset from the start of the passed node
     * @throws BadLocationException
     */
    protected int getOffsetFromNodeBegin(SimpleNode node, IDocument doc) throws BadLocationException {
        int line = node.beginLine-1;
        int col = node.beginColumn-1;
        
        int lineOffset = doc.getLineOffset(line);
        lineOffset += col;
        return lineOffset;
    }

}
