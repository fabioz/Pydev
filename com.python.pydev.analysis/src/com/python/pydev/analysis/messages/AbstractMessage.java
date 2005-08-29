/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.codecompletion.revisited.IToken;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.IAnalysisPreferences;


public abstract class AbstractMessage implements IMessage{

    public static final Map<Integer, String> messages = new HashMap<Integer, String>();

    private int type;

    private int severity;

    private IToken generator;

    public AbstractMessage(int type, IToken generator, IAnalysisPreferences prefs) {
        this.severity = prefs.getSeverityForType(type);
        this.type = type;
        this.generator = generator;
    }

    private String getTypeStr() {
        if (messages.size() == 0) {
            messages.put(IAnalysisPreferences.TYPE_UNUSED_IMPORT, "Unused import: %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, "Unused variable: %s");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, "Undefined variable: %s");
            messages.put(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, "Duplicated signature: %s");
            messages.put(IAnalysisPreferences.TYPE_REIMPORT, "Import redefinition: %s");
            messages.put(IAnalysisPreferences.TYPE_UNRESOLVED_IMPORT, "Unresolved import: %s");
            messages.put(IAnalysisPreferences.TYPE_NO_SELF, "Method '%s' should have self as first parameter");
        }
        return messages.get(getType());

    }
    
    public int getSeverity() {
        return severity;
    }

    public int getType() {
        return type;
    }

    public int getStartLine(IDocument doc) {
        return generator.getLineDefinition();
    }

    public int getStartCol(IDocument doc) {
        int colDefinition;
        
        if(!generator.isImport()){
            colDefinition = generator.getColDefinition();
            if(colDefinition > 0){
                colDefinition = fixCol(colDefinition);
                return colDefinition;
            }
        }

        //it depends on the document contents... we have to remove the empty spaces to its left
        int startLine = getStartLine(doc);
        try {
            IRegion start = doc.getLineInformation(startLine-1);
            String line = doc.get(start.getOffset(), start.getLength());

            colDefinition = 0;
            while(line.length() > colDefinition && Character.isWhitespace(line.charAt(colDefinition))){
                colDefinition++;
            }
            colDefinition++;

        } catch (BadLocationException e) {
            colDefinition = 1;
        }
        return colDefinition;
    }

    private int fixCol(int col) {
        if(generator instanceof SourceToken){
            SimpleNode ast = ((SourceToken)generator).getAst();
            if(ast instanceof ClassDef){
                return col + 6;
            }
            if(ast instanceof FunctionDef){
                return col + 4;
            }
        }
        return col;
    }

    public int getEndLine(IDocument doc) {
        if(generator instanceof SourceToken){
            return ((SourceToken)generator).getLineEnd();
        }
        return -1;
    }

    
    public int getEndCol(IDocument doc) {
        if(generator instanceof SourceToken){
            int colEnd = ((SourceToken)generator).getColEnd();
            
            if(colEnd == -1){
                return -1;
            }
            
            return fixCol(colEnd);
        }
        return -1;
    }


    public String toString() {
        return getMessage();
    }

    public String getMessage() {
        String typeStr = getTypeStr();
        if(typeStr == null){
            throw new AssertionError("Unable to get message for type: "+getSeverity());
        }
        Object shortMessage = getShortMessage();
        if(shortMessage == null){
            throw new AssertionError("Unable to get shortMessage ("+typeStr+")");
        }
        if( shortMessage instanceof Object[]){
            Object[] o = (Object[]) shortMessage;
            StringBuffer buf = new StringBuffer();
            for (int i = 0; i < o.length; i++) {
                buf.append(o[i].toString());
                if(i != o.length-1){
                    buf.append(" ");
                }
            }
            shortMessage = buf.toString();
        }
        return String.format(typeStr, shortMessage);
    }
    
    public IToken getGenerator() {
        return generator;
    }
}
