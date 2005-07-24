/*
 * Created on 23/07/2005
 */
package com.python.pydev.analysis.messages;

import java.util.HashMap;
import java.util.Map;

import org.python.parser.SimpleNode;
import org.python.parser.ast.ClassDef;
import org.python.parser.ast.FunctionDef;
import org.python.pydev.editor.codecompletion.revisited.modules.SourceToken;

import com.python.pydev.analysis.IAnalysisPreferences;


public abstract class AbstractMessage implements IMessage{

    public static final Map<Integer, String> messages = new HashMap<Integer, String>();

    private int type;

    private int severity;

    private SourceToken generator;

    public AbstractMessage(int type, SourceToken generator, IAnalysisPreferences prefs) {
        this.severity = prefs.getSeverityForType(type);
        this.type = type;
        this.generator = generator;
    }

    private String getTypeStr() {
        if (messages.size() == 0) {
            messages.put(IAnalysisPreferences.TYPE_UNUSED_IMPORT, "Unused import(s): %s");
            messages.put(IAnalysisPreferences.TYPE_UNUSED_VARIABLE, "Unused variable: %s");
            messages.put(IAnalysisPreferences.TYPE_UNDEFINED_VARIABLE, "Undefined variable: %s");
            messages.put(IAnalysisPreferences.TYPE_DUPLICATED_SIGNATURE, "Duplicated signature: %s");
        }
        return messages.get(getType());

    }
    
    public int getSeverity() {
        return severity;
    }

    public int getType() {
        return type;
    }

    public int getStartLine() {
        return generator.getLineDefinition();
    }

    public int getStartCol() {
        int colDefinition = generator.getColDefinition();
        colDefinition = fixCol(colDefinition);
        return colDefinition;
    }

    private int fixCol(int col) {
        SimpleNode ast = generator.getAst();
        if(ast instanceof ClassDef){
            return col + 6;
        }
        if(ast instanceof FunctionDef){
            return col + 4;
        }
        return col;
    }

    public int getEndLine() {
        return generator.getLineEnd();
    }

    public int getEndCol() {
        return fixCol(generator.getColEnd());
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
    
    public SourceToken getGenerator() {
        return generator;
    }
}
